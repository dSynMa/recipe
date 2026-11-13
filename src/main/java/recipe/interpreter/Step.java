package recipe.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import recipe.Config;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.NamedLocation;
import recipe.lang.ltol.Observation;
import recipe.lang.process.BasicProcess;
import recipe.lang.process.BasicProcessWithMessage;
import recipe.lang.process.GetProcess;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;
import recipe.lang.process.SupplyProcess;
import recipe.lang.store.CompositeStore;
import recipe.lang.store.ConcreteStore;
import recipe.lang.store.Store;
import recipe.lang.types.Type;
import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.MismatchingTypeException;

public class Step {
    /**
     *
     */
    private final Interpreter interpreter;
    private Map<AgentInstance,ConcreteStore> stores;
    private Map<TypedValue, Set<AgentInstance>> listeners;
    List<Transition> transitions;
    private Transition chosenTransition;
    private recipe.lang.System sys;
    // The transition that led to this state
    private Transition inboundTransition;
    int depth;
    Step parent;
    private JSONObject annotations;

    protected void handleEvaluationException(Exception e) {
        // TODO
        System.err.println(e);
        e.printStackTrace();
    }

    public Step getParent() {
        return parent;
    }

    public JSONObject toJSON() {
        JSONObject jStores = new JSONObject();
        for (AgentInstance instance : stores.keySet()) {
            ConcreteStore store = stores.get(instance);
            JSONObject jStore = new JSONObject();
            jStore.put("**state**", store.getState().label.toString());
            if (parent != null) {
                ProcessTransition maybeTransition = parent.chosenTransition.findTransitionForAgent(instance);
                if (maybeTransition != null) {
                    BasicProcess trProcess = maybeTransition.getLabel();
                    String lbl = trProcess.prettyPrintLabel();

                    jStore.put("**from_state**", parent.stores.get(instance).getState().label.toString());
                    jStore.put("**last_transition**", trProcess.toString());
                    jStore.put("**last_label**", lbl);
                }
            }
            store.getData().forEach((var, value) -> {
                jStore.put(var.getName(), value.toString());
            });
            jStores.put(instance.getLabel(), jStore);
        }
        List<JSONObject> jTransitions = new ArrayList<>(transitions.size());
        for (Transition t : transitions) {
            jTransitions.add(t.toJSON());
        }
        JSONObject result = new JSONObject();
        result.put("depth", depth);
        result.put(
            "inboundTransition",
            inboundTransition == null ? null : inboundTransition.toJSON());
        result.put("state", jStores);
        result.put("transitions", jTransitions);
        if (this.transitions.size() == 0) {
            result.put("___DEADLOCK___", true);
        }
        if (this.annotations != null) {
            for (String key : annotations.keySet()) {
                result.put(key, annotations.get(key));
            }
        }
        return result;
    }

    private static java.lang.Boolean traceBoolean(Object value) {
        if (value instanceof java.lang.Boolean) return (java.lang.Boolean) value;
        if (value instanceof String) {
            if ("TRUE".equalsIgnoreCase((String) value)) return true;
            if ("FALSE".equalsIgnoreCase((String) value)) return false;
        }
        return null;
    }

