package recipe.analysis;

import static recipe.Config.commVariableReferences;
import static recipe.Config.isCvRef;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import recipe.Config;
import recipe.lang.System;
import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.Predicate;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.location.AnyLocation;
import recipe.lang.expressions.location.Location;
import recipe.lang.expressions.location.NamedLocation;
import recipe.lang.expressions.location.SelfLocation;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.GuardReference;
import recipe.lang.expressions.predicate.Implies;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.expressions.predicate.Not;
import recipe.lang.expressions.predicate.Or;
import recipe.lang.ltol.LTOL;
import recipe.lang.ltol.Observation;
import recipe.lang.process.BasicProcess;
import recipe.lang.process.GetProcess;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;
import recipe.lang.process.SupplyProcess;
import recipe.lang.types.Boolean;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Enum;
import recipe.lang.types.Type;
import recipe.lang.utils.Pair;
import recipe.lang.utils.Triple;
import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;
import recipe.lang.utils.exceptions.TypeCreationException;

public class ToNuXmv {

    private static Expression sendRename(TypedVariable v, TypedValue sender, Expression channel, TypedValue noAgent) {
        String vName = v.getName();
        
        if (vName.equals("sender") || vName.equals("producer") || vName.equals("initiator")) { return sender; }
        if (vName.equals("supplier") || vName.equals("getter")) { return noAgent; }
        if (vName.equals(Config.channelLabel)) { return channel; }
        if (vName.equals(Config.p2pLabel)) { return Condition.getFalse(); }
        return v;
    }
    private static Expression supplyRename(TypedVariable v, TypedValue supplier, TypedValue getter, TypedValue noAgent) {
        String vName = v.getName();
        if (vName.equals("supplier") || vName.equals("producer")) { return supplier; }
        if (vName.equals("getter") || vName.equals("initiator")) { return getter; }
        if (vName.equals("sender")) { return noAgent; }
        if (vName.equals(Config.channelLabel)) { return Condition.getFalse(); }
        if (vName.equals(Config.p2pLabel)) { return Condition.getTrue(); }
        return v;
    }

    public static Expression<Boolean> specialiseObservationToSendTransition(Map<String, Type> cvs,
                                                                            Expression<Boolean> obs,
                                                                            Expression<Boolean> sendGuard,
                                                                            Map<String, Expression> message,
                                                                            TypedValue sender,
                                                                            TypedValue noAgent,
                                                                            Expression channel) throws Exception 
    {
        // If obs talks about supplier-<cv> then it is False
        for (Expression<Boolean> sub : obs.subformulas()) {
            if (sub instanceof TypedVariable) {
                if (sub.toString().startsWith("supplier-")) return Condition.getFalse();
            }
        }



        Expression<Boolean> o = obs.relabel((v) -> sendRename(v, sender, channel, noAgent) );
        return specialiseObservationToTransition(cvs, o, sendGuard, message);
    }

    private static Expression specializeSupplierCV(TypedVariable v, Agent supplierAgent, TypedValue supplier) {
        if (!v.getName().startsWith("supplier-")) return v;
        try {
            TypedVariable vv = v.sameTypeWithName(v.getName().replaceFirst("supplier-", ""));
            Expression exp = supplierAgent.getRelabel().get(vv);
            String supplierName = supplier.getValue().toString();
            return exp.relabel((x) -> ((TypedVariable) x).sameTypeWithName(supplierName.toString() + "-" + x));
        } catch (Exception e) {
            // WE TRIED
            e.printStackTrace();
            return v.sameTypeWithName(v.getName() + "-NOT-FOUND");
        }
    }

    public static Expression<Boolean> specialiseObservationToSupplyTransition(Map<String, Type> cvs,
                                                                              Expression<Boolean> obs,
                                                                              Expression<Boolean> getGuard,
                                                                              Map<String, Expression> message,
                                                                              TypedValue supplier,
                                                                              TypedValue getter,
                                                                              TypedValue noAgent,
                                                                              Agent supplierAgent) throws Exception
    {
        Expression<Boolean> o = quantToSupplier(cvs, obs);
        o = o.relabel((v) -> specializeSupplierCV(v, supplierAgent, supplier));
        o = o.relabel((v) -> supplyRename(v, supplier, getter, noAgent));
        return specialiseObservationToTransition(cvs, o, getGuard, message);
    }


    protected static Expression<Boolean> specialiseObservationToTransition(Map<String, Type> cvs,
                                                                            Expression<Boolean> obs,
                                                                            Expression<Boolean> sendGuard,
                                                                            Map<String, Expression> message) throws Exception {
        //handle message variables
        Expression<Boolean> observation = obs.relabel((v) -> message.getOrDefault(v.getName(), v));
        observation = observation.simplify();
        return handleCVsInObservation(cvs, observation, sendGuard).simplify();
    }

    public static Set<Expression<Boolean>> specialiseOnAllPossibleValues(String cv,
                                                                         Type type,
                                                                         Expression<Boolean> cond
                                                                    ) throws InfiniteValueTypeException,
                                                                            RelabellingTypeException,
                                                                            MismatchingTypeException,
                                                                            AttributeTypeException,
                                                                            TypeCreationException,
                                                                            AttributeNotInStoreException {
        //associate cv with all its possible values
        //instantiate condition for each value

        //get all cvs and order them
        List<TypedValue> vals = new ArrayList<>();
        if(type.getClass().equals(Boolean.class)){
            vals.add(Condition.getTrue());
            vals.add(Condition.getFalse());
        } else if(type.getClass().equals(BoundedInteger.class)){
            vals.addAll(((BoundedInteger) type).getAllValues());
        } else if(type.getClass().equals(Enum.class)){
            vals.addAll(((Enum) type).getAllValues());
        } else {
            throw new InfiniteValueTypeException("For LTOL analysis communication variables must be of finite type.");
        }

        Set<Expression<Boolean>> conditions = new HashSet<>();
        final String cv_cleaned = cv.replaceAll("^@", "");
        for(int j = 0; j < vals.size(); j++){
            TypedValue val = vals.get(j);
            conditions.add(cond.relabel((v) -> {
                String v_cleaned = v.getName().replaceAll("^@", "");
                if(v_cleaned.equals(cv_cleaned)){
                    return val;
                } else{
                    return v;
                }
            }).simplify());
        }

        return conditions;
    }

