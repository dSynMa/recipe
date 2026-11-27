package recipe.analysis;

import static recipe.lang.utils.Cancellation.checkCancellation;

import static recipe.analysis.ToNuXmv.ltolToLTLAndObservationVariables;
import static recipe.analysis.ToNuXmv.specialiseObservationToSendTransition;

import static recipe.Config.commVariableReferences;
import static recipe.Config.isCvRef;
import static recipe.analysis.Smv.*;

import recipe.analysis.Smv.Expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import recipe.Config;
import recipe.lang.System;
import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.agents.State;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.GuardReference;
import recipe.lang.expressions.location.AnyLocation;
import recipe.lang.expressions.location.Location;
import recipe.lang.expressions.location.NameLocation;
import recipe.lang.expressions.location.NamedLocation;
import recipe.lang.expressions.location.PredicateLocation;
import recipe.lang.expressions.location.SelfLocation;
import recipe.lang.ltol.LTOL;
import recipe.lang.ltol.Observation;
import recipe.lang.process.BasicProcess;
import recipe.lang.process.GetProcess;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;
import recipe.lang.process.SupplyProcess;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Type;
import recipe.lang.utils.Pair;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

/** One translation invocation; mutable output is never shared between calls. */
final class NuXmvTranslation {
    private final System system;
    private final Smv.Module module = new Smv.Module();
    private int unlabelledCounter = 0;
    private List<LTOL> specs;
    private Map<String, Observation> observations;
    private final List<AgentInstance> agentInstances;
    private final Map<AgentState, List<Expr>> agentSendPreds = new LinkedHashMap<>();
    private final Map<AgentState, List<Expr>> agentSendProgressConds = new LinkedHashMap<>();
    private final Map<String, List<Expr>> sendLabels = new LinkedHashMap<>();
    private final Set<String> actionLabelNames = new HashSet<>();
    private final List<Expr> progress = new ArrayList<>();
    private final Set<String> supplyGetDefinitions = new LinkedHashSet<>();
    private final Map<Agent, Map<?, Set<ProcessTransition>>> agentStateSendTransitionMap = new HashMap<>();
    private final Map<Agent, Map<?, Set<ProcessTransition>>> agentStateReceiveTransitionMap = new HashMap<>();
    private final Map<Agent, Map<?, Set<ProcessTransition>>> agentStateGetTransitionMap = new HashMap<>();
    private final Map<Agent, Map<?, Set<ProcessTransition>>> agentStateSupplyTransitionMap = new HashMap<>();

    NuXmvTranslation(System system) throws Exception {
        this.system = system;
        this.agentInstances = system.getAgentInstances();
    }

    private String constant(String value) {
        return Config.broadcast.equals(value) ? Config.nuxmvBroadcast : value;
    }

    private String variableType(Type type) {
        if (type instanceof Enum enumeration) {
            return "{" + String.join(",", enumeration.getValues().stream()
                .map(this::constant).toArray(String[]::new)) + "}";
        }
        return type.name();
    }

    private Expr source(Expression<?> expression) throws Exception {
        Expression<Boolean> defined = expression.definednessCondition().simplify();
        if (defined.equals(Condition.getTrue())) return atom(expression.toNuXmv());
        // nuXmv prints definitions even for disabled transitions. Their inactive values
        // must be evaluable; action feasibility still forbids taking an undefined update.
        Expr inactive = expression.getType() instanceof Boolean ? FALSE : atom("0");
        if (defined.equals(Condition.getFalse())) return inactive;
        return conditional(source(defined), atom(expression.toNuXmv()), inactive);
    }

    private Expr guard(Expression<Boolean> expression) throws Exception {
        return source(new recipe.lang.expressions.predicate.And(
            expression.definednessCondition(), expression).simplify());
    }

    String translate() throws Exception {
        boolean previousResolve = GuardReference.resolve;
        GuardReference.resolve = true;
        try {
            prepareSpecifications();
            defineUnchangedStateAndLabelResets();
            indexTransitions();
            translateInteractions();
            combineTransitions();
            return renderModule();
        } finally {
            GuardReference.resolve = previousResolve;
        }
    }