    /**
     * Check satisfaction of a state constraint
     * @param constraint a JSON object
     * @return true iff the state satisfies the constraint
     */
    public boolean satisfies(Map<String,Observation> obsMap, JSONObject constraint) {
        // LTOL
        JSONObject ltol = constraint.optJSONObject("___LTOL___");
        if (ltol != null) {
            for (String obsVar : ltol.keySet()) {
                if (obsVar.equals("no-observations")) continue;
                java.lang.Boolean expectedObservation = traceBoolean(ltol.get(obsVar));
                if (expectedObservation == null) return false;
                Observation descriptor = obsMap.get(obsVar);
                if (descriptor == null) return false;
                if (inboundTransition == null || !(inboundTransition.getProducerProcess() instanceof SendProcess)) {
                    if (expectedObservation) return false;
                    continue;
                }
                AgentInstance sender = inboundTransition.getProducer();
                Store senderStore = parent.stores.get(sender);
                try {
                    SendProcess send = (SendProcess) inboundTransition.getProducerProcess();
                    TypedValue channel = send.getChannel().valueIn(senderStore);
                    Map<String, Expression> message = new HashMap<>();
                    for (Map.Entry<String, Expression> entry : send.getMessage().entrySet()) {
                        message.put(entry.getKey(), entry.getValue().valueIn(senderStore));
                    }
                    Expression<recipe.lang.types.Boolean> selection = send.getMessageGuard().relabel(variable -> {
                        if (variable.getName().equals("channel")
                                && !sender.getAgent().getStore().getAttributes().containsKey("channel")) return channel;
                        if (Config.isCvRef(sys, variable.getName())) return variable;
                        return senderStore.getValue(variable);
                    });
                    // Use the same finite-domain observation semantics as translation:
                    // quantify over assignments satisfying the sender's selection predicate.
                    Expression<recipe.lang.types.Boolean> observed = recipe.analysis.ToNuXmv.specialiseObservationToSendTransition(
                        Config.commVariableReferences(sys.getCommunicationVariables()), descriptor.getObservation(),
                        selection, message, new TypedValue(Config.getAgentType(), sender.getLabel()), channel,
                        sys.getMessageStructure().keySet());
                    boolean actual = Condition.getTrue().equals(observed.valueIn(senderStore));
                    if (actual != expectedObservation) return false;
                } catch (Exception e) {
                    handleEvaluationException(e);
                    return false;
                }
            }
        }
        // Agents
        for (String agentInstanceName : constraint.keySet()) {
            AgentInstance inst = null;
            for (AgentInstance i : sys.getAgentInstances()) {
                if (i.getLabel().equals(agentInstanceName)) {
                    inst = i;
                    break;
                }
            }
            if (inst == null) {
                // Skip special fields
                if (agentInstanceName.startsWith("___")) {
                    // System.out.printf(">> Found annotation %s : %s\n", agentInstanceName, constraint.get(agentInstanceName));
                    continue;
                }
                System.out.printf(">> Agent instance not found: %s\n", agentInstanceName);
                return false;
            }
            JSONObject instanceConstraint = constraint.getJSONObject(agentInstanceName);
            ConcreteStore instanceStore = this.stores.get(inst);
            if (this.parent != null) {
                // Everything not in the constraint must remain unchanged
                Store pastInstanceStore = this.parent.stores.get(inst);
                for (String varName : instanceStore.getAttributes().keySet()) {
                    try {
                        TypedVariable var = inst.getAgent().getStore().getAttribute(varName);
                        TypedValue value = instanceStore.getValue(var);
                        TypedValue pastValue = pastInstanceStore.getValue(var);
                        if (!instanceConstraint.has(varName) && !pastValue.equals(value)) {
                            System.out.printf(">> %s: expected %s (from past state), got %s\n", pastValue, value);
                        }
                    } catch (AttributeNotInStoreException e) {
                        // Should never match
                        System.out.printf(">> Not found: %s\n", varName);
                    }
                }
            }

            for (String varName : instanceConstraint.keySet()) {
                try {
                    if (varName.equals("automaton-state")) {
                        String stateConstraint = instanceConstraint.getString(varName);
                        String stateInst = instanceStore.getState().getLabel().toString();
                        if (!stateInst.equals(stateConstraint)) {
                            System.out.printf(">> Expected %s, got %s\n", stateConstraint, stateInst);
                            return false;
                        }
                        continue;
                    }
                    boolean sendLabel = inst.getAgent().getSendTransitions().stream()
                        .anyMatch(action -> varName.equals(action.getLabel().getLabel()));
                    if (sendLabel) {
                        java.lang.Boolean expected = traceBoolean(instanceConstraint.get(varName));
                        if (expected == null) return false;
                        boolean enabled = transitions.stream().anyMatch(action ->
                            action.getProducer().getLabel().equals(agentInstanceName) && action.getProducerProcess() instanceof SendProcess
                                && varName.equals(action.getProducerProcess().getLabel()));
                        if (enabled != expected) return false;
                        continue;
                    }
                    boolean eventLabel = inst.getAgent().getActions().stream().anyMatch(action ->
                        action instanceof BasicProcess && varName.equals(((BasicProcess) action).getLabel()));
                    if (eventLabel) {
                        java.lang.Boolean expected = traceBoolean(instanceConstraint.get(varName));
                        if (expected == null) return false;
                        ProcessTransition taken = inboundTransition == null ? null : inboundTransition.findTransitionForAgent(inst);
                        boolean actual = taken != null && varName.equals(taken.getLabel().getLabel());
                        if (actual != expected) return false;
                        continue;
                    }
                    TypedVariable var = inst.getAgent().getStore().getAttribute(varName);
                    TypedValue value = this.stores.get(inst).getValue(var);
                    if (value != null) {
                        if (value.getType() instanceof recipe.lang.types.Number) {
                            // Go for numerical comparison
                            try {
                                java.math.BigDecimal x = new java.math.BigDecimal(instanceConstraint.get(varName).toString());
                                java.math.BigDecimal y = new java.math.BigDecimal(value.getValue().toString());
                                if (x.compareTo(y) != 0) {
                                    System.out.printf(
                                        ">>%s-%s: Expected %s, got %s\n",
                                        inst.getLabel(), varName, x, y);
                                    return false;
                                }
                            } catch (NumberFormatException e) {
                                handleEvaluationException(e);
                                return false;
                            }

                        }
                        else if (value.getType() instanceof recipe.lang.types.Boolean) {
                            // Go for Boolean comparison
                            java.lang.Boolean isConstraintTrue = traceBoolean(instanceConstraint.get(varName));
                            if (isConstraintTrue == null) return false;
                            if (!value.getValue().equals(isConstraintTrue)) {
                                System.out.printf(
                                    ">>%s-%s: Expected %s, got %s\n",
                                    inst.getLabel(), varName, isConstraintTrue, value.getValue());
                                return false;
                            }
                        }

                        else if (!value.getValue().toString().equals(instanceConstraint.get(varName).toString())) {
                            System.out.printf(
                                ">>%s-%s: Expected %s, got %s\n",
                                inst.getLabel(), varName, instanceConstraint.get(varName), value.getValue());
                            return false;
                        }
                    } else {
                        // System.out.printf(">> Not found: %s\n", varName);
                    }
                } catch (AttributeNotInStoreException e) {
                    // Other variables that we do not care about
                    // System.out.printf(">> Not found: %s\n", varName);
                }
            }
        }
        return true;
    }