    /**  Statically transform exists(phi(cv)) and forall(phi(cv)) into phi(supplier-cv)
    */
    public static Expression<Boolean> quantToSupplier(Map<String, Type> cvs, Expression<Boolean> obs) throws RelabellingTypeException, MismatchingTypeException {
        if (obs instanceof And) {
            And obss = (And) obs;
            return new And(quantToSupplier(cvs, obss.getLhs()), quantToSupplier(cvs, obss.getRhs()));
        } else if (obs instanceof Or) {
            Or obss = (Or) obs;
            return new Or(quantToSupplier(cvs, obss.getLhs()), quantToSupplier(cvs, obss.getRhs()));
        } else if (obs instanceof Not) {
            Not obss = (Not) obs;
            return new Not(quantToSupplier(cvs, obss.getArgument()));
        } else if (obs instanceof Implies) {
            Implies obss = (Implies) obs;
            return new Implies(quantToSupplier(cvs, obss.getLhs()), quantToSupplier(cvs, obss.getRhs()));
        } else if (obs instanceof IsEqualTo) {
            IsEqualTo obss = (IsEqualTo) obs;
            return new IsEqualTo(quantToSupplier(cvs, obss.getLhs()), quantToSupplier(cvs, obss.getRhs()));
        } else if (obs instanceof Predicate) {
            Predicate obss = (Predicate) obs;
            Expression<Boolean> expr = obss.getInput();
            return expr.relabel((v) -> cvs.containsKey("@"+v.getName()) ? v.sameTypeWithName("supplier-" + v.getName()) : v);
        }

        return obs;
    }



    public static Expression<Boolean> handleCVsInObservation(Map<String, Type> cvs, Expression<Boolean> obs, Expression<Boolean> sendGuard) throws Exception {
        if(obs.getClass().equals(And.class)){
            And obss = (And) obs;
            return new And(handleCVsInObservation(cvs, obss.getLhs(), sendGuard), handleCVsInObservation(cvs, obss.getRhs(), sendGuard));
        } else if(obs.getClass().equals(Or.class)){
            Or obss = (Or) obs;
            return new Or(handleCVsInObservation(cvs, obss.getLhs(), sendGuard), handleCVsInObservation(cvs, obss.getRhs(), sendGuard));
        } else if(obs.getClass().equals(Not.class)){
            Not obss = (Not) obs;
            return new Not(handleCVsInObservation(cvs, obss.getArgument(), sendGuard));
        } else if(obs.getClass().equals(Implies.class)){
            Implies obss = (Implies) obs;
            return new Implies(handleCVsInObservation(cvs, obss.getLhs(), sendGuard), handleCVsInObservation(cvs, obss.getRhs(), sendGuard));
        } else if(obs.getClass().equals(IsEqualTo.class)){
            IsEqualTo obss = (IsEqualTo) obs;
            if(obss.getLhs().getType().equals(Boolean.getType())){
                return new And(
                        new Implies(handleCVsInObservation(cvs, obss.getLhs(), sendGuard), handleCVsInObservation(cvs, obss.getRhs(), sendGuard)),
                        new Implies(handleCVsInObservation(cvs, obss.getRhs(), sendGuard), handleCVsInObservation(cvs, obss.getLhs(), sendGuard)));
            } else {
                return obs;
            }
        } else if(obs.getClass().equals(Predicate.class)){
            Predicate obss = (Predicate) obs;
            //t(forall(o)) = /\cv g_s -> o

            if(obss.getName().equals("forall")){
                Expression<Boolean> finalExpression = null;

                Set<Expression<Boolean>> current = new HashSet<>();
                current.add(new Implies(sendGuard, obss.getInput()));

                for(Map.Entry<String, Type> entry : cvs.entrySet()){
                    Set<Expression<Boolean>> next = new HashSet<>();
                    for(Expression<Boolean> expr : current){
                        Set<Expression<Boolean>> nextExpressions = specialiseOnAllPossibleValues(entry.getKey(), entry.getValue(), expr);
                        if(nextExpressions.contains(Condition.getFalse())){
                            next.clear();
                            next.add(Condition.getFalse());
                            break;
                        }
                        nextExpressions.removeIf((p) -> p.equals(Condition.getTrue()));
                        next.addAll(nextExpressions);
                    }
                    current = next;
                    if(current.size() == 1 && current.contains(Condition.getFalse())){
                        break;
                    }
                }

                for(Expression<Boolean> expr : current){
                    if(finalExpression == null){
                        finalExpression = expr.simplify();
                    } else{
                        finalExpression = new And(finalExpression, expr).simplify();
                    }
                }
                return finalExpression;
            } else if(obss.getName().equals("exists")) {
                //t(exists(o)) = \/cv g_s && o
                Expression<Boolean> t = null;

                Set<Expression<Boolean>> current = new HashSet<>();
                current.add(new And(sendGuard, obss.getInput()));

                // TODO restrict to cvs that appear in obss
                for(Map.Entry<String, Type> nameTypePair : cvs.entrySet()){
                    Set<Expression<Boolean>> next = new HashSet<>();
                    for(Expression<Boolean> expr : current){
                        Set<Expression<Boolean>> nextExpressions = specialiseOnAllPossibleValues(nameTypePair.getKey(), nameTypePair.getValue(), expr);
                        if(nextExpressions.contains(Condition.getTrue())){
                            next.clear();
                            next.add(Condition.getTrue());
                            break;
                        }
                        nextExpressions.removeIf((p) -> p.equals(Condition.getFalse()));
                        next.addAll(nextExpressions);
                    }
                    current = next;
                    if(current.size() == 1 && current.contains(Condition.getTrue())){
                        break;
                    }
                }

                for(Expression<Boolean> expr : current){
                    if(t == null){
                        t = expr.simplify();
                    } else{
                        t = new Or(t, expr).simplify();
                    }
                }
                return t;
            } else{
                throw new Exception("Predicate " + obss.getName() + " unknown.");
            }
        } else{
            return obs;
        }
    }

    public static Pair<List<LTOL>, Map<String, Observation>> ltolToLTLAndObservationVariables(List<LTOL> specs) throws Exception {
        Integer counter = 0;
        List<LTOL> pureLTLSpecs = new ArrayList<>();
        Map<String, Observation> observations = new HashMap<>();

        for(int i = 0; i < specs.size(); i++){
            LTOL ltol = specs.get(i);
            Triple<Integer, Map<String, Observation>, LTOL> integerMapLTOLTriple = ltol.abstractOutObservations(counter);
            counter = integerMapLTOLTriple.getLeft();
            observations.putAll(integerMapLTOLTriple.getMiddle());
            pureLTLSpecs.add(integerMapLTOLTriple.getRight());
        }

        return new Pair<>(pureLTLSpecs, observations);
    }