    private EncodedStep receiverResponse(AgentInstance receivingAgentInstance, PreparedSend send) throws Exception {
        Expression<Enum> channel = send.channel;
        Expression<Boolean> sendGuardExpr = send.guard;
        Agent receiveAgent = receivingAgentInstance.getAgent();
        String receiveName = receivingAgentInstance.getLabel();

        Expression<Boolean> receiveGuardExpr = receiveAgent.getReceiveGuard()
            .relabel(v -> v.toString().equals(Config.channelLabel)
            ? channel
            : v.sameTypeWithName(receiveName + "-" + v));
        receiveGuardExpr = receiveGuardExpr.simplify();

        Expr receiveGuard;

        if (channel.toString().equals(Config.broadcast))
            receiveGuard = TRUE;
        else if (channel instanceof TypedValue && receiveGuardExpr.toString().equals("FALSE")) {
            receiveGuard = FALSE;
        } else if(channel.getClass().equals(TypedValue.class)){
            receiveGuard = guard(receiveGuardExpr);
        } else {
            receiveGuard = or(guard(receiveGuardExpr), eq(source(channel), atom(Config.nuxmvBroadcast)));
        }

        Expression<Boolean> receiverSelection = sendGuardExpr.relabel(v -> {
            return v.getName().startsWith("@") ? v.sameTypeWithName(v.getName().substring(1)) : v;
        });

        receiverSelection = receiverSelection.relabel(v -> {
            try {

                return isCvRef(system, v.getName())
                    ? ((Expression<?>) receiveAgent.getRelabel().get(v)).relabel(vv -> vv.sameTypeWithName(receiveName + "-" + vv))
                    : v;
            } catch (RelabellingTypeException | MismatchingTypeException e) {
                e.printStackTrace();
            }
            return null;
        }).simplify();

        Map<State<?>, List<EncodedStep>> receiveTransitions = new LinkedHashMap<>();
        for (State<?> receiveAgentState : receiveAgent.getStates()) {
            Set<ProcessTransition> receiveAgentReceiveTransitions = agentStateReceiveTransitionMap.get(receiveAgent).get(receiveAgentState);
            if(receiveAgentReceiveTransitions == null){
                receiveAgentReceiveTransitions = new HashSet<>();
            }


            List<EncodedStep> transitions = new ArrayList<>();

            for (ProcessTransition receiveTrans : receiveAgentReceiveTransitions) {
                EncodedStep receive = translateReceiveTransition(receivingAgentInstance, receiveTrans, send);
                if (receive != null) {
                    transitions.add(receive);
                }
            }

            if (!transitions.isEmpty()) {
                receiveTransitions.put(receiveAgentState, transitions);
            }
        }

        List<EncodedStep> cases = new ArrayList<>();

        addBroadcastWithoutReceiveCases(receivingAgentInstance, send.channel, receiveTransitions, cases);
        addEnabledReceiveCases(receiveName, receiveGuard, receiverSelection, receiveTransitions, cases);
        addNotListeningCase(receiveGuard, receiveName, cases);
        addUnselectedBroadcastCase(channel, receiverSelection, receiveName, cases);

        return new EncodedStep(or(relations(cases)), or(enabled(cases)));
    }