    public Step next(int index, Interpreter interpreter) {
        assert index < transitions.size();
        this.chosenTransition = transitions.get(index);

        Map<AgentInstance,ConcreteStore> nextStores = new HashMap<AgentInstance,ConcreteStore>(stores);

        try {
            // Update sender
            AgentInstance initiator = chosenTransition.getProducer();
            ConcreteStore nextSenderStore = stores.get(initiator).BuildNext(chosenTransition.getProducerTransition());
            nextStores.put(initiator, nextSenderStore);

            Store msgStore = makeMessageStore(stores.get(initiator), chosenTransition.getProducerProcess(), sys);
            for (AgentInstance receiver : chosenTransition.getConsumers()) {
                ProcessTransition receive = chosenTransition.findTransitionForAgent(receiver);
                ConcreteStore nextReceiverStore = stores.get(receiver).BuildNext(receive, msgStore);
                nextStores.put(receiver, nextReceiverStore);
            }
        } catch (Exception e) {
            handleEvaluationException(e);
        }
        Step next = new Step(nextStores, this, interpreter);
        return next;
    }

    public void loadAnnotations(JSONObject obj) {
        if (annotations == null) annotations = new JSONObject();
        for (String key : obj.keySet())
            if (key.startsWith("___"))
                annotations.put(key, obj.get(key));
    }