    public static String nuxmvModelChecking(System system) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter("translation.smv"));
        String script = transform(system);
        writer.write(script);
        writer.close();
        Runtime rt = Runtime.getRuntime();
        String[] cmd = new String[] {Config.getNuxmvPath(), " translation.smv"};
        // Process pr = rt.exec(Config.getNuxmvPath() + " translation.smv");
        Process pr = rt.exec(cmd);

        AtomicReference<String> out = new AtomicReference<>("");

        new Thread(() -> {
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = null;

            try {
                while ((line = input.readLine()) != null) {
                    if(!line.startsWith("***") && !line.trim().equals(""))
                        out.set(out.get() + line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        pr.waitFor();

        return out.get();
    }

    public static String nuxmvTypeOfTypedVar(TypedVariable typedVariable){
        Type type = typedVariable.getType();
        if(type.getClass().equals(Enum.class)){
            return "{" + String.join(",", ((Enum) type).getValues()) + "}";
        } else{
            return type.name();
        }
    }

    public static String indent(String text){
        String s = text.replaceAll("(?<=(^|\n))", "    ");
        return s;
    }

    public static String transform(System system) throws Exception {
        return transform(system, false);
    }

    //TODO use sendTagsAsVars
    public static String transform(System system, boolean sendTagsAsVars) throws Exception {
        GuardReference.resolve = true;

        int unlabelledCounter = 0;

        String nuxmv = "MODULE main\n";
        String vars = "VAR\n";
        String broadcastChannel = "broadcast";

        String define = "DEFINE\n";
        String init = "INIT\n    TRUE\n";
        String trans = "";

        Pair<List<LTOL>, Map<String, Observation>> specsAndObs = ltolToLTLAndObservationVariables(system.getSpecs());
        List<LTOL> specs = specsAndObs.getLeft();
        Map<String, Observation> observations = specsAndObs.getRight();

        String noObservations = "no-observations := TRUE";

        for(Map.Entry<String, Observation> entry : observations.entrySet()){
            vars += "\t" + entry.getKey() + " : boolean;\n";
            noObservations += " & next(" + entry.getKey() + ") = FALSE";
        }

        for(String obs : observations.keySet()){
            init += "\t& " + obs + " = FALSE\n";
        }

        List<AgentInstance> agentInstances = system.getAgentInstances();
        Map<String, List<String>> agentSendPreds = new HashMap<>();
        Map<String, List<String>> agentSendProgressConds = new HashMap<>();
        TypedValue noAgent = Config.getNoAgent();

        Set<String> sendProcessNames = new HashSet<>();
        Set<String> receiveProcessNames = new HashSet<>();
        List<String> keepFunctions = new ArrayList<>();

        String keepAll = "keep-all := TRUE";

        for(String sendName : sendProcessNames){
            String keep = "keep-not-" + sendName + " := TRUE";
            for(String sendName2 : sendProcessNames){
                if(!sendName.equals(sendName2)){
                    keep += " & next(" + sendName2 + ") = " + sendName2;
                }
            }

            keepFunctions.add(keep);
        }

        for(int i = 0; i < agentInstances.size(); i++) {
            Agent agenti = agentInstances.get(i).getAgent();
            String namei = agentInstances.get(i).getLabel();

            String keepThis = "keep-all-" + namei + " := TRUE";

            for(String var : agenti.getStore().getAttributes().keySet()){
                keepThis += " & next(" + namei + "-" + var + ") = " + namei + "-" + var;
                keepThis += " & next(" + namei + "-automaton-state) = " + namei + "-automaton-state";
            }

            String falsifyAllLabelsNotOfI = "falsify-not-" + namei + " := TRUE";
            String falsifyAllLabelsOfI = "falsify-" + namei + " := TRUE";

            Set<Transition> receiveAndSupplyTransitions = new HashSet<>(agenti.getReceiveTransitions());
            // receiveAndSupplyTransitions.addAll(agenti.getSupplyTransitions());

            for(Transition t : receiveAndSupplyTransitions){
                String label = ((BasicProcess) t.getLabel()).getLabel();
                if (label != null && !label.equals("")){
                    keepThis += " & next(" + namei + "-" + label + ") = FALSE";
                    falsifyAllLabelsNotOfI += " & next(" + namei + "-" + label + ") = FALSE";
                    keepAll += " & next(" + namei + "-" + label + ") = FALSE";
                    falsifyAllLabelsOfI += " & next(" + namei + "-" + label + ") = FALSE";
                    receiveProcessNames.add(namei + "-" + label);

                    String falsifyAllLabelsExceptThis = "falsify-not-" + namei + "-" + label + " := TRUE";
                    for(Transition tt : receiveAndSupplyTransitions){
                        String receiveLabel = ((BasicProcess) tt.getLabel()).getLabel();
                        if(receiveLabel != null && !receiveLabel.equals(label) &&!receiveLabel.equals("")){
                            falsifyAllLabelsExceptThis += " & next(" + namei + "-" + receiveLabel + ") = FALSE";
                        }
                    }
                    keepFunctions.add(falsifyAllLabelsExceptThis);
                }
            }

            keepFunctions.add(falsifyAllLabelsNotOfI);
            keepFunctions.add(falsifyAllLabelsOfI);

            keepAll += " & keep-all-" + namei;

            keepFunctions.add(keepThis);
        }

        if(keepFunctions.size() > 0)
            define += "\t" + String.join(";\n\t", keepFunctions) + ";\n";
        define += "\t" + keepAll + ";\n";
        define += "\t" + noObservations + ";\n";


        List<String> progress = new ArrayList<>();
        List<String> getSupplyTrans = new ArrayList<>();

        Map<Agent, Map<State, Set<ProcessTransition>>> agentStateSendTransitionMap = new HashMap<>();
        Map<Agent, Map<State, Set<ProcessTransition>>> agentStateReceiveTransitionMap = new HashMap<>();
        Map<Agent, Map<State, Set<ProcessTransition>>> agentStateGetTransitionMap = new HashMap<>();
        Map<Agent, Map<State, Set<ProcessTransition>>> agentStateSupplyTransitionMap = new HashMap<>();

        for(Agent agent : system.getAgents()){
            agentStateSendTransitionMap.put(agent, agent.getStateTransitionMap(agent.getSendTransitions()));
            agentStateReceiveTransitionMap.put(agent, agent.getStateTransitionMap(agent.getReceiveTransitions()));
            agentStateGetTransitionMap.put(agent, agent.getStateTransitionMap(agent.getGetTransitions()));
            agentStateSupplyTransitionMap.put(agent, agent.getStateTransitionMap(agent.getSupplyTransitions()));
        }


        for(int i = 0; i < agentInstances.size(); i++) {
            AgentInstance sendingAgentInstance = agentInstances.get(i);
            Agent sendingAgent = sendingAgentInstance.getAgent();
            String sendingAgentName = sendingAgentInstance.getLabel();
            TypedValue sendingAgentNameValue = new TypedValue(Config.getAgentType(), sendingAgentName);

            // Declare sendingAgent's states as nuxmv variables
            Stream<String> allStates = sendingAgent.getStates().stream().map(s -> s.toString());
            String stateList = String.join(",", allStates.toArray(String[]::new));

            for (TypedVariable typedVariable : sendingAgent.getStore().getAttributes().values()) {
                vars += "\t" + sendingAgentName + "-" + typedVariable.getName() + " : " + nuxmvTypeOfTypedVar(typedVariable) + ";\n";
            }

            vars += "\t" + sendingAgentName + "-automaton-state" + " : {" + stateList + "};\n";
            ///////////////

            // Initialise sendingAgent's states and init condition
            if(!init.equals("INIT\n")){
                init += "\t& ";
            }

            init += sendingAgentName + "-automaton-state" + " = " + sendingAgent.getInitialState().toString() + "\n";

            init += "\t& " + sendingAgent.getInit().relabel(v -> ((TypedVariable) v).sameTypeWithName(sendingAgentName + "-" + v)) + "\n";
            init += "\t& " + sendingAgentInstance.getInit().relabel(v -> ((TypedVariable) v).sameTypeWithName(sendingAgentName + "-" + v)) + "\n";


            //For each state of the sending agents we are going to iterate over all of its possible send transitions
            // We shall create predicates for each send transition, and then disjunct them.
            for (State state : sendingAgent.getStates()) {
                Set<ProcessTransition> sendTransitions = agentStateSendTransitionMap.get(sendingAgent).get(state);

                String sendStateIsCurrentState = sendingAgentName + "-automaton-state" + " = " + state;

                if (sendTransitions != null && sendTransitions.size() > 0) {
                    List<String> transitionSendPreds = new ArrayList<>();
                    List<String> transitionSendProgressCond = new ArrayList<>();

                    for (ProcessTransition t : sendTransitions) {
                        //conditions for activation in now (is in source, and local guard holds),
                        // and effects in next (update, next state, and other stuff for receipt)
                        List<String> sendTriggeredIf = new ArrayList<>();
                        List<String> sendEffects = new ArrayList<>();

                        SendProcess sendingProcess = (SendProcess) t.getLabel();

                        Expression<Enum> sendingOnThisChannelVarOrVal = sendingProcess
                                .getChannel()
                                .relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v));

                        // add the guard of the sendingProcess to the guards required for the send to trigger
                        sendTriggeredIf.add(sendingProcess.getPsi()
                                .relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v)).simplify().toString());

                        sendEffects.add("falsify-" + sendingAgentName);
                        // add next state to send effects
                        sendEffects.add("next(" + sendingAgentName + "-automaton-state" + ") = " + t.getDestination());

                        // Add updates to send effects
                        for (Map.Entry<String, Expression> entry : sendingProcess.getUpdate().entrySet()) {
                            sendEffects.add(
                                    "next(" + sendingAgentName + "-" + entry.getKey() + ") " +
                                    "= (" + entry.getValue().relabel(v -> ((TypedVariable) v).sameTypeWithName(sendingAgentName + "-" + v)).simplify() + ")");
                        }
                        //keep variable values not mentioned in the update
                        for (String var : sendingAgent.getStore().getAttributes().keySet()) {
                            if (!sendingProcess.getUpdate().containsKey(var)) {
                                sendEffects.add("next(" + sendingAgentName + "-" + var + ") = " + sendingAgentName + "-" + var);
                            }
                        }

                        // relabelling message var names
                        Map<String, Expression> relabelledMessage = new HashMap<>();
                        for (Map.Entry<String, Expression> entry : sendingProcess.getMessage().entrySet()) {
                            relabelledMessage.put(entry.getKey(), entry.getValue().relabel(v -> ((TypedVariable) v).sameTypeWithName(sendingAgentName + "-" + ((TypedVariable) v).getName())).simplify());
                        }

                        //relabelling send agent variables in sendGuard
                        Expression<Boolean> sendGuardExpr = sendingProcess.getMessageGuard().relabel(v -> {
                            //if v is just the special variable we use in our syntax to refer to the current
                            // channel being sent on, then replace it with the sending transitions channel reference
                            if (v.getName().equals(Config.channelLabel)) {
                                return sendingOnThisChannelVarOrVal;
                            } else {
                                //relabelling local variables to those of the sending agents
                                return isCvRef(system, v.getName())
                                        ? v
                                        : v.sameTypeWithName(sendingAgentName + "-" + v);
                            }
                        }).simplify();

                        //Dealing with LTOL observations
                        for(Map.Entry<String, Observation> entry : observations.entrySet()){
                            Observation obs = entry.getValue();
                            String var = entry.getKey();

                            Expression<Boolean> observationCondition = specialiseObservationToSendTransition(
                                    commVariableReferences(system.getCommunicationVariables()),
                                    obs.getObservation(),
                                    sendGuardExpr,
                                    relabelledMessage,
                                    sendingAgentNameValue,
                                    noAgent,
                                    sendingOnThisChannelVarOrVal);

                            sendEffects.add("next(" + var + ") = (" + observationCondition + ")");
                        }

                        //Now we will iterate over all other agents, and for every receive transition,
                        // we create predicates for when the above send transition can trigger the receive transition.
                        List<String> agentReceivePreds = new ArrayList<>();
                        List<String> agentReceiveProgressConds = new ArrayList<>();

                        agentInstanceReceiveLoop:
                        for (int j = 0; j < agentInstances.size(); j++) {
                            if (i != j) {
                                AgentInstance receivingAgentInstance = agentInstances.get(j);
                                Agent receiveAgent = receivingAgentInstance.getAgent();
                                String receiveName = receivingAgentInstance.getLabel();

                                Expression<Boolean> receiveGuardExpr = receiveAgent.getReceiveGuard()
                                        .relabel(v -> v.toString().equals(Config.channelLabel)
                                                ? sendingOnThisChannelVarOrVal
                                                : v.sameTypeWithName(receiveName + "-" + v));
                                receiveGuardExpr = receiveGuardExpr.simplify();

                                String receiveGuard;

                                if (sendingOnThisChannelVarOrVal.toString().equals(Config.broadcast))
                                    //then sending channel is broadcast and we always want to listen to broadcasts
                                    receiveGuard = "TRUE";
                                else if (receiveGuardExpr.toString().equals("FALSE")) {
                                    receiveGuard = "FALSE";
                                } else if(sendingOnThisChannelVarOrVal.getClass().equals(TypedValue.class)){
                                    receiveGuard = "(" + receiveGuardExpr + ")";
                                } else {
                                    receiveGuard = "(" + receiveGuardExpr + ") | " + sendingOnThisChannelVarOrVal + " = " + broadcastChannel;
                                }

                                //relabelling sendGuard
                                // remove @s
                                Expression<Boolean> sendGuardExprHere = sendGuardExpr.relabel(v -> {
                                    return v.getName().startsWith("@") ? ((TypedVariable) v).sameTypeWithName(v.getName().substring(1)) : v;
                                });

                                // rename references to cvs to receiving agents cv
                                sendGuardExprHere = sendGuardExprHere.relabel(v -> {
                                    //if v is just the special variable we use in our syntax to refer to the current
                                    // channel being sent on, then replace it with the sending transitions channel reference
                                    try {
                                        //relabelling cvs to those of the receiving agents

                                        return isCvRef(system, v.getName())
                                                ? receiveAgent.getRelabel().get(v).relabel(vv -> ((TypedVariable) vv).sameTypeWithName(receiveName + "-" + vv))
                                                : v;
                                    } catch (RelabellingTypeException | MismatchingTypeException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }).simplify();


                                Map<String, List<String>> receiveAgentReceivePreds = new HashMap<>();
                                Map<String, List<String>> receiveAgentReceiveProgressConds = new HashMap<>();


                                if (!sendGuardExprHere.toString().equals("FALSE")) {
                                    for (State receiveAgentState : receiveAgent.getStates()) {
                                        Set<ProcessTransition> receiveAgentReceiveTransitions = agentStateReceiveTransitionMap.get(receiveAgent).get(receiveAgentState);
                                        if (receiveAgentReceiveTransitions == null) {
                                            receiveAgentReceiveTransitions = new HashSet<>();
                                        }

                                        String receiveStateIsCurrentState = receiveName + "-automaton-state" + " = " + receiveAgentState.toString();

                                        List<String> transitionReceivePreds = new ArrayList<>();
                                        List<String> transitionReceiveProgressConds = new ArrayList<>();

                                        receiveTransLoop:
                                        for (Transition receiveTrans : receiveAgentReceiveTransitions) {
                                            ReceiveProcess receiveProcess = (ReceiveProcess) receiveTrans.getLabel();

                                            List<String> receiveTransTriggeredIf = new ArrayList<>();
                                            List<String> receiveTransEffects = new ArrayList<>();

                                            //This is a hack to allow us to stop considering this transition
                                            // when the incoming message does not contain all message vars required
                                            // for this transition
                                            AtomicReference<java.lang.Boolean> stop = new AtomicReference<java.lang.Boolean>(false);
                                            Function<Expression, Expression> stopHelper = (e) -> {
                                                stop.set(true);
                                                return e;
                                            };

                                            // relabel the transition guard
                                            //     for message vars with the relabelled sending Agent messages and
                                            //     for local vars with the name of the agent
                                            Expression<recipe.lang.types.Boolean> receiveTransitionGuard = receiveProcess.getPsi()
                                                    .relabel(v -> sendingProcess.getMessage().containsKey(((TypedVariable) v).getName())
                                                            ? relabelledMessage.get(((TypedVariable) v).getName())
                                                            : (system.getMessageStructure().containsKey(((TypedVariable) v).getName())
                                                            ? stopHelper.apply(v)
                                                            : ((TypedVariable) v).sameTypeWithName(receiveName + "-" + v)));
                                            receiveTransitionGuard = receiveTransitionGuard.simplify();

                                            ////stop considering this transition if the incoming message does not contain all message vars required
                                            if (stop.get()) continue receiveTransLoop;
                                            ////////////////////////

                                            // if the receiveTransitionGuard has evaluated to false, then we can stop.
                                            if (receiveTransitionGuard.equals(Condition.getFalse()))
                                                continue receiveTransLoop;
                                            else
                                                receiveTransTriggeredIf.add(receiveTransitionGuard.toString());


                                            Expression<Enum> receivingOnThisChannelVarOrVal = receiveProcess.getChannel()
                                                    .relabel(v -> v.sameTypeWithName(receiveName + "-" + v.toString()));

                                            // if the sending and receiving transition channels are both values,
                                            // and they are not the same values then this transition can stop being
                                            // considered
                                            if (receivingOnThisChannelVarOrVal.getClass().equals(TypedValue.class)
                                                    && sendingOnThisChannelVarOrVal.getClass().equals(TypedValue.class)
                                                    && !sendingOnThisChannelVarOrVal.equals(receivingOnThisChannelVarOrVal))
                                                continue receiveTransLoop;
                                            else
                                                receiveTransTriggeredIf.add(sendingOnThisChannelVarOrVal + " = " + receivingOnThisChannelVarOrVal);
                                            //                                            agentReceiveNows.add(receiveNow);

                                            //for each variable update, if the updates uses a message variable that is
                                            // not set by the send transition, then exit
                                            // else relabel variables appropriately
                                            for (Map.Entry<String, Expression> entry : receiveProcess.getUpdate().entrySet()) {
                                                receiveTransEffects
                                                        .add("next(" + receiveName + "-" + entry.getKey() + ") = ("
                                                                + entry.getValue().relabel(v ->
                                                                sendingProcess.getMessage().containsKey(((TypedVariable) v).getName())
                                                                        ? relabelledMessage.get(((TypedVariable) v).getName())
                                                                        : (system.getMessageStructure().containsKey(((TypedVariable) v).getName())
                                                                        ? stopHelper.apply((TypedVariable) v)
                                                                        : ((TypedVariable) v).sameTypeWithName(receiveName + "-" + v))) + ")");
                                            }

                                            //stop considering this transition if update uses a message variable
                                            // that is not set by the send transition
                                            if (stop.get()) continue receiveTransLoop;
                                            ///////

                                            // keep the same variables for variables not mentioned in the update
                                            for (String var : receiveAgent.getStore().getAttributes().keySet()) {
                                                if (!receiveProcess.getUpdate().containsKey(var)) {
                                                    receiveTransEffects.add("next(" + receiveName + "-" + var + ") = " + receiveName + "-" + var);
                                                }
                                            }

                                            //if the receive transition is labelled, then set it's next value as true
                                            // and set all other labels of this agents as false
                                            if (receiveProcess.getLabel() != null && !receiveProcess.getLabel().equals("")) {
                                                receiveTransEffects.add("next(" + receiveName + "-" + receiveProcess.getLabel() + ") = TRUE");
                                                receiveTransEffects.add("falsify-not-" + receiveName + "-" + receiveProcess.getLabel() + "");
                                            } else {
                                                // if the transition is not labelled, then when it is taken no label
                                                // should be set as true
                                                receiveTransEffects.add("falsify-not-" + receiveName);
                                            }

                                            // set the transition destination state as the next state
                                            receiveTransEffects.add("next(" + receiveName + "-automaton-state" + ") = " + receiveTrans.getDestination());

                                            transitionReceivePreds.add("(" + String.join(")\n \t\t& (", receiveTransTriggeredIf) + ") & "
                                                    + "(" + String.join(")\n \t\t& (", receiveTransEffects) + ")");

                                            transitionReceiveProgressConds.add("(" + String.join(")\n \t\t& (", receiveTransTriggeredIf) + ")");
                                        }

                                        if (transitionReceivePreds.size() > 0) {
                                            receiveAgentReceivePreds.put(receiveStateIsCurrentState, transitionReceivePreds);
                                            receiveAgentReceiveProgressConds.put(receiveStateIsCurrentState, transitionReceiveProgressConds);
                                        }
                                    }
                                }

                                List<String> currentAgentReceivePreds = new ArrayList<>();
                                List<String> currentAgentProgressConds = new ArrayList<>();


                                for(State explicitState : receiveAgent.getStates()) {
                                    String stateCond = receiveName + "-automaton-state = " + explicitState.toString();
                                    String noExplicitTransition = sendingOnThisChannelVarOrVal.toString() + " = " + broadcastChannel;
                                    if (receiveAgentReceivePreds.containsKey(stateCond)
                                            && !(receiveAgentReceivePreds.get(stateCond) == null)){
                                        List<String> receiveTransPreds = receiveAgentReceivePreds.get(stateCond);
                                        List<String> receiveTransProgressConds = receiveAgentReceiveProgressConds.get(stateCond);
                                        if (receiveTransPreds.size() > 0) {
                                            currentAgentReceivePreds.add(stateCond + " & " + noExplicitTransition + " & keep-all-" + receiveName + " & !(" + String.join(" | ", receiveTransProgressConds) + ")");
                                            currentAgentProgressConds.add(stateCond + " & " + noExplicitTransition + " & !(" + String.join(" | ", receiveTransProgressConds) + ")");
                                        }
                                    }
                                    else {
                                        currentAgentReceivePreds.add(stateCond + " & " + noExplicitTransition + " & keep-all-" + receiveName);
                                        currentAgentProgressConds.add(stateCond + " & " + noExplicitTransition);
                                    }
                                }

                                //TRANSITION SEMANTICS

                                //This checks that the receive guard holds, the transition relation holds, and the send guard holds
                                // otherwise a disjunct is not added to the above lists
                                if (receiveAgentReceivePreds.size() > 0
                                        && !receiveGuard.equals("FALSE")) {
                                    //Compute transition predicate (and progress predicate) for current agent
                                    List<String> stateTransitionPreds = new ArrayList<>();
                                    List<String> stateTransitionProgressConds = new ArrayList<>();

                                    for (Map.Entry<String, List<String>> entry : receiveAgentReceivePreds.entrySet()) {
                                        stateTransitionPreds.add(entry.getKey() + " & ("
                                                + String.join(")\n \t\t| (", entry.getValue()) + ")");
                                    }
                                    for (Map.Entry<String, List<String>> entry : receiveAgentReceiveProgressConds.entrySet()) {
                                        stateTransitionProgressConds.add(entry.getKey() + " & ("
                                                + String.join(")\n \t\t| (", entry.getValue()) + ")");
                                    }

                                    String transitionPred = "(" + String.join(")\n \t\t| (", stateTransitionPreds) + ")";
                                    String transitionProgressCond = "(" + String.join(")\n \t\t| (", stateTransitionProgressConds) + ")";

                                    if (receiveGuard.equals("TRUE") && sendGuardExprHere.toString().equals("TRUE")) {
                                        currentAgentReceivePreds.add(transitionPred);
                                        currentAgentProgressConds.add(transitionProgressCond);
                                    } else if (receiveGuard.equals("TRUE")) {
                                        currentAgentReceivePreds.add("(" + transitionPred + ")\n \t\t& (" + sendGuardExprHere + ")");
                                        currentAgentProgressConds.add("(" + transitionProgressCond + ")\n \t\t& (" + sendGuardExprHere + ")");
                                    } else if (sendGuardExprHere.toString().equals("TRUE")) {
                                        currentAgentReceivePreds.add("(" + receiveGuard + ")\n \t\t& (" + transitionPred + ")");
                                        currentAgentProgressConds.add("(" + receiveGuard + ")\n \t\t& (" + transitionProgressCond + ")");
                                    } else {
                                        currentAgentReceivePreds.add("(" + receiveGuard + ")\n \t\t& (" + transitionPred + ")\n \t\t& (" + sendGuardExprHere + ")");
                                        currentAgentProgressConds.add("(" + receiveGuard + ")\n \t\t& (" + transitionProgressCond + ")\n \t\t& (" + sendGuardExprHere + ")");
                                    }
                                } else if(receiveAgentReceivePreds.size() > 0){
                                    throw new Exception("receiveAgentReceivePreds is empty");
                                }

                                //if receiveguard is false then the whole predicate is true
                                if (receiveGuard.equals("FALSE")){
                                    currentAgentReceivePreds.clear();
                                    currentAgentReceivePreds.add("keep-all-" + receiveName);

                                    currentAgentProgressConds.clear();
                                    currentAgentProgressConds.add("TRUE");
                                }
                                // if it is true then this disjunct is false (so add nothing)
                                else if(receiveGuard.equals("TRUE")){
                                    //do nothing
                                }
                                // otherwise add the receiveguard
                                else{
                                    currentAgentReceivePreds.add("!(" + receiveGuard + ") & keep-all-" + receiveName);
                                    currentAgentProgressConds.add("!(" + receiveGuard + ")");
                                }

                                if(sendingOnThisChannelVarOrVal.getClass().equals(TypedValue.class)
                                        && sendingOnThisChannelVarOrVal.toString().equals(Config.broadcast)
                                        && sendGuardExprHere.toString().equals("FALSE")){
                                    currentAgentReceivePreds.clear();
                                    currentAgentReceivePreds.add("keep-all-" + receiveName);
                                    currentAgentProgressConds.clear();
                                    currentAgentProgressConds.add("TRUE");
                                } else if((sendingOnThisChannelVarOrVal.getClass().equals(TypedValue.class)
                                        && !sendingOnThisChannelVarOrVal.toString().equals(Config.broadcast))
                                        || sendGuardExprHere.toString().equals("TRUE")){
                                    //do nothing, disjunct does not hold
                                } else{
                                    if(sendingOnThisChannelVarOrVal.getClass().equals(TypedValue.class)
                                            && sendingOnThisChannelVarOrVal.toString().equals(Config.broadcast)){
                                        currentAgentReceivePreds.add("!(" + sendGuardExprHere.toString() + ") & " + "keep-all-" + receiveName);
                                        currentAgentProgressConds.add("!(" + sendGuardExprHere.toString() + ")");
                                    } else if(sendGuardExprHere.toString().equals("FALSE")){
                                        currentAgentReceivePreds.add(sendingOnThisChannelVarOrVal.toString() + " = " + broadcastChannel + " & " + "keep-all-" + receiveName);
                                        currentAgentProgressConds.add(sendingOnThisChannelVarOrVal.toString() + " = " + broadcastChannel);
                                    } else{
                                        currentAgentReceivePreds.add(sendingOnThisChannelVarOrVal.toString() + " = " + broadcastChannel + " & !(" + sendGuardExprHere.toString() + ") & " + "keep-all-" + receiveName);
                                        currentAgentProgressConds.add(sendingOnThisChannelVarOrVal.toString() + " = " + broadcastChannel + " & !(" + sendGuardExprHere.toString() + ")");
                                    }
                                }


                                if(currentAgentReceivePreds.size() == 0){
                                    throw new Exception("currentAgentReceivePreds is empty");
                                }

                                agentReceivePreds.add("(" + String.join(")\n\n \t\t| (", currentAgentReceivePreds) + ")");
                                agentReceiveProgressConds.add("(" + String.join(")\n\n \t\t| (", currentAgentProgressConds) + ")");

                            }//end if(i != j)
                        }//for loop over agents for receive

                        String sendTriggeredIfPred = "(" + String.join(")\n \t\t& (", sendTriggeredIf) + ")";
                        String sendEffectsPred = "(" + String.join(")\n \t\t& (", sendEffects) + ")";

                        String agentSendPred = "(" + sendTriggeredIfPred + ")\n \t\t& (" + sendEffectsPred + ")";
                        String agentSendProgressCond = "(" + sendTriggeredIfPred + ")\n";
                        if(agentReceivePreds.size() > 0) {
                            agentSendPred += "\n \t\t& (" + String.join(")\n\n \t\t& (", agentReceivePreds) + ")";
                            agentSendProgressCond += "\n \t\t& (" + String.join(")\n\n \t\t& (", agentReceiveProgressConds) + ")";
                        }

                        if(sendingProcess.getLabel() != null && !sendingProcess.getLabel().equals("")) {
                            define +=  "\t" + sendingAgentName + "-" + sendingProcess.getLabel() + " := " + agentSendPred + ";\n";
                            transitionSendPreds.add(sendingAgentName + "-" + sendingProcess.getLabel());
                        } else {
                            transitionSendPreds.add(agentSendPred);
                        }

                        transitionSendProgressCond.add(agentSendProgressCond);
                    }
                    agentSendPreds.put(sendStateIsCurrentState, transitionSendPreds);
                    agentSendProgressConds.put(sendStateIsCurrentState, transitionSendProgressCond);
                }//endif

                Set<ProcessTransition> supplyTransitions = agentStateSupplyTransitionMap.get(sendingAgent).get(state);
                String supplierStateIsCurrentState = sendingAgentName + "-automaton-state" + " = " + state;
                if (supplyTransitions != null && supplyTransitions.size() > 0) {
                    for (ProcessTransition t : supplyTransitions) {
                        List<String> supplyTriggeredIf = new ArrayList<>();
                        List<String> supplyEffects = new ArrayList<>();
                        SupplyProcess supplyProcess = (SupplyProcess) t.getLabel();
                        // add the guard of the supplyProcess to the guards required for the supply to trigger
                        supplyTriggeredIf.add(supplierStateIsCurrentState);
                        supplyTriggeredIf.add(supplyProcess.getPsi().relabel(v -> v.sameTypeWithName(sendingAgentName + "-" + v)).simplify().toString());
                        
                        // supplyEffects.add("falsify-" + sendingAgentName);
                        // add next state to supply effects
                        supplyEffects.add("next(" + sendingAgentName + "-automaton-state" + ") = " + t.getDestination());

                        // Add supply updates
                        for (Map.Entry<String, Expression> entry : supplyProcess.getUpdate().entrySet()) {
                            supplyEffects.add(
                                    "next(" + sendingAgentName + "-" + entry.getKey() + ") " +
                                    "= (" + entry.getValue().relabel(v -> ((TypedVariable) v).sameTypeWithName(sendingAgentName + "-" + v)).simplify() + ")");
                        }
                        //keep variable values not mentioned in the update
                        for (String var : sendingAgent.getStore().getAttributes().keySet()) {
                            if (!supplyProcess.getUpdate().containsKey(var)) {
                                supplyEffects.add("next(" + sendingAgentName + "-" + var + ") = " + sendingAgentName + "-" + var);
                            }
                        }

                        // relabelling message var names
                        Map<String, Expression> relabelledMessage = new HashMap<>();
                        for (Map.Entry<String, Expression> entry : supplyProcess.getMessage().entrySet()) {
                            relabelledMessage.put(entry.getKey(), entry.getValue().relabel(v -> ((TypedVariable) v).sameTypeWithName(sendingAgentName + "-" + ((TypedVariable) v).getName())).simplify());
                        }
                        Location supplyLoc = supplyProcess.getLocation();
                        if (!(supplyLoc instanceof SelfLocation || supplyLoc instanceof AnyLocation)) {
                            throw new Exception(
                                "SUPPLY@" + supplyLoc.toString() + " not allowed " +
                                "(use either SUPPLY@SELF or SUPPLY@ANY).");
                        }

                        //Now we will iterate over all other agents, and for every get transition,
                        // we create predicates for when the above supply transition can trigger the get transition.
                        
                        for (int j = 0; j < agentInstances.size(); j++) {
                            if (i == j) continue;
                            AgentInstance getterInstance = agentInstances.get(j);
                            Agent getterAgent = getterInstance.getAgent();
                            String getterName = getterInstance.getLabel();
                            TypedValue getterNameValue = new TypedValue(Config.getAgentType(), getterName);

                            for (State getterState : getterAgent.getStates()) {
                                Set<ProcessTransition> getTransitions = agentStateGetTransitionMap.get(getterAgent).get(getterState);
                                if (getTransitions == null || getTransitions.size() == 0) continue; // no get's in this state, nothing to do
                                String getterStateIsCurrentState = getterName + "-automaton-state" + " = " + state;
                                
                                // We'll set this to true if we need to skip a transition
                                AtomicReference<java.lang.Boolean> stop = new AtomicReference<java.lang.Boolean>(false);
                                Function<Expression, Expression> stopHelper = (e) -> { stop.set(true); return e; };

                                getterTrLoop:
                                for (ProcessTransition gt : getTransitions) {
                                    List<String> getTriggeredIf = new ArrayList<>();
                                    getTriggeredIf.add(getterStateIsCurrentState);
                                    List<String> getEffects = new ArrayList<>();
                                    GetProcess getProcess = (GetProcess) gt.getLabel();
                                
                                    Expression<recipe.lang.types.Boolean> getTransitionGuard = getProcess.getPsi()
                                        .relabel(v -> supplyProcess.getMessage().containsKey(((TypedVariable) v).getName())
                                            ? relabelledMessage.get(((TypedVariable) v).getName())
                                            : (system.getMessageStructure().containsKey(((TypedVariable) v).getName())
                                            ? stopHelper.apply(v)
                                            : ((TypedVariable) v).sameTypeWithName(getterName + "-" + v)));
                                            
                                        ////stop considering this transition if the supplied message does not contain all message vars required
                                        if (stop.get()) continue getterTrLoop;
                                        // If guard evaluates to false, we can skip
                                        if (getTransitionGuard.equals(Condition.getFalse())) continue getterTrLoop;
                                        else getTriggeredIf.add(getTransitionGuard.toString());    


                                        // Handle message guard
                                        Location getterLoc = getProcess.getLocation();

                                        // Static checks
                                        if (supplyLoc instanceof SelfLocation && !(getterLoc instanceof NamedLocation)) continue getterTrLoop;

                                        Expression<Boolean> getterPredicate = getterLoc.getPredicate(sendingAgentNameValue);
                                        
                                        getterPredicate = getterPredicate.relabel(v -> {
                                            //relabelling local variables to those of the sending agents
                                            return isCvRef(system, v.getName())
                                                ? v
                                                : getterAgent.getStore().getAttributes().containsKey(v.getName()) 
                                                ? v.sameTypeWithName(getterName + "-" + v)
                                                : v;
                                        }).simplify();
                                        //relabelling getterGuard
                                        // remove @s
                                        getterPredicate = getterPredicate.relabel(v -> {
                                            return v.getName().startsWith("@") ? ((TypedVariable) v).sameTypeWithName(v.getName().substring(1)) : v;
                                        });

                                        // rename references to cvs to receiving agents cv
                                        getterPredicate = getterPredicate.relabel(v -> {
                                            //if v is just the special variable we use in our syntax to refer to the current
                                            // channel being sent on, then replace it with the sending transitions channel reference
                                            try {
                                                //relabelling cvs to those of the supplier
                                                return isCvRef(system, v.getName())
                                                        ? sendingAgent.getRelabel().get(v).relabel(vv -> ((TypedVariable) vv).sameTypeWithName(sendingAgentName + "-" + vv))
                                                        : v;
                                            } catch (RelabellingTypeException | MismatchingTypeException e) {
                                                e.printStackTrace();
                                            }
                                            return null;
                                        }).simplify();

                                        //Dealing with LTOL observations
                                        for(Map.Entry<String, Observation> entry : observations.entrySet()){
                                            Observation obs = entry.getValue();
                                            String var = entry.getKey();

                                            Expression<Boolean> observationCondition = specialiseObservationToSupplyTransition(
                                                    commVariableReferences(system.getCommunicationVariables()),
                                                    obs.getObservation(),
                                                    getterPredicate,
                                                    relabelledMessage,  
                                                    sendingAgentNameValue,
                                                    getterNameValue,
                                                    noAgent,
                                                    sendingAgent);

                                            getEffects.add("next(" + var + ") = (" + observationCondition + ")");
                                        }

                                        //for each variable update, if the updates uses a message variable that is
                                        // not set by the send transition, then exit
                                        // else relabel variables appropriately
                                        for (Map.Entry<String, Expression> entry : getProcess.getUpdate().entrySet()) {
                                            getEffects.add("next(" + getterName + "-" + entry.getKey() + ") = ("
                                            + entry.getValue().relabel(v ->
                                            supplyProcess.getMessage().containsKey(((TypedVariable) v).getName())
                                            ? relabelledMessage.get(((TypedVariable) v).getName())
                                            : (system.getMessageStructure().containsKey(((TypedVariable) v).getName())
                                            ? stopHelper.apply((TypedVariable) v)
                                            : ((TypedVariable) v).sameTypeWithName(getterName + "-" + v))) + ")");
                                        }
                                        
                                        // Stop considering this transition if update uses a message variable
                                        // that is not set by the send transition
                                        if (stop.get()) continue getterTrLoop;
                                        ///////
                                        
                                        // keep the same variables for variables not mentioned in the update
                                        for (String var : getterAgent.getStore().getAttributes().keySet()) {
                                            if (!getProcess.getUpdate().containsKey(var)) {
                                                getEffects.add("next(" + getterName + "-" + var + ") = " + getterName + "-" + var);
                                            }
                                        }
                                        
                                        // Add the destination state to getter effects
                                        getEffects.add("next(" + getterName + "-automaton-state" + ") = " + gt.getDestination());
                                        
                                        // Keep all other agents as they are
                                        for (AgentInstance other : agentInstances) {
                                            String otherName = other.getLabel();
                                            if (otherName != sendingAgentName && otherName != getterName)
                                                getEffects.add(String.format("keep-all-%s", otherName));
                                        }
                                        // TODO add "falsify-" defines to allow mentioning labelled get/supply actions in specs
 
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

                                        String getterGuardStr = getterPredicate.toString();

                                        define += String.format(
                                            "\t%s := (%s)\n\t\t& (%s)\n\t\t& (%s)\n\t\t& (%s) \n\t\t& (%s);\n",
                                            lbl,
                                            String.join(" & ", supplyTriggeredIf),
                                            String.join(" & ", supplyEffects),
                                            String.join(" & ", getTriggeredIf),
                                            getterGuardStr,
                                            String.join(" & ", getEffects));
                                        getSupplyTrans.add(lbl);
                                        
                                        define += String.format(
                                            "\t%s-progress := (%s)\n\t\t& (%s)\n\t\t& (%s);\n",
                                            lbl,
                                            String.join(" & ", supplyTriggeredIf),
                                            String.join(" & ", getTriggeredIf),
                                            getterGuardStr,
                                        progress.add(String.format("%s-progress", lbl)));
                                        
                                    } // getter transition loop
                            } //getter state loop
                        } // getter agent loop
                    } // supplier transition loop
                } //Supplier/sender state loop
            } // Supplier/sender agent loop
        }

        if(agentSendPreds.size() > 0) {
            List<String> stateTransitionPreds = new ArrayList<>();
            List<String> stateTransitionProgressConds = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : agentSendPreds.entrySet()) {
                stateTransitionPreds.add(entry.getKey() + "\n\n \t\t& ((" + String.join(")\n\n \t\t| (", entry.getValue()) + "))");
            }
            for (Map.Entry<String, List<String>> entry : agentSendProgressConds.entrySet()) {
                stateTransitionProgressConds.add(entry.getKey() + "\n\n \t\t& ((" + String.join(")\n\n \t\t| (", entry.getValue()) + "))");
            }

            trans += "(" + String.join(")\n\n \t\t| (", stateTransitionPreds) + ")";
            if (getSupplyTrans.size() > 0) trans += "\n\n\t\t| ";

            progress.addAll(stateTransitionProgressConds);
        }
        
        if (getSupplyTrans.size() > 0) {
            trans += "(" + String.join(")\n\n \t\t| (", getSupplyTrans) + ")";
        }

        
        if (trans.strip() == "") trans = "FALSE";
        trans += ";\n";

        for(String name : receiveProcessNames){
            vars += "\t" + name + " : " + "boolean;\n";
        }
        nuxmv += vars;
        define += "\ttransition := " + trans;
        if(progress.size() > 0)
            define += "\tprogress := (" + String.join(")\n \t\t| (", progress) + ");\n";
        else
            define += "\tprogress := FALSE;\n";
        nuxmv += define;
        List<String> constants = new ArrayList<>();
        for(String label : Enum.getEnumLabels()){
            constants.addAll(Enum.getEnum(label).getValues());
        }
        if(constants.size() > 0)
            nuxmv += "CONSTANTS\n\t";
            nuxmv +=  String.join(", ", constants);
            nuxmv += ";\n";

        for(String name : receiveProcessNames){
            init += "\t& " + name + " = " + "FALSE\n";
        }

        nuxmv += init;
        nuxmv += "TRANS\n";
        nuxmv += "\t(transition)\n \t\t| (!progress & keep-all & no-observations)\n";
        nuxmv = nuxmv.replaceAll("&( |\n)*TRUE(( )*&( )*)( |\n)*", "");
        nuxmv = nuxmv.replaceAll("TRUE(( )*&( )*)", "");
        nuxmv = nuxmv.replaceAll("==", " = ");
        nuxmv = nuxmv.replaceAll("\\*", broadcastChannel);


        nuxmv += String.join("\n", specs.stream().map((s) -> "LTLSPEC " + s.toString()).toArray(String[]::new));

        GuardReference.resolve = false;

        return nuxmv;
    }

}