    private PreparedSend prepareSend(AgentInstance sender, ProcessTransition t) throws Exception {
        Agent sendingAgent = sender.getAgent();
        String sendingAgentName = sender.getLabel();
        TypedValue<?> sendingAgentNameValue = new TypedValue<>(Config.getAgentType(), sendingAgentName);
        List<Expr> sendTriggeredIf = new ArrayList<>();
        List<Expr> sendEffects = new ArrayList<>();

        SendProcess sendingProcess = (SendProcess) t.getLabel();

        Expression<Enum> channel = sendingProcess
            .getChannel()
            .relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v));

        sendTriggeredIf.add(source(sendingProcess.getPsi()
            .relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v)).simplify()));

        sendEffects.add(atom("falsify-" + sendingAgentName));
        sendEffects.add(eq(next(state(sendingAgentName)), atom(t.getDestination().toString())));

        for (Map.Entry<String, ?> entry : sendingProcess.getUpdate().entrySet()) {
            Expression<?> update = (Expression<?>) entry.getValue();
            sendEffects.add(eq(next(local(sendingAgentName, entry.getKey())),
                source(update.relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v)).simplify())));
        }
        for (String var : sendingAgent.getStore().getAttributes().keySet()) {
            if (!sendingProcess.getUpdate().containsKey(var)) {
                sendEffects.add(eq(next(local(sendingAgentName, var)), local(sendingAgentName, var)));
            }
        }

        Map<String, Expression<?>> relabelledMessage = new HashMap<>();
        for (Map.Entry<String, ?> entry : sendingProcess.getMessage().entrySet()) {
            Expression<?> payload = (Expression<?>) entry.getValue();
            relabelledMessage.put(entry.getKey(), payload.relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v.getName())).simplify());
        }

        Expression<Boolean> sendGuardExpr = sendingProcess.getMessageGuard().relabel(v -> {
            if (v.getName().equals(Config.channelLabel)
                && !sendingAgent.getStore().getAttributes().containsKey(v.getName())) {
                return channel;
            } else {
                return isCvRef(system, v.getName())
                    ? v
                    : v.sameTypeWithName(sendingAgentName + "-" + v);
            }
        }).simplify();

        for(Map.Entry<String, Observation> entry : observations.entrySet()){
            Observation obs = entry.getValue();
            String var = entry.getKey();

            Expression<Boolean> observationCondition = specialiseObservationToSendTransition(
                commVariableReferences(system.getCommunicationVariables()),
                obs.getObservation(),
                sendGuardExpr,
                relabelledMessage,
                sendingAgentNameValue,
                channel,
                system.getMessageStructure().keySet());

            sendEffects.add(eq(next(atom(var)), source(observationCondition)));
        }
        return new PreparedSend(sendingProcess, channel, relabelledMessage, sendGuardExpr, sendTriggeredIf, sendEffects);
    }

    private EncodedStep translateSend(int index, ProcessTransition transition) throws Exception {
        PreparedSend send = prepareSend(agentInstances.get(index), transition);
        List<Expr> receivers = new ArrayList<>();
        List<Expr> enabledReceivers = new ArrayList<>();
        for (int i = 0; i < agentInstances.size(); i++) {
            if (i == index) continue;
            EncodedStep receiver = receiverResponse(agentInstances.get(i), send);
            receivers.add(receiver.relation);
            enabledReceivers.add(receiver.enabled);
        }
        Expr guard = and(send.triggeredIf);
        return new EncodedStep(and(guard, and(send.effects), and(receivers)),
            and(guard, and(enabledReceivers)));
    }

    private void translateSupply(int i, State<?> state, ProcessTransition t) throws Exception {
        AgentInstance sender = agentInstances.get(i);
        Agent sendingAgent = sender.getAgent();
        String sendingAgentName = sender.getLabel();
        Expr supplierStateIsCurrentState = eq(state(sendingAgentName), atom(state.toString()));
        List<Expr> supplyTriggeredIf = new ArrayList<>();
        List<Expr> supplyEffects = new ArrayList<>();
        SupplyProcess supplyProcess = (SupplyProcess) t.getLabel();
        supplyTriggeredIf.add(supplierStateIsCurrentState);
        supplyTriggeredIf.add(source(supplyProcess.getPsi().relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v)).simplify()));

        supplyEffects.add(labelEffects(sendingAgentName, supplyProcess));
        supplyEffects.add(atom("no-observations"));
        supplyEffects.add(eq(next(state(sendingAgentName)), atom(t.getDestination().toString())));

        for (Map.Entry<String, ?> entry : supplyProcess.getUpdate().entrySet()) {
            Expression<?> update = (Expression<?>) entry.getValue();
            supplyEffects.add(eq(next(local(sendingAgentName, entry.getKey())),
                source(update.relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v)).simplify())));
        }
        for (String var : sendingAgent.getStore().getAttributes().keySet()) {
            if (!supplyProcess.getUpdate().containsKey(var)) {
                supplyEffects.add(eq(next(local(sendingAgentName, var)), local(sendingAgentName, var)));
            }
        }

        Map<String, Expression<?>> relabelledMessage = new HashMap<>();
        for (Map.Entry<String, ?> entry : supplyProcess.getMessage().entrySet()) {
            Expression<?> payload = (Expression<?>) entry.getValue();
            relabelledMessage.put(entry.getKey(), payload.relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v.getName())).simplify());
        }
        Location supplyLocation = supplyProcess.getLocation();
        Expression<Boolean> supplyPredicate = null;
        if (supplyLocation instanceof PredicateLocation) {
            supplyPredicate = ((PredicateLocation) supplyLocation).getExpression().relabel(v -> {
                return isCvRef(system, v.getName())
                    ? v
                    : v.sameTypeWithName(sendingAgentName + "-" + v);
            }).simplify();
        }

        PreparedSupply preparedSupply = new PreparedSupply(sender, supplyProcess, supplyLocation,
            supplyPredicate, relabelledMessage, supplyTriggeredIf, supplyEffects);
        for (int j = 0; j < agentInstances.size(); j++) {
            if (i == j) continue;
            AgentInstance getterInstance = agentInstances.get(j);
            Agent getterAgent = getterInstance.getAgent();
            String getterName = getterInstance.getLabel();

            for (State<?> getterState : getterAgent.getStates()) {
                Set<ProcessTransition> getTransitions = agentStateGetTransitionMap.get(getterAgent).get(getterState);
                if (getTransitions == null || getTransitions.size() == 0) continue; // no get's in this state, nothing to do
                Expr getterStateIsCurrentState = eq(state(getterName), atom(getterState.toString()));

                for (ProcessTransition gt : getTransitions) {
                    translateSupplyGetPair(preparedSupply, getterInstance, gt, getterStateIsCurrentState);
                }
            }
        }
    }

    private void declareAndInitializeAgent(AgentInstance instance) throws Exception {
        Agent agent = instance.getAgent();
        String name = instance.getLabel();
        for (TypedVariable<?> variable : agent.getStore().getAttributes().values()) {
            module.variable(name + "-" + variable.getName(), variableType(variable.getType()));
        }
        String states = String.join(",", agent.getStates().stream().map(Object::toString).toArray(String[]::new));
        module.variable(name + "-automaton-state", "{" + states + "}");
        module.initially(eq(state(name), atom(agent.getInitialState().toString())));
        module.initially(source(agent.getInit().relabel(v -> v.sameTypeWithName(name + "-" + v))));
        module.initially(source(instance.getInit().relabel(v -> v.sameTypeWithName(name + "-" + v))));
        if (agent.getStore().getAttributes().containsKey(Config.myselfKeyword)) {
            module.initially(eq(local(name, Config.myselfKeyword), atom(name)));
        }
    }

    private void translateInteractions() throws Exception {
        for(int i = 0; i < agentInstances.size(); i++) {
            AgentInstance sendingAgentInstance = agentInstances.get(i);
            Agent sendingAgent = sendingAgentInstance.getAgent();
            String sendingAgentName = sendingAgentInstance.getLabel();

            declareAndInitializeAgent(sendingAgentInstance);
            for (State<?> state : sendingAgent.getStates()) {
                checkCancellation();
                Set<ProcessTransition> sendTransitions = agentStateSendTransitionMap.get(sendingAgent).get(state);

                AgentState sendStateIsCurrentState = new AgentState(sendingAgentName, state.toString());

                if (sendTransitions != null && sendTransitions.size() > 0) {
                    List<Expr> transitionSendPreds = new ArrayList<>();
                    List<Expr> transitionSendProgressCond = new ArrayList<>();

                    for (ProcessTransition t : sendTransitions) {
                        EncodedStep send = translateSend(i, t);
                        Expr agentSendPred = send.relation;
                        Expr agentSendProgressCond = send.enabled;
                        transitionSendPreds.add(agentSendPred);
                        String label = t.getLabel().getLabel();
                        if (label != null && !label.isEmpty()) {
                            sendLabels.computeIfAbsent(sendingAgentName + "-" + label, key -> new ArrayList<>())
                                .add(and(sendStateIsCurrentState.condition(), agentSendProgressCond));
                        }

                        transitionSendProgressCond.add(agentSendProgressCond);
                    }
                    agentSendPreds.put(sendStateIsCurrentState, transitionSendPreds);
                    agentSendProgressConds.put(sendStateIsCurrentState, transitionSendProgressCond);
                }

                Set<ProcessTransition> supplyTransitions = agentStateSupplyTransitionMap.get(sendingAgent).get(state);
                if (supplyTransitions != null && !supplyTransitions.isEmpty()) {
                    for (ProcessTransition t : supplyTransitions) {
                        translateSupply(i, state, t);
                    }
                }
            }
        }
    }

    private void prepareSpecifications() throws Exception {
        Pair<List<LTOL>, Map<String, Observation>> specsAndObs = ltolToLTLAndObservationVariables(system.getSpecs());
        specs = specsAndObs.getLeft();
        observations = specsAndObs.getRight();
        for (String observation : observations.keySet()) {
            module.initially(eq(atom(observation), FALSE));
            module.variable(observation, "boolean");
        }
    }

    private void defineUnchangedStateAndLabelResets() {
        List<Expr> keepAll = new ArrayList<>(List.of(TRUE));
        for (AgentInstance instance : agentInstances) {
            Agent agent = instance.getAgent();
            String name = instance.getLabel();
            List<Expr> keep = new ArrayList<>(List.of(TRUE));
            for (String variable : agent.getStore().getAttributes().keySet()) {
                keep.add(eq(next(local(name, variable)), local(name, variable)));
                keep.add(eq(next(state(name)), state(name)));
            }
            List<Expr> resetLabels = new ArrayList<>(List.of(TRUE));
            Set<ProcessTransition> labelled = new HashSet<>(agent.getReceiveTransitions());
            labelled.addAll(agent.getSupplyTransitions());
            labelled.addAll(agent.getGetTransitions());
            Set<String> seen = new HashSet<>();
            for (ProcessTransition transition : labelled) {
                String label = transition.getLabel().getLabel();
                if (label == null || label.isEmpty() || !seen.add(label)) continue;
                Expr reset = eq(next(local(name, label)), FALSE);
                keep.add(reset);
                resetLabels.add(reset);
                keepAll.add(reset);
                actionLabelNames.add(name + "-" + label);
                List<Expr> resetOthers = new ArrayList<>(List.of(TRUE));
                for (ProcessTransition other : labelled) {
                    String otherLabel = other.getLabel().getLabel();
                    if (otherLabel != null && !otherLabel.isEmpty() && !otherLabel.equals(label))
                        resetOthers.add(eq(next(local(name, otherLabel)), FALSE));
                }
                module.define("falsify-not-" + name + "-" + label, and(resetOthers));
            }
            module.define("falsify-not-" + name, and(resetLabels));
            module.define("falsify-" + name, and(resetLabels));
            keepAll.add(atom("keep-all-" + name));
            module.define("keep-all-" + name, and(keep));
        }
        module.define("keep-all", and(keepAll));
        List<Expr> resetObservations = new ArrayList<>(List.of(TRUE));
        for (String observation : observations.keySet())
            resetObservations.add(eq(next(atom(observation)), FALSE));
        module.define("no-observations", and(resetObservations));
    }

    private void indexTransitions() throws Exception {
        for(Agent agent : system.getAgents()){
            for (var action : agent.getActions()) {
                if (action instanceof BasicProcess process) {
                    String label = process.getLabel();
                    if (label != null && agent.getStore().getAttributes().containsKey(label)) {
                        throw new IllegalArgumentException("Action label '" + label + "' in agent "
                            + agent.getName() + " conflicts with a local variable. Rename the label or variable.");
                    }
                }
            }
            agentStateSendTransitionMap.put(agent, agent.getStateTransitionMap(agent.getSendTransitions()));
            agentStateReceiveTransitionMap.put(agent, agent.getStateTransitionMap(agent.getReceiveTransitions()));
            agentStateGetTransitionMap.put(agent, agent.getStateTransitionMap(agent.getGetTransitions()));
            agentStateSupplyTransitionMap.put(agent, agent.getStateTransitionMap(agent.getSupplyTransitions()));
        }
    }

    private void combineTransitions() {
        List<Expr> sends = new ArrayList<>();
        for (Map.Entry<AgentState, List<Expr>> entry : agentSendPreds.entrySet())
            sends.add(and(entry.getKey().condition(), or(entry.getValue())));
        for (Map.Entry<AgentState, List<Expr>> entry : agentSendProgressConds.entrySet())
            progress.add(and(entry.getKey().condition(), or(entry.getValue())));
        List<Expr> pairs = new ArrayList<>();
        for (String name : supplyGetDefinitions) pairs.add(atom(name));
        module.define("transition", or(or(sends), or(pairs)));
    }

    private String renderModule() throws Exception {
        for (Map.Entry<String, List<Expr>> label : sendLabels.entrySet()) {
            if (actionLabelNames.contains(label.getKey()))
                throw new IllegalArgumentException("Send and event actions cannot share label " + label.getKey());
            module.define(label.getKey(), or(label.getValue()));
        }
        for (String name : actionLabelNames) {
            module.variable(name, "boolean");
            module.initially(eq(atom(name), FALSE));
        }
        module.define("progress", progress.isEmpty() ? FALSE : or(progress));
        for (String label : Enum.getEnumLabels())
            module.constants(Enum.getEnum(label).getValues().stream().map(this::constant).collect(java.util.stream.Collectors.toList()));
        module.transition(or(atom("transition"),
            and(not(atom("progress")), atom("keep-all"), atom("no-observations"))));
        for (LTOL specification : specs) module.specification(specification.toNuXmv());
        return module.render();
    }

    private static List<Expr> relations(List<EncodedStep> steps) {
        List<Expr> result = new ArrayList<>();
        for (EncodedStep step : steps) result.add(step.relation);
        return result;
    }

    private static List<Expr> enabled(List<EncodedStep> steps) {
        List<Expr> result = new ArrayList<>();
        for (EncodedStep step : steps) result.add(step.enabled);
        return result;
    }

    private record AgentState(String agent, String state) {
        Expr condition() { return eq(Smv.state(agent), atom(state)); }
    }

    private static Expr labelEffects(String agent, BasicProcess action) {
        String label = action.getLabel();
        if (label == null || label.isEmpty()) return atom("falsify-" + agent);
        return and(eq(next(local(agent, label)), TRUE), atom("falsify-not-" + agent + "-" + label));
    }

    private static final class EncodedStep {
        final Expr relation;
        final Expr enabled;

        EncodedStep(Expr relation, Expr enabled) {
            this.relation = relation;
            this.enabled = enabled;
        }
    }

    private static final class PreparedSend {
        final SendProcess process;
        final Expression<Enum> channel;
        final Map<String, Expression<?>> message;
        final Expression<Boolean> guard;
        final List<Expr> triggeredIf;
        final List<Expr> effects;

        PreparedSend(SendProcess process, Expression<Enum> channel,
            Map<String, Expression<?>> message, Expression<Boolean> guard,
            List<Expr> triggeredIf, List<Expr> effects) {
            this.process = process;
            this.channel = channel;
            this.message = message;
            this.guard = guard;
            this.triggeredIf = triggeredIf;
            this.effects = effects;
        }
    }
    /** Returns null when this receive cannot consume the outgoing message. */
    private EncodedStep translateReceiveTransition(
        AgentInstance receiver,
        ProcessTransition receiveTrans,
        PreparedSend send) throws Exception {
        Agent receiveAgent = receiver.getAgent();
        String receiveName = receiver.getLabel();
        SendProcess sendingProcess = send.process;
        Map<String, Expression<?>> relabelledMessage = send.message;
        Expression<Enum> channel = send.channel;
        ReceiveProcess receiveProcess = (ReceiveProcess) receiveTrans.getLabel();

        List<Expr> receiveTransTriggeredIf = new ArrayList<>();
        List<Expr> receiveTransEffects = new ArrayList<>();

        AtomicReference<java.lang.Boolean> stop = new AtomicReference<java.lang.Boolean>(false);
        Function<Expression<?>, Expression<?>> stopHelper = (e) -> {
            stop.set(true);
            return e;
        };

        Expression<recipe.lang.types.Boolean> receiveTransitionGuard = receiveProcess.getPsi()
            .relabel(v -> sendingProcess.getMessage().containsKey(v.getName())
            ? relabelledMessage.get(v.getName())
            : (system.getMessageStructure().containsKey(v.getName())
            ? stopHelper.apply(v)
            : v.sameTypeWithName(receiveName + "-" + v)));
        receiveTransitionGuard = receiveTransitionGuard.simplify();

        if (stop.get()) return null;

        if (receiveTransitionGuard.equals(Condition.getFalse()))
            return null;
        else
            receiveTransTriggeredIf.add(source(receiveTransitionGuard));

        Expression<Enum> receivingOnThisChannelVarOrVal = receiveProcess.getChannel()
            .relabel(v -> v.sameTypeWithName(receiveName + "-" + v.toString()));

        if (receivingOnThisChannelVarOrVal.getClass().equals(TypedValue.class)
            && channel.getClass().equals(TypedValue.class)
            && !channel.equals(receivingOnThisChannelVarOrVal))
        return null;
        else
            receiveTransTriggeredIf.add(eq(source(channel), source(receivingOnThisChannelVarOrVal)));

        for (Map.Entry<String, ?> entry : receiveProcess.getUpdate().entrySet()) {
            Expression<?> update = (Expression<?>) entry.getValue();
            receiveTransEffects.add(eq(next(local(receiveName, entry.getKey())),
                source(update.relabel(v ->
                sendingProcess.getMessage().containsKey(v.getName())
                ? relabelledMessage.get(v.getName())
                : (system.getMessageStructure().containsKey(v.getName())
                ? stopHelper.apply(v)
                : v.sameTypeWithName(receiveName + "-" + v))))));
        }

        if (stop.get()) return null;

        for (String var : receiveAgent.getStore().getAttributes().keySet()) {
            if (!receiveProcess.getUpdate().containsKey(var)) {
                receiveTransEffects.add(eq(next(local(receiveName, var)), local(receiveName, var)));
            }
        }

        if (receiveProcess.getLabel() != null && !receiveProcess.getLabel().equals("")) {
            receiveTransEffects.add(eq(next(local(receiveName, receiveProcess.getLabel())), TRUE));
            receiveTransEffects.add(atom("falsify-not-" + receiveName + "-" + receiveProcess.getLabel()));
        } else {
            receiveTransEffects.add(atom("falsify-not-" + receiveName));
        }

        receiveTransEffects.add(eq(next(state(receiveName)), atom(receiveTrans.getDestination().toString())));

        return new EncodedStep(and(and(receiveTransTriggeredIf), and(receiveTransEffects)), and(receiveTransTriggeredIf));
    }

    private void addBroadcastWithoutReceiveCases(
        AgentInstance receiver, Expression<Enum> channel,
        Map<State<?>, List<EncodedStep>> receiveTransitions, List<EncodedStep> cases) throws Exception {
        String name = receiver.getLabel();
        for (State<?> state : receiver.getAgent().getStates()) {
            Expr atState = eq(state(name), atom(state.toString()));
            Expr broadcast = eq(source(channel), atom(Config.nuxmvBroadcast));
            List<EncodedStep> transitions = receiveTransitions.get(state);
            if (transitions == null) {
                cases.add(new EncodedStep(and(atState, broadcast, atom("keep-all-" + name)), and(atState, broadcast)));
            } else if (!transitions.isEmpty()) {
                Expr disabled = not(or(enabled(transitions)));
                cases.add(new EncodedStep(and(atState, broadcast, atom("keep-all-" + name), disabled),
                    and(atState, broadcast, disabled)));
            }
        }
    }

    private void addEnabledReceiveCases(
        String receiver, Expr receiveGuard, Expression<Boolean> receiverSelection,
        Map<State<?>, List<EncodedStep>> receiveTransitions, List<EncodedStep> cases) throws Exception {
        if (receiveTransitions.isEmpty()) return;
        List<Expr> relations = new ArrayList<>();
        List<Expr> enabled = new ArrayList<>();
        for (Map.Entry<State<?>, List<EncodedStep>> entry : receiveTransitions.entrySet()) {
            Expr atState = eq(state(receiver), atom(entry.getKey().toString()));
            relations.add(and(atState, or(relations(entry.getValue()))));
            enabled.add(and(atState, or(enabled(entry.getValue()))));
        }
        Expr participates = and(receiveGuard, guard(receiverSelection));
        cases.add(new EncodedStep(and(participates, or(relations)), and(participates, or(enabled))));
    }

    private void addNotListeningCase(Expr receiveGuard, String receiver, List<EncodedStep> cases) {
        Expr notListening = not(receiveGuard);
        cases.add(new EncodedStep(and(notListening, atom("keep-all-" + receiver)), notListening));
    }

    private void addUnselectedBroadcastCase(
        Expression<Enum> channel, Expression<Boolean> selection, String receiver, List<EncodedStep> cases) throws Exception {
        Expr unselectedBroadcast = and(eq(source(channel), atom(Config.nuxmvBroadcast)), not(guard(selection)));
        cases.add(new EncodedStep(and(unselectedBroadcast, atom("keep-all-" + receiver)), unselectedBroadcast));
    }

    private void translateSupplyGetPair(
        PreparedSupply supply,
        AgentInstance getterInstance,
        ProcessTransition gt,
        Expr getterStateIsCurrentState) throws Exception {
        Agent sendingAgent = supply.sender.getAgent();
        String sendingAgentName = supply.sender.getLabel();
        Agent getterAgent = getterInstance.getAgent();
        String getterName = getterInstance.getLabel();
        SupplyProcess supplyProcess = supply.process;
        Location supplyLocation = supply.location;
        GetProcess getProcess = (GetProcess) gt.getLabel();
        Location getterLocation = getProcess.getLocation();
        Map<String, Expression<?>> relabelledMessage = supply.message;
        List<Expr> supplyTriggeredIf = supply.triggeredIf;
        List<Expr> supplyEffects = supply.effects;
        AtomicReference<java.lang.Boolean> stop = new AtomicReference<>(false);
        Function<Expression<?>, Expression<?>> stopHelper = e -> { stop.set(true); return e; };
        List<Expr> getTriggeredIf = new ArrayList<>();
        getTriggeredIf.add(getterStateIsCurrentState);
        List<Expr> getEffects = new ArrayList<>();
        getEffects.add(labelEffects(getterName, gt.getLabel()));

        // Static location compatibility. The old Java grammar used concrete names
        // and SELF; the JSON grammar uses location variables.
        if (supplyLocation instanceof NameLocation
                && !((NameLocation) supplyLocation).getName().equals(getterName)) return;
        if (getterLocation instanceof NameLocation) {
            if (!((NameLocation) getterLocation).getName().equals(sendingAgentName)) return;
        }
        if (getterLocation instanceof SelfLocation) {
            throw new Exception("GET@SELF is not allowed.");
        }
        if (supplyLocation instanceof SelfLocation) {
            boolean getterNamesSupplier = getterLocation instanceof NamedLocation
                || (getterLocation instanceof NameLocation
                    && ((NameLocation) getterLocation).getName().equals(sendingAgentName));
            if (!getterNamesSupplier) return;
        }

        Expression<recipe.lang.types.Boolean> getTransitionGuard = getProcess.getPsi()
            .relabel(v -> supplyProcess.getMessage().containsKey(v.getName())
            ? relabelledMessage.get(v.getName())
            : (system.getMessageStructure().containsKey(v.getName())
            ? stopHelper.apply(v)
            : v.sameTypeWithName(getterName + "-" + v)));

        if (stop.get()) return;
        if (getTransitionGuard.equals(Condition.getFalse())) return;
        else getTriggeredIf.add(source(getTransitionGuard));

        Expr sendSelection = TRUE;
        if (supplyLocation instanceof PredicateLocation) {
            Expression<Boolean> supplyGuardExprHere = supply.predicate.relabel(v -> {
                return v.getName().startsWith("@") ? v.sameTypeWithName(v.getName().substring(1)) : v;
            });
            supplyGuardExprHere = supplyGuardExprHere.relabel(v -> {
                try {
                    return isCvRef(system, v.getName())
                        ? ((Expression<?>) getterAgent.getRelabel().get(v)).relabel(vv -> vv.sameTypeWithName(getterName + "-" + vv))
                        : v;
                } catch (RelabellingTypeException | MismatchingTypeException e) {
                    e.printStackTrace();
                }
                return null;
            }).simplify();
            sendSelection = guard(supplyGuardExprHere);
        } else if (supplyLocation instanceof NamedLocation) {
            String var = ((NamedLocation) supplyLocation).getVariable().getName();
            sendSelection = eq(local(sendingAgentName, var), atom(getterName));
        }

        Expr getSelection = TRUE;
        if (getterLocation instanceof PredicateLocation) {
            Expression<Boolean> getterGuardExpr = ((PredicateLocation) getterLocation).getExpression()
                .relabel(v -> isCvRef(system, v.getName())
                    ? v
                    : v.sameTypeWithName(getterName + "-" + v))
                .simplify();
            Expression<Boolean> getterGuardExprHere = getterGuardExpr.relabel(v -> {
                return v.getName().startsWith("@") ? v.sameTypeWithName(v.getName().substring(1)) : v;
            });
            getterGuardExprHere = getterGuardExprHere.relabel(v -> {
                try {
                    return isCvRef(system, v.getName())
                        ? ((Expression<?>) sendingAgent.getRelabel().get(v)).relabel(vv -> vv.sameTypeWithName(sendingAgentName + "-" + vv))
                        : v;
                } catch (RelabellingTypeException | MismatchingTypeException e) {
                    e.printStackTrace();
                }
                return null;
            }).simplify();
            getSelection = guard(getterGuardExprHere);
        } else if (getterLocation instanceof NamedLocation) {
            String var = ((NamedLocation) getterLocation).getVariable().getName();
            getSelection = eq(local(getterName, var), atom(sendingAgentName));
        }

        for (Map.Entry<String, ?> entry : getProcess.getUpdate().entrySet()) {
            Expression<?> update = (Expression<?>) entry.getValue();
            getEffects.add(eq(next(local(getterName, entry.getKey())),
                source(update.relabel(v ->
                supplyProcess.getMessage().containsKey(v.getName())
                ? relabelledMessage.get(v.getName())
                : (system.getMessageStructure().containsKey(v.getName())
                ? stopHelper.apply(v)
                : v.sameTypeWithName(getterName + "-" + v))))));
        }

        if (stop.get()) return;

        for (String var : getterAgent.getStore().getAttributes().keySet()) {
            if (!getProcess.getUpdate().containsKey(var)) {
                getEffects.add(eq(next(local(getterName, var)), local(getterName, var)));
            }
        }

        getEffects.add(eq(next(state(getterName)), atom(gt.getDestination().toString())));

        for (AgentInstance other : agentInstances) {
            String otherName = other.getLabel();
            if (otherName != sendingAgentName && otherName != getterName)
                getEffects.add(atom("keep-all-" + otherName));
        }

        String splyLbl = sendingAgentName + "-";
        String getLbl = getterName + "-";
        if (supplyProcess.getLabel() != null && !supplyProcess.getLabel().equals("")) {
            splyLbl += supplyProcess.getLabel();
        } else {
            splyLbl += String.format("unlabelled_supply_%d", unlabelledCounter++);
        }
        if (getProcess.getLabel() != null && !getProcess.getLabel().equals("")) {
            getLbl += getProcess.getLabel();
        } else {
            getLbl += String.format("unlabelled_get_%d", unlabelledCounter++);
        }
        String lbl = splyLbl + "-" + getLbl;
        while (supplyGetDefinitions.contains(lbl)) lbl += "-" + unlabelledCounter++;

        Expr enabled = and(and(supplyTriggeredIf), sendSelection, and(getTriggeredIf), getSelection);
        module.define(lbl, and(enabled, and(supplyEffects), and(getEffects)));
        supplyGetDefinitions.add(lbl);
        module.define(lbl + "-progress", enabled);
        progress.add(atom(lbl + "-progress"));
    }

    private static final class PreparedSupply {
        final AgentInstance sender;
        final SupplyProcess process;
        final Location location;
        final Expression<Boolean> predicate;
        final Map<String, Expression<?>> message;
        final List<Expr> triggeredIf;
        final List<Expr> effects;

        PreparedSupply(AgentInstance sender, SupplyProcess process, Location location,
            Expression<Boolean> predicate, Map<String, Expression<?>> message,
            List<Expr> triggeredIf, List<Expr> effects) {
            this.sender = sender;
            this.process = process;
            this.location = location;
            this.predicate = predicate;
            this.message = message;
            this.triggeredIf = triggeredIf;
            this.effects = effects;
        }
    }
}