    protected Store makeMessageStore(Store senderStore, BasicProcessWithMessage procWithMsg, recipe.lang.System sys) {
        // Add message and channel to a new map
        Map<TypedVariable, TypedValue> msgMap = new HashMap<TypedVariable, TypedValue>();
        try {
            procWithMsg.getMessage().forEach((msgVar, msgExpr) -> {
                try {
                    TypedValue msgVal = msgExpr.valueIn(senderStore);
                    Type msgType = sys.getMessageStructure().get(msgVar);
                    // Numeric expressions may evaluate to a wider numeric type.
                    // Validate and represent the value in the declared message domain.
                    if (msgType instanceof recipe.lang.types.Number
                            && msgVal.getType() instanceof recipe.lang.types.Number) {
                        msgVal = new TypedValue(msgType, new java.math.BigDecimal(
                            msgVal.getValue().toString()).stripTrailingZeros().toPlainString());
                    }
                    if (msgType != msgVal.getType()) {
                        throw new MismatchingTypeException(
                            String.format("Mismatch type for message variable %s (expected %s, got %s)",
                            msgVar,
                            msgType.toString(),
                            msgVal.getType().toString()));
                    }
                    TypedVariable<Type> tv = new TypedVariable<>(msgType, msgVar);
                    msgMap.put(tv, msgVal);
                } catch (Exception e) {
                    handleEvaluationException(e);
                }
            });
            if (procWithMsg instanceof SendProcess) {
                Expression chanExpr = procWithMsg.getChannel();
                TypedValue chan = chanExpr.valueIn(senderStore);
                msgMap.put(interpreter.getChannelTV(), chan);
            }
            return new ConcreteStore(msgMap);
        } catch (Exception e) {
            handleEvaluationException(e);
        }
        return null;
    }

    private static boolean actionEnabled(BasicProcess action, Store store) {
        try {
            if (!Condition.getTrue().equals(action.getPsi().valueIn(store))) return false;
            for (Expression update : action.getUpdate().values()) {
                if (update.valueIn(store) == null) return false;
            }
            return true;
        } catch (Exception unavailableValue) {
            // Reject only this candidate, not the other receive/get alternatives.
            return false;
        }
    }

    private static boolean selectionHolds(Expression selection, Store store) {
        try {
            return Condition.getTrue().equals(selection.valueIn(store));
        } catch (Exception unavailableValue) {
            return false;
        }
    }

