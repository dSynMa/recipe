package recipe.analysis;

import recipe.lang.System;
import recipe.lang.agents.*;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.expressions.strings.StringVariable;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;

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

    public static String transform(System system) throws RelabellingTypeException {
        String nuxmv = "MODULE main\n";
        String vars = "VAR\n";
        vars += "\tsendingAgent : {" + String.join(", ", system.getAgents().stream().map(a -> a.getName().trim().toLowerCase()).toArray(String[]::new)) + "};\n";
        Set<String> channelVals = new HashSet(Arrays.asList(system.getChannels().stream().map(a -> a.getValue().toLowerCase()).toArray(String[]::new)));
        String anyChannel = String.join("-", channelVals);
        channelVals.add(anyChannel);
        vars += "\tchannel : {" + String.join(", ",channelVals)  + "};\n";
        for(TypedVariable typedVariable : system.getMessageStructure().values()){
            vars += "\t" + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
        }

        for(TypedVariable typedVariable : system.getCommunicationVariables().values()){
            vars += "\t" + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
        }
        String define = "DEFINE\n";
        String init = "INIT\n";
        String trans = "TRANS\n";

        List<Agent> agents = new ArrayList<>(system.getAgents());
        List<String> agentSendPreds = new ArrayList<>();
        List<String> agentReceivePreds = new ArrayList<>();
        for(int i = 0; i < agents.size(); i++){
            Agent agent = agents.get(i);
            Stream<String> allStates = agent.getStates().stream().map(s -> s.toString());
            String stateList = String.join("," , allStates.toArray(String[]::new));
            String name = agent.getName();

            for(TypedVariable typedVariable : agent.getStore().getAttributes().values()){
                vars += "\t" + name + "." + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
            }
            for(TypedVariable typedVariable : system.getCommunicationVariables().values()){
                vars += "\t" + name + "." + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
            }
//            vars += "\t" + name + ".channel : {" + String.join(", ", system.getChannels().stream().map(a -> a.getValue()).toArray(String[]::new)) + "};\n";
            vars += "\t" + name + ".state" + " : {" + stateList + "};\n";
            init += "\t" + name + ".state" + " = " + agent.getInitialState().toString() + ";\n";
            define += "\treceive-guard-" + name + " = " + agent.getReceiveGuard().relabel(v -> system.getCommunicationVariables().containsKey(v.getName()) ? v : v.getName().equals("channel") ? v : v.sameTypeWithName(name + "." + v)) + ";\n";

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

                String stateReceivePrd = "(" + name + ".state" + " = " + state.toString() +  ") -> ";
                if (receiveTransitions != null && receiveTransitions.size() > 0) {
                    List<String> transitionReceivePreds = new ArrayList<>();
                    for (Transition t : receiveTransitions) {
                        ReceiveProcess process = (ReceiveProcess) t.getLabel();
                        String transitionPred = "(";
                        transitionPred += "(";
                        transitionPred += process.getPsi().relabel(v -> v.sameTypeWithName(name + "." + v)).toString();
                        transitionPred += " & ";
                        transitionPred += "(channel = * | channel = " + process.getChannel().relabel(v -> v.sameTypeWithName(name + "." + v.toString().toLowerCase())).toString() +")";
                        transitionPred += ")";
                        transitionPred += " -> (";
                        transitionPred += "(TRUE";
                        for (Map.Entry<TypedVariable, Expression> entry : agent.getRelabel().entrySet()) {
                            transitionPred += " & (" + name + "." + entry.getKey().getName() + " = " + entry.getValue().relabel(v -> system.getMessageStructure().containsKey(v.getName()) ? v : v.sameTypeWithName(name + "." + v)) + ")";
                        }
                        transitionPred += ")";
                        transitionPred += " & (TRUE";
                        for (Map.Entry<String, Expression> entry : process.getUpdate().entrySet()) {
                            transitionPred += " & (" + "next(" + name + "." + entry.getKey() + ") = " + entry.getValue().relabel(v -> system.getMessageStructure().containsKey(v.getName()) ? v : v.sameTypeWithName(name + "." + v)) + ")";
                        }
                        for (String var : agent.getStore().getAttributes().keySet()) {
                            if (!process.getUpdate().containsKey(var)) {
                                transitionPred += " & (" + "next(" + name + "." + var + ") = " + name + "." + var + ")";
                            }
                        }
                        transitionPred += ")";
                        transitionPred += ")";
                        transitionReceivePreds.add(transitionPred);
                    }
                    stateReceivePrd +=  "(" + String.join(" xor ", transitionReceivePreds) + ")\n";
                } else{
                    stateReceivePrd += "next(" + name + ".state" + ") = " + name + ".state" + " \n";
                }

                stateReceivePreds.add(stateReceivePrd.trim());

                if (sendTransitions != null && sendTransitions.size() > 0) {
                    List<String> transitionSendPreds = new ArrayList<>();

                    String stateSendPred = "";
                    for (ProcessTransition t : sendTransitions) {
                        String transPred = "";
                        SendProcess process = (SendProcess) t.getLabel();
                        transPred += "(";
                        transPred += "" + name + ".state" + " = " + t.getSource();
                        transPred += " & (" + process.entryCondition().relabel(v -> v.sameTypeWithName(name + "." + v)) + ")";
                        transPred += ")";
                        transPred += " -> (";
                        transPred += " channel = " + process.getChannel().relabel(v -> v.sameTypeWithName(agent.getName() + "." + v));
                        transPred += " & ";
                        transPred += " sendingAgent = " + name;
                        transPred += " & ";

                        for (Map.Entry<String, Expression> entry : process.getMessage().entrySet()) {
                            transPred += " & (" + entry.getKey() + " = " + entry.getValue().relabel(v -> v.sameTypeWithName(name + "." + v)) + ")";
                        }

                        transPred += " & ";

                        trans += process.getMessageGuard().relabel(v -> system.getCommunicationVariables().containsKey(v.getName()) ? v : v.sameTypeWithName(name + "." + v));

                        transPred += " & ";

                        //TODO this needs to be dealt when setting the next state here also in receiving transition
                        for (Transition tt : agent.getIterationExitTransitions()) {

                        }

                        transPred += "next(" + name + ".state" + ") = " + t.getDestination();
                        transPred += " & (TRUE";
                        for (Map.Entry<String, Expression> entry : process.getUpdate().entrySet()) {
                            transPred += " & (" + "next(" + name + "." + entry.getKey() + ") = " + entry.getValue().relabel(v -> v.sameTypeWithName(name + "." + v)) + ")";
                        }
                        for (String var : agent.getStore().getAttributes().keySet()) {
                            if (!process.getUpdate().containsKey(var)) {
                                transPred += " & (" + "next(" + name + "." + var + ") = " + name + "." + var + ")";
                            }
                        }
                        transPred += ")";
                        transPred += ")";

                        transitionSendPreds.add(transPred);
                    }
                    stateSendPred = String.join("\n xor ", transitionSendPreds);
                    stateSendPreds.add(stateSendPred);
                }
            }

            agentReceivePreds.add("receive-trans-" + name + " := (" + String.join(") & (", stateReceivePreds) + ")");
            if(stateSendPreds.size() > 0)
                agentSendPreds.add(String.join(" & ", stateSendPreds));
        }

        define += "\t" + String.join(";\n\t", agentReceivePreds) + ";\n";

        trans += "\t (" + String.join(" | ", agentSendPreds) + ")";
        trans += "\n\t & ";
        trans += String.join("\n\t & ", system.getAgents().stream().map(a -> {
            String left = "sendingAgent != " + a.getName();
            left += " -> ";
            left += "(((" + "channel = * | receive-guard-" + a.getName() + ")";
            left += " & (TRUE";
            for(String s : system.getCommunicationVariables().keySet()){
                left += " & " + s + " = " + a.getName() + "." + s;
            }
            left += ")";
            String right = "receive-trans-" + a.getName();
            return "(" + (left + " -> " + right) + ") | next(" + a.getName() + ".state) = " + a.getName() + ".state))";
        }).toArray(String[]::new)) + "\n";

        nuxmv += vars;
        nuxmv += define;
        nuxmv += init;
        nuxmv += trans;
        nuxmv = nuxmv.replaceAll("TRUE & ", "");
        nuxmv = nuxmv.replaceAll("==", " = ");
        nuxmv = nuxmv.replaceAll("\\*", anyChannel);

        return nuxmv;
    }
}
