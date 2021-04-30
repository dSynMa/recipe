package recipe.analysis;

import recipe.lang.System;
import recipe.lang.agents.*;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.BooleanValue;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.strings.StringVariable;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;
import recipe.lang.utils.Pair;
import recipe.lang.utils.TypingContext;

import java.util.*;
import java.util.stream.Stream;

public class ToNuXmv {

    public static String type(TypedVariable typedVariable, System system){
        if(typedVariable.getClass().equals(NumberVariable.class)){
            return "real";
        } else if(typedVariable.getClass().equals(ChannelVariable.class)){
            return "{" + String.join(", ", system.getChannels().stream().map(a -> a.getValue()).toArray(String[]::new)) + "}";
        } else if(typedVariable.getClass().equals(BooleanVariable.class)) {
            return "boolean";
        } else if(typedVariable.getClass().equals(StringVariable.class)) {
            return "STRING";
        } else {
            return "UNKNOWN";
        }
    }

    public static String indent(String text){
        String s = text.replaceAll("(?<=(^|\n))", "    ");
        return s;
    }

    //TODO This is not the exact semantics
    public static String transformSendAndReceiveSteps(System system) throws RelabellingTypeException {
        String nuxmv = "MODULE main\n";
        String vars = "VAR\n";
        String[] agentsNames = system.getAgents().stream().map(a -> a.getName().trim().toLowerCase()).toArray(String[]::new);
        vars += "\tsending : boolean;\n";
        vars += "\tsendingAgent : {" + String.join(", ", agentsNames) + "};\n";
        for(String s : agentsNames){
            vars += "\treceiving-" + s + " : boolean;\n";
        }
        Set<String> channelVals = new HashSet(Arrays.asList(system.getChannels().stream().map(a -> a.getValue().toLowerCase()).toArray(String[]::new)));
        String anyChannel = "broadcast";
        channelVals.add(anyChannel);
        vars += "\tchannel : {" + String.join(", ",channelVals)  + "};\n";
        for(TypedVariable typedVariable : system.getMessageStructure().values()){
            vars += "\t" + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
        }

        String define = "DEFINE\n";
        String init = "INIT\n";
        init += "\tsending = TRUE\n";
        String trans = "TRANS\n";

        List<Agent> agents = new ArrayList<>(system.getAgents());
        List<String> agentSendPreds = new ArrayList<>();
        List<String> agentReceivePreds = new ArrayList<>();

        List<String> keepFunctions = new ArrayList<>();
        String keepAll = "keep-all := TRUE";
        List<String> blocking = new ArrayList<>();
        for(int i = 0; i < agents.size(); i++) {
            Agent agent = agents.get(i);
            String name = agent.getName();

            String keep = "keep-all-not-" + name + " := TRUE";
            String keepThis = "keep-all-" + name + " := TRUE";
            for(int j = 0; j < agents.size(); j++) {
                Agent agent1 = agents.get(j);
                String name1 = agent1.getName();
                if(j != i){
                    for(String var : agent1.getStore().getAttributes().keySet()){
                        keep += " & next(" + name1 + "-" + var + ") = " + name1 + "-" + var;
                    }
                    keep += " & next(" + name1 + ".state) = " + name1 + ".state";
                }
            }
            keepFunctions.add(keep);

            for(String var : agent.getStore().getAttributes().keySet()){
                keepThis += " & next(" + name + "-" + var + ") = " + name + "-" + var;
                keepAll += " & next(" + name + "-" + var + ") = " + name + "-" + var;
            }
            keepAll += " & next(" + name + ".state) = " + name + ".state";
            keepFunctions.add(keepThis);
        }

        define += "\t" + String.join(";\n\t", keepFunctions) + ";\n";
        define += "\t" + keepAll + ";\n";

        int choice = 0;

        List<String> sendNows = new ArrayList<>();
        List<String> receiveNows = new ArrayList<>();

        for(int i = 0; i < agents.size(); i++){
            Agent agent = agents.get(i);
            String name = agent.getName();

            Stream<String> allStates = agent.getStates().stream().map(s -> name + "-" + s.toString());
            String stateList = String.join("," , allStates.toArray(String[]::new));

            for(TypedVariable typedVariable : agent.getStore().getAttributes().values()){
                vars += "\t" + name + "-" + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
            }
//            for(TypedVariable typedVariable : system.getCommunicationVariables().values()){
//                vars += "\t" + name + "-" + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
//            }
//            vars += "\t" + name + ".channel : {" + String.join(", ", system.getChannels().stream().map(a -> a.getValue()).toArray(String[]::new)) + "};\n";
            vars += "\t" + name + ".state" + " : {" + stateList + "};\n";
            init += "\t& " + name + ".state" + " = " + name + "-" + agent.getInitialState().toString() + "\n";
            init += "\t& receiving-" + name + " = FALSE\n";

            for(Map.Entry<String, TypedValue> entry : agent.getStore().getData().entrySet()){
                init += "\t& " + name + "-" + entry.getKey() + " = " + entry.getValue().getValue() + "\n";
            }
            define += "\treceive-guard-" + name + " := channel == * | (" + agent.getReceiveGuard().relabel(v -> system.getCommunicationVariables().containsKey(v.getName()) ? v : v.getName().equals("channel") ? v : v.sameTypeWithName(name + "-" + v)) + ");\n";

            Map<State, Set<ProcessTransition>> stateSendTransitionMap = new HashMap<>();
            Map<State, Set<ProcessTransition>> stateReceiveTransitionMap = new HashMap<>();
            Map<State, Set<IterationExitTransition>> stateIterationExitTransitionMap = new HashMap<>();
            stateSendTransitionMap.putAll(agent.getStateTransitionMap(agent.getSendTransitions()));
            stateReceiveTransitionMap.putAll(agent.getStateTransitionMap(agent.getReceiveTransitions()));
            stateIterationExitTransitionMap.putAll(agent.getStateTransitionMap(agent.getIterationExitTransitions()));

            List<String> stateReceivePreds = new ArrayList<>();

            List<String> stateSendPreds = new ArrayList<>();
            for(State state : agent.getStates()) {
                Set<ProcessTransition> sendTransitions = stateSendTransitionMap.get(state);
                Set<ProcessTransition> receiveTransitions = stateReceiveTransitionMap.get(state);
                Set<IterationExitTransition> iterationExitTransitions = stateIterationExitTransitionMap.get(state);


                if (sendTransitions != null && sendTransitions.size() > 0) {
                    List<String> transitionSendPreds = new ArrayList<>();

                    String stateSendPred = "";
                    for (ProcessTransition t : sendTransitions) {
                        //conditions for activation in now (is in source, and local guard holds),
                        // and effects in next (update, next state, and other stuff for receipt)

                        String now = "";
                        String next = "";
                        SendProcess process = (SendProcess) t.getLabel();
                        now += "" + name + ".state" + " = " + name + "-" + t.getSource();
                        now += " & (" + process.entryCondition().relabel(v -> v.sameTypeWithName(name + "-" + v)) + ")";
                        now += " & !blocked-" + choice;

                        String blockingPred = "blocked-" + choice + " := " + process.getChannel().relabel(v -> v.sameTypeWithName(agent.getName() + "-" + v)) + " != " + anyChannel;
                        for(int j = 0; j < agents.size(); j++) {
                            Agent agent1 = agents.get(j);
                            String name1 = agent1.getName();
                            if (j != i) {
                                blockingPred += " & (";
                                blockingPred += "receive-guard-" + agents.get(j).getName();
                                blockingPred += " -> !(" +  process.getMessageGuard().relabel(v -> {
                                    try {
                                        return system.getCommunicationVariables().containsKey(v.getName()) ? agent1.getRelabel().get(v).relabel(vv -> vv.sameTypeWithName(name1 + "-" + vv)) : v.sameTypeWithName(name + "-" + v);
                                    } catch (RelabellingTypeException e) {
                                        e.printStackTrace();
                                    }
                                    //TODO should not get here
                                    return null;
                                });
                                blockingPred += "))";
                            }
                        }

                        blocking.add((((blockingPred))));

                        sendNows.add(now);

                        next += "next(channel) = " + process.getChannel().relabel(v -> v.sameTypeWithName(agent.getName() + "-" + v));
                        next += "\n & next(sendingAgent) = " + name;

                        for (Map.Entry<String, Expression> entry : process.getMessage().entrySet()) {
                            next += "\n & (next(" + entry.getKey() + ") = " + entry.getValue().relabel(v -> v.sameTypeWithName(name + "-" + v)) + ")";
                        }

                        for(int j = 0; j < agents.size(); j++){
                            if(j != i){
                                Agent agent1 = agents.get(j);
                                String name1 = agent1.getName();
                                next += "\n & next(receiving-" + name1 + ") = (";
//                                transPred += "(" + anyChannel + " = " + process.getChannel().relabel(v -> v.sameTypeWithName(agent.getName() + "-" + v));
//                                transPred += "|" + agent1.getReceiveGuard().relabel(v -> v.sameTypeWithName(name1 + "-" + v)) + ")";
                                next += process.getMessageGuard().relabel(v -> {
                                    try {
                                        return system.getCommunicationVariables().containsKey(v.getName()) ? agent1.getRelabel().get(v).relabel(vv -> vv.sameTypeWithName(name1 + "-" + vv)) : v.sameTypeWithName(name + "-" + v);
                                    } catch (RelabellingTypeException e) {
                                        e.printStackTrace();
                                    }
                                    //TODO should not get here
                                    return null;
                                });
                                next += ")";
                            }
                        }

                        next += "\n & ";

                        if(stateIterationExitTransitionMap.containsKey(t.getDestination())
                            && stateIterationExitTransitionMap.get(t.getDestination()) != null
                            && stateIterationExitTransitionMap.get(t.getDestination()).size() > 0){
                            Set<IterationExitTransition> destItExit = stateIterationExitTransitionMap.get(t.getDestination());
                            List<String> exitConds = new ArrayList<>();
                            next += "case\n";
                            for(IterationExitTransition tt : destItExit){
                                String exitCond = tt.getLabel().relabel(v -> v.sameTypeWithName(name + "-" + v)).toString();
                                exitConds.add(exitCond);
                                next += "\t\t" + exitCond + " : next(" + name + ".state" + ") = " + t.getDestination() + ");";
                            }
                            next += "\t\t TRUE : next(" + name + ".state" + ") = " + name + "-" + t.getDestination() + ");";
                            next += "\n \tesac";
                        } else{
                            next += "next(" + name + ".state" + ") = " + name + "-" + t.getDestination();
                        }

                        for (Map.Entry<String, Expression> entry : process.getUpdate().entrySet()) {
                            next += "\n & next(" + name + "-" + entry.getKey() + ") = (" + entry.getValue().relabel(v -> v.sameTypeWithName(name + "-" + v)) + ")";
                        }
                        for (String var : agent.getStore().getAttributes().keySet()) {
                            if (!process.getUpdate().containsKey(var)) {
                                next += "\n & " + "next(" + name + "-" + var + ") = " + name + "-" + var;
                            }
                        }
                        next += "\n & keep-all-not-" + name;
                        next += "\n & next(sending) = FALSE";

                        transitionSendPreds.add("(" + now + ") & " + indent(indent(indent("\n" + next))));
                        choice++;
                    }
//                    stateSendPred += String.join(";\n", transitionSendPreds) + ";";

                    stateSendPreds.addAll(transitionSendPreds);
                }

                String stateReceivePrd = "";
                if (receiveTransitions != null && receiveTransitions.size() > 0) {
                    List<String> transitionReceivePreds = new ArrayList<>();
                    for (Transition t : receiveTransitions) {
                        ReceiveProcess process = (ReceiveProcess) t.getLabel();

                        String now = "";
                        String next = "";
                        now += "receiving-" + name;
                        now += "\n & receive-guard-" + name;
                        now += "\n & " + name + ".state" + " = " + name + "-" + state.toString();
                        now += "\n & " + process.getPsi().relabel(v -> v.sameTypeWithName(name + "-" + v)).toString();
                        now += "\n & (channel = " + anyChannel + " | channel = " + process.getChannel().relabel(v -> v.sameTypeWithName(name + "-" + v.toString().toLowerCase())).toString() +")";
                        receiveNows.add(now);

                        for (Map.Entry<String, Expression> entry : process.getUpdate().entrySet()) {
                            next += "\n & (" + "next(" + name + "-" + entry.getKey() + ") = " + entry.getValue().relabel(v -> system.getMessageStructure().containsKey(v.getName()) ? v : v.sameTypeWithName(name + "-" + v)) + ")";
                        }
                        for (String var : agent.getStore().getAttributes().keySet()) {
                            if (!process.getUpdate().containsKey(var)) {
                                next += "\n & (" + "next(" + name + "-" + var + ") = " + name + "-" + var + ")";
                            }
                        }

                        if(stateIterationExitTransitionMap.containsKey(t.getDestination())
                                && stateIterationExitTransitionMap.get(t.getDestination()) != null
                                && stateIterationExitTransitionMap.get(t.getDestination()).size() > 0){
                            Set<IterationExitTransition> destItExit = stateIterationExitTransitionMap.get(t.getDestination());
                            List<String> exitConds = new ArrayList<>();
                            for(IterationExitTransition tt : destItExit){
                                String exitCond = tt.getLabel().relabel(v -> v.sameTypeWithName(name + "-" + v)).toString();
                                exitConds.add(exitCond);
                                next += "\n & (" + exitCond + " -> next(" + name + ".state" + ") = " + name + "-" + t.getDestination() + ")";
                            }
                            next += "\n & (!(" + String.join(" | ", exitConds) + ") -> next(" + name + ".state" + ") = " + name + "-" + t.getDestination() + ")";
                            trans += ")";
                        } else{
                            next += "\n & next(" + name + ".state" + ") = " + name + "-" + t.getDestination();
                        }

                        next = next.replaceAll("^\n *&", "");
                        transitionReceivePreds.add("(" + now + ") & " + indent(indent(indent("\n" + next))));
                    }

                    stateReceivePrd += String.join(";\n", transitionReceivePreds) + ";";
                    stateReceivePreds.add(stateReceivePrd);
                }
            }

            stateReceivePreds.add("!((" + String.join(") | (", receiveNows) + ")) & keep-all-" + name);
            agentReceivePreds.add("\n" + indent(String.join("\n | ", stateReceivePreds)) + "\n");

            agentSendPreds.addAll(stateSendPreds);;
        }

//        define += "\t" + String.join(";\n\t", agentReceivePreds) + ";\n";
//        vars += "\tsendingChoice : 0.." + (choice - 1) + ";\n"

        trans += "\tcase";
        trans += "\n\t sending : ";
        trans += "\n\t\t (";
        trans += "\n" + indent(indent(indent("(" + String.join(")\n| (", agentSendPreds)))) + ")";
        trans += "\n\t\t\t| !((" + String.join(") | (", sendNows) + ")) & keep-all & next(sending) = TRUE";
        trans += "\n\t\t );";
        trans += "\n\t !sending :";
        trans += "\n\t\t next(sending) = TRUE";
        trans += "\n\t\t & (" + indent(indent((String.join(") & (", agentReceivePreds)))) + ")";
        trans += "\n\t esac;";

        nuxmv += vars;
        nuxmv += define + "\t" + String.join(";\n\t",  blocking) + ";\n";
        nuxmv += init;
        nuxmv += trans;
//        nuxmv = nuxmv.replaceAll("TRUE(( )*&( )*)", "");
//        nuxmv = nuxmv.replaceAll("TRUE\n(\t& )", "\t");
        nuxmv = nuxmv.replaceAll("==", " = ");
        nuxmv = nuxmv.replaceAll("\\*", anyChannel);

        return nuxmv;
    }

    public static String transform(System system) throws RelabellingTypeException {
        String nuxmv = "MODULE main\n";
        String vars = "VAR\n";
        String broadcastChannel = "broadcast";

        String define = "DEFINE\n";
        String init = "INIT\n";
        String trans = "TRANS\n";

        List<Agent> agents = new ArrayList<>(system.getAgents());
        List<String> agentSendPreds = new ArrayList<>();

        List<String> keepFunctions = new ArrayList<>();
        String keepAll = "keep-all := TRUE";

        for(int i = 0; i < agents.size(); i++) {
            Agent agent = agents.get(i);
            String name = agent.getName();

            String keep = "keep-all-not-" + name + " := TRUE";
            String keepThis = "keep-all-" + name + " := TRUE";
            for(int j = 0; j < agents.size(); j++) {
                Agent agent1 = agents.get(j);
                String name1 = agent1.getName();
                if(j != i){
                    for(String var : agent1.getStore().getAttributes().keySet()){
                        keep += " & next(" + name1 + "-" + var + ") = " + name1 + "-" + var;
                    }
                    keep += " & next(" + name1 + ".state) = " + name1 + ".state";
                }
            }
            keepFunctions.add(keep);

            for(String var : agent.getStore().getAttributes().keySet()){
                keepThis += " & next(" + name + "-" + var + ") = " + name + "-" + var;
                keepAll += " & next(" + name + "-" + var + ") = " + name + "-" + var;
            }
            keepAll += " & next(" + name + ".state) = " + name + ".state";
            keepFunctions.add(keepThis);
        }

        define += "\t" + String.join(";\n\t", keepFunctions) + ";\n";
        define += "\t" + keepAll + ";\n";

        init += "\tTRUE\n";

        List<String> sendNows = new ArrayList<>();
        List<String> receiveNows = new ArrayList<>();

        for(int i = 0; i < agents.size(); i++) {
            Agent agent = agents.get(i);
            String name = agent.getName();

            Stream<String> allStates = agent.getStates().stream().map(s -> name + "-" + s.toString());
            String stateList = String.join(",", allStates.toArray(String[]::new));

            for (TypedVariable typedVariable : agent.getStore().getAttributes().values()) {
                vars += "\t" + name + "-" + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
            }

            vars += "\t" + name + ".state" + " : {" + stateList + "};\n";
            init += "\t& " + name + ".state" + " = " + name + "-" + agent.getInitialState().toString() + "\n";

            for (Map.Entry<String, TypedValue> entry : agent.getStore().getData().entrySet()) {
                init += "\t& " + name + "-" + entry.getKey() + " = " + entry.getValue().getValue() + "\n";
            }
//            define += "\treceive-guard-" + name + " := (" + agent.getReceiveGuard().relabel(v -> system.getCommunicationVariables().containsKey(v.getName()) ? v : v.getName().equals("channel") ? v : v.sameTypeWithName(name + "-" + v)) + ");\n";

            Map<State, Set<ProcessTransition>> stateSendTransitionMap = new HashMap<>();
            Map<State, Set<ProcessTransition>> stateReceiveTransitionMap = new HashMap<>();
            Map<State, Set<IterationExitTransition>> stateIterationExitTransitionMap = new HashMap<>();
            stateSendTransitionMap.putAll(agent.getStateTransitionMap(agent.getSendTransitions()));
            stateReceiveTransitionMap.putAll(agent.getStateTransitionMap(agent.getReceiveTransitions()));
            stateIterationExitTransitionMap.putAll(agent.getStateTransitionMap(agent.getIterationExitTransitions()));

            List<String> stateSendPreds = new ArrayList<>();
            for (State state : agent.getStates()) {
                Set<ProcessTransition> sendTransitions = stateSendTransitionMap.get(state);
                Set<ProcessTransition> receiveTransitions = stateReceiveTransitionMap.get(state);
                Set<IterationExitTransition> iterationExitTransitions = stateIterationExitTransitionMap.get(state);


                if (sendTransitions != null && sendTransitions.size() > 0) {
                    List<String> transitionSendPreds = new ArrayList<>();

                    String stateSendPred = "";
                    for (ProcessTransition t : sendTransitions) {
                        //conditions for activation in now (is in source, and local guard holds),
                        // and effects in next (update, next state, and other stuff for receipt)
                        String now = "";
                        String next = "";
                        SendProcess process = (SendProcess) t.getLabel();

                        String channel = process.getChannel().relabel(v -> v.sameTypeWithName(agent.getName() + "-" + v)).toString();
                        now += "" + name + ".state" + " = " + name + "-" + t.getSource();
                        now += " & (" + process.entryCondition().relabel(v -> v.sameTypeWithName(name + "-" + v)) + ")";

                        sendNows.add(now);

                        if (iterationExitTransitions != null && iterationExitTransitions.size() > 0) {
                            Set<IterationExitTransition> destItExit = stateIterationExitTransitionMap.get(t.getDestination());
                            List<String> exitConds = new ArrayList<>();
                            next += "\n & case\n";
                            for (IterationExitTransition tt : destItExit) {
                                String exitCond = tt.getLabel().relabel(v -> v.sameTypeWithName(name + "-" + v)).toString();
                                exitConds.add(exitCond);
                                next += "\t\t" + exitCond + " : next(" + name + ".state" + ") = " + t.getDestination() + ");";
                            }
                            next += "\t\t TRUE : next(" + name + ".state" + ") = " + name + "-" + t.getDestination() + ");";
                            next += "\n \tesac";
                        } else {
                            next += "\n & next(" + name + ".state" + ") = " + name + "-" + t.getDestination();
                        }

                        for (Map.Entry<String, Expression> entry : process.getUpdate().entrySet()) {
                            next += "\n & next(" + name + "-" + entry.getKey() + ") = (" + entry.getValue().relabel(v -> v.sameTypeWithName(name + "-" + v)) + ")";
                        }
                        for (String var : agent.getStore().getAttributes().keySet()) {
                            if (!process.getUpdate().containsKey(var)) {
                                next += "\n & " + "next(" + name + "-" + var + ") = " + name + "-" + var;
                            }
                        }

                        List<String> agentReceivePreds = new ArrayList<>();
                        //TODO deal with messages
                        for (int j = 0; j < agents.size(); j++) {
                            if (i != j) {
                                Agent receiveAgent = agents.get(j);
                                String receiveName = receiveAgent.getName();

                                String receiveGuard = receiveAgent.getReceiveGuard().relabel(v -> v.sameTypeWithName(v.toString().equals("channel") ? v.toString() : receiveName + "-" + v)).toString().replaceAll("channel", channel);

                                String sendGuard = "(" + process.getMessageGuard().relabel(v -> {
                                    try {
                                        return system.getCommunicationVariables().containsKey(v.getName()) ? receiveAgent.getRelabel().get(v).relabel(vv -> vv.sameTypeWithName(receiveName + "-" + vv)) : v.sameTypeWithName(name + "-" + v);
                                    } catch (RelabellingTypeException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }) + ")";

                                String stateReceivePrd = "";
                                Set<ProcessTransition> receiveAgentReceiveTransitions = receiveAgent.getReceiveTransitions();
                                if (receiveAgentReceiveTransitions != null && receiveAgentReceiveTransitions.size() > 0) {
                                    List<String> transitionReceivePreds = new ArrayList<>();
                                    for (Transition receiveTrans : receiveAgentReceiveTransitions) {
                                        ReceiveProcess receiveProcess = (ReceiveProcess) receiveTrans.getLabel();

                                        String receiveNow = "";
                                        String receiveNext = "";
                                        receiveNow += receiveName + ".state" + " = " + receiveName + "-" + state.toString();
                                        receiveNow += "\n & " + receiveProcess.getPsi().relabel(v -> v.sameTypeWithName(receiveName + "-" + v)).toString();
                                        receiveNow += "\n & (" + channel + " = " + broadcastChannel + " | " + channel + " = " + receiveProcess.getChannel().relabel(v -> v.sameTypeWithName(receiveName + "-" + v.toString().toLowerCase())).toString() + ")";
                                        receiveNows.add(receiveNow);

                                        Map<String, Expression> relabelledMessage = new HashMap<>();
                                        for(Map.Entry<String, Expression> entry : process.getMessage().entrySet()){
                                            relabelledMessage.put(entry.getKey(), entry.getValue().relabel(v -> v.sameTypeWithName(name + "-" + v.getName())));
                                        }

                                        for (Map.Entry<String, Expression> entry : receiveProcess.getUpdate().entrySet()) {
                                            receiveNext += "\n & (" + "next(" + receiveName + "-" + entry.getKey() + ") = " + entry.getValue().relabel(v -> system.getMessageStructure().containsKey(v.getName()) ? relabelledMessage.get(v.getName()) : v.sameTypeWithName(receiveName + "-" + v)) + ")";
                                        }
                                        for (String var : receiveAgent.getStore().getAttributes().keySet()) {
                                            if (!receiveProcess.getUpdate().containsKey(var)) {
                                                receiveNext += "\n & (" + "next(" + receiveName + "-" + var + ") = " + receiveName + "-" + var + ")";
                                            }
                                        }


                                        if (iterationExitTransitions != null && iterationExitTransitions.size() > 0) {
                                            Set<IterationExitTransition> destItExit = stateIterationExitTransitionMap.get(t.getDestination());
                                            List<String> exitConds = new ArrayList<>();
                                            next += "\n & case\n";
                                            for (IterationExitTransition tt : destItExit) {
                                                String exitCond = tt.getLabel().relabel(v -> v.sameTypeWithName(receiveName + "-" + v)).toString();
                                                exitConds.add(exitCond);
                                                receiveNext += "\t\t" + exitCond + " : next(" + receiveName + ".state" + ") = " + t.getDestination() + ");";
                                            }
                                            receiveNext += "\t\t TRUE : next(" + receiveName + ".state" + ") = " + receiveName + "-" + t.getDestination() + ");";
                                            receiveNext += "\n \tesac";
                                        } else {
                                            receiveNext += "\n & next(" + receiveName + ".state" + ") = " + receiveName + "-" + t.getDestination();
                                        }

                                        receiveNext = receiveNext.replaceAll("^\n *&", "");
                                        transitionReceivePreds.add("(" + receiveNow + ") & " + indent(indent(indent("\n" + receiveNext))));
                                    }

                                    stateReceivePrd += String.join("\n | ", transitionReceivePreds);
//                                    stateReceivePrd += "\n | ";
//                                    stateReceivePrd += ("(!((" + agentReceiveGuard.get(receiveName) + ") & (" + sendGuard + ") & (" + String.join(") | (", receiveNows) + ")) & keep-all-" + receiveName + ")");
                                } else{
                                    stateReceivePrd += "FALSE";
                                }

                                
                                String agentReceivePred = "(" + receiveGuard + ")\n -> \n" + indent("((" + sendGuard + "\n" + indent(indent("& (" + stateReceivePrd) + "))"));
                                agentReceivePred += indent("\n | (" + channel + " = " + broadcastChannel + " & " + "!(" + sendGuard + ") & keep-all-" + receiveName + ")");
                                agentReceivePred += "\n)";
                                agentReceivePreds.add(agentReceivePred);
                            }
                        }

                        transitionSendPreds.add(now + indent(indent(indent(next))) + "\n" + indent("& (" + String.join("\n & ", agentReceivePreds)+ ")") );
                    }
                    stateSendPreds.addAll(transitionSendPreds);
                }

                agentSendPreds.addAll(stateSendPreds);
                ;
            }
        }
//        define += "\t" + String.join(";\n\t", agentReceivePreds) + ";\n";
//        vars += "\tsendingChoice : 0.." + (choice - 1) + ";\n"

        trans += indent((("((" + String.join(")\n| (", agentSendPreds)))) + ")";
        trans += "\n\t\t| (!((" + String.join(") | (", sendNows) + ")) & keep-all))";

        nuxmv += vars;
        nuxmv +=  define;
        nuxmv += "CONSTANTS";
        nuxmv += "\n\t" + broadcastChannel;
        for(ChannelValue v : system.getChannels()){
            nuxmv += "," + v.getValue();
        }
        nuxmv += ";\n";
        nuxmv += init;
        nuxmv += trans;
        nuxmv = nuxmv.replaceAll("&( |\n)*TRUE(( )*&( )*)( |\n)*", "");
        nuxmv = nuxmv.replaceAll("TRUE(( )*&( )*)", "");
//        nuxmv = nuxmv.replaceAll("TRUE\n(\t& )", "\t");
        nuxmv = nuxmv.replaceAll("==", " = ");
        nuxmv = nuxmv.replaceAll("\\*", broadcastChannel);

        return nuxmv;
    }

}