    public Step(Map<AgentInstance,ConcreteStore> stores, Step parent, Interpreter interp) {
        this.interpreter = interp;
        this.sys = interpreter.sys;
        this.parent = parent;
        this.stores = stores;
        this.transitions = new LinkedList<>();
        this.listeners = new HashMap<>();
        this.depth = parent == null ? 0 : parent.depth + 1;

        if (parent != null) {
            this.inboundTransition = parent.chosenTransition;
        }

        // Evaluate which channels each agent is currently listening to
        try {
            Type chanEnum = recipe.lang.types.Enum.getEnum("channel");
            for (AgentInstance instance : interpreter.sys.getAgentInstances()) {
                ConcreteStore store = stores.get(instance);
                for (Object objChan : chanEnum.getAllValues()) {
                    TypedValue chan = (TypedValue) objChan;
                    Store s = store.push(interpreter.getChannelTV(), chan);
                    TypedValue evalGuard = instance.getAgent().getReceiveGuard().valueIn(s);
                    boolean isListening = chan.toString().equals(Config.broadcast) || Condition.getTrue().equals(evalGuard);
                    // System.out.printf("eval %s on store %s ---> %s\n", instance.getAgent().getReceiveGuard(), s, isListening);
                    if (isListening) {
                        listeners.putIfAbsent(chan, new HashSet<>());
                        listeners.get(chan).add(instance);
                    }
                }
            }
        } catch (Exception e) {
            handleEvaluationException(e);
        }

        // Compute available transitions (put/receive)
        interpreter.sys.getAgentInstances().forEach(sender -> {
            ConcreteStore senderStore = stores.get(sender);
            Set<ProcessTransition> instSends = interpreter.sends.get(senderStore.getState());

            if (instSends != null) {
                instSends.forEach(tr -> {
                    SendProcess sendProcess = (SendProcess) tr.getLabel();
                    try {
                        boolean psiSat = actionEnabled(sendProcess, senderStore);

                        if (psiSat) {
                            // Add message and channel to a new map
                            Store msgStore = makeMessageStore(senderStore, sendProcess, sys);
                            TypedValue chan = msgStore.getValue(interpreter.getChannelTV());

                            // Set up map of receivers
                            // A receiver must
                            // a) not be the sender
                            // b) be listening to channel "chan"
                            // c) satisfy send guard
                            Map<AgentInstance, Set<ProcessTransition>> receivesMap = new HashMap<AgentInstance, Set<ProcessTransition>>();
                            for (AgentInstance inst : listeners.getOrDefault(chan, Set.of())) {
                                if (inst != sender) {
                                    try {
                                        Store instStore = stores.get(inst).push(msgStore);
                                        // Local variables get evaluated over the sender,
                                        // CVs get evaluated over the receiver (inst)
                                        CompositeStore sendGuardStore = new CompositeStore(sys);
                                        sendGuardStore.push(sender.getAgent().getStore().getAttributes().containsKey("channel")
                                            ? senderStore : senderStore.push(interpreter.getChannelTV(), chan));
                                        sendGuardStore.pushReceiverStore(instStore);

                                        TypedValue sendGuard = sendProcess.getMessageGuard().valueIn(sendGuardStore);
                                        boolean sendGuardOk = Condition.getTrue().equals(sendGuard);

                                        TypedValue receiveGuard = inst.getAgent().getReceiveGuard().valueIn(instStore);
                                        boolean receiveGuardOk = chan.toString().equals(Config.broadcast) || Condition.getTrue().equals(receiveGuard);
                                        // System.out.printf("sendguard: %s (%s)\n", sendGuard, sendGuardOk);
                                        // System.out.printf("receiveguard: %s (%s)\n", receiveGuard, receiveGuardOk);

                                        if (sendGuardOk && receiveGuardOk) {
                                            receivesMap.put(inst, new HashSet<ProcessTransition>());
                                        } else if (!chan.toString().equals(Config.broadcast)) {
                                            // Every listening multicast receiver must be selected.
                                            return;
                                        }
                                    } catch (Exception e) {
                                        handleEvaluationException(e);
                                        if (!chan.toString().equals(Config.broadcast)) return;
                                    }
                                }
                            }
                            // Map every receiver to its receive actions
                            // (channel must match and psi must hold)
                            receivesMap.keySet().forEach(receiver -> {
                                try {
                                    Store store = stores.get(receiver).push(msgStore);
                                    Set<ProcessTransition> receives = interpreter.receives.get(stores.get(receiver).getState());
                                    if (receives != null) {
                                        for (ProcessTransition rec : receives) {
                                            ReceiveProcess recLbl = (ReceiveProcess) rec.getLabel();
                                            Expression recChanExpr = recLbl.getChannel();
                                            if (recChanExpr.valueIn(store).equals(chan)) {
                                                boolean recPsiSat = actionEnabled(recLbl, store);
                                                // System.err.printf("%s evaluates to %s\n", recPsi, recPsiSat);
                                                // System.err.printf("receive: %s\n", recLbl);
                                                // System.err.printf("store: %s\n", store);

                                                if (recPsiSat) {
                                                    receivesMap.get(receiver).add(rec);
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    handleEvaluationException(e);
                                }
                            });

                            // Each receiver chooses independently. Empty broadcast choices
                            // idle; an empty multicast choice blocks the whole send.
                            List<Map<AgentInstance, ProcessTransition>> combinations = new ArrayList<>();
                            combinations.add(new HashMap<>());
                            for (Map.Entry<AgentInstance, Set<ProcessTransition>> entry : receivesMap.entrySet()) {
                                if (entry.getValue().isEmpty()) {
                                    if (chan.toString().equals(Config.broadcast)) continue;
                                    combinations.clear();
                                    break;
                                }
                                List<Map<AgentInstance, ProcessTransition>> expanded = new ArrayList<>();
                                for (Map<AgentInstance, ProcessTransition> combination : combinations) {
                                    for (ProcessTransition receive : entry.getValue()) {
                                        Map<AgentInstance, ProcessTransition> choice = new HashMap<>(combination);
                                        choice.put(entry.getKey(), receive);
                                        expanded.add(choice);
                                    }
                                }
                                combinations = expanded;
                            }
                            for (Map<AgentInstance, ProcessTransition> combination : combinations) {
                                SendReceiveTransition transition = new SendReceiveTransition();
                                transition.setProducer(sender, tr);
                                for (Map.Entry<AgentInstance, ProcessTransition> entry : combination.entrySet()) {
                                    transition.pushConsumer(entry.getKey(), entry.getValue());
                                }
                                transitions.add(transition);
                            }
                        }
                    } catch (Exception e) {
                        handleEvaluationException(e);
                    }
                });
            }
        });

        //Compute available transitions (get/supply)
        interpreter.sys.getAgentInstances().forEach(supplier -> {
            ConcreteStore supplierStore = stores.get(supplier);
            Set<ProcessTransition> splys = interpreter.supplys.get(supplierStore.getState());
            if (splys == null) return;
            for (ProcessTransition sply : splys) {
                SupplyProcess splyProc = (SupplyProcess) sply.getLabel();
                try {
                    boolean psiSat = actionEnabled(splyProc, supplierStore);
                    if (!psiSat) continue;
                    for (AgentInstance getter : interpreter.sys.getAgentInstances()) {
                        if (getter == supplier) continue;
                        ConcreteStore getterStore = stores.get(getter);
                        Set<ProcessTransition> gets = interpreter.gets.get(getterStore.getState());
                        if (gets == null) continue;
                        for (ProcessTransition get : gets) {
                            Store msgStore = makeMessageStore(supplierStore, splyProc, sys);
                            Store instStore = getterStore.push(msgStore);
                            GetProcess getProc = (GetProcess) get.getLabel();
                            boolean getPsiSat = actionEnabled(getProc, instStore);
                            if (!getPsiSat) continue;
                            // Check if predicates match
                            Expression<recipe.lang.types.Boolean> splyGuard = splyProc.getMessageGuard();
                            Expression<recipe.lang.types.Boolean> getGuard = getProc.getMessageGuard();

                            if (splyGuard instanceof NamedLocation) {
                                boolean isSelf = ((NamedLocation) splyGuard).isSelf();
                                // SPLY@SELF = Only getters that know the supplier's name will be served
                                if (isSelf && !getGuard.toString().equals(supplier.getLabel())) continue;
                                // SPLY@foo = Only getter with name foo will be served
                                if (!isSelf && !splyGuard.toString().equals(getter.getLabel())) continue;
                            }
                            if (getGuard instanceof NamedLocation) {
                                boolean isSelf = ((NamedLocation) getGuard).isSelf();
                                // GET@SELF = Only suppliers that know the getter's name will be chosen
                                if (isSelf && !splyGuard.toString().equals(getter.getLabel())) continue;
                                // GET@foo = Only supplier with name foo will be chosen
                                if (!isSelf && !getGuard.toString().equals(supplier.getLabel())) continue;
                            }
                            CompositeStore supplySelection = new CompositeStore(sys);
                            supplySelection.push(supplierStore);
                            supplySelection.pushReceiverStore(getterStore);
                            CompositeStore getSelection = new CompositeStore(sys);
                            getSelection.push(instStore);
                            getSelection.pushReceiverStore(supplierStore);
                            if (!(splyGuard instanceof NamedLocation) && !selectionHolds(splyGuard, supplySelection)) continue;
                            if (!(getGuard instanceof NamedLocation) && !selectionHolds(getGuard, getSelection)) continue;

                            Transition tr = new SupplyGetTransition();
                            tr.setProducer(supplier, sply);
                            tr.pushConsumer(getter, get);
                            transitions.add(tr);
                        }
                    }
                } catch (Exception e) {
                    handleEvaluationException(e);
                }
            }
        });

    }

}