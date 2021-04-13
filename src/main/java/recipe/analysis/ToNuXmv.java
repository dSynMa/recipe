package recipe.analysis;

import recipe.lang.System;
import recipe.lang.agents.*;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
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
        init += "TRUE\n";
        String trans = "TRANS\n";

        List<Agent> agents = new ArrayList<>(system.getAgents());
        List<String> agentSendPreds = new ArrayList<>();
        List<String> agentReceivePreds = new ArrayList<>();
        for(int i = 0; i < agents.size(); i++){
            Agent agent = agents.get(i);
            String name = agent.getName();
            Stream<String> allStates = agent.getStates().stream().map(s -> name + "-" + s.toString());
            String stateList = String.join("," , allStates.toArray(String[]::new));

            for(TypedVariable typedVariable : agent.getStore().getAttributes().values()){
                vars += "\t" + name + "-" + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
            }
            for(TypedVariable typedVariable : system.getCommunicationVariables().values()){
                vars += "\t" + name + "-" + typedVariable.getName() + " : " + type(typedVariable, system) + ";\n";
            }
//            vars += "\t" + name + ".channel : {" + String.join(", ", system.getChannels().stream().map(a -> a.getValue()).toArray(String[]::new)) + "};\n";
            vars += "\t" + name + ".state" + " : {" + stateList + "};\n";
            init += "\t& " + name + ".state" + " = " + name + "-" + agent.getInitialState().toString() + "\n";
            for(Map.Entry<String, TypedValue> entry : agent.getStore().getData().entrySet()){
                init += "\t& " + name + "-" + entry.getKey() + " = " + entry.getValue().getValue() + "\n";
            }
            define += "\treceive-guard-" + name + " := " + agent.getReceiveGuard().relabel(v -> system.getCommunicationVariables().containsKey(v.getName()) ? v : v.getName().equals("channel") ? v : v.sameTypeWithName(name + "-" + v)) + ";\n";

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

                String stateReceivePrd = "((" + name + ".state" + " = " + name + "-" + state.toString() +  ") -> ";
                if (receiveTransitions != null && receiveTransitions.size() > 0) {
                    List<String> transitionReceivePreds = new ArrayList<>();
                    for (Transition t : receiveTransitions) {
                        ReceiveProcess process = (ReceiveProcess) t.getLabel();
                        String transPred = "(";
                        transPred += "(";
                        transPred += process.getPsi().relabel(v -> v.sameTypeWithName(name + "-" + v)).toString();
                        transPred += " & ";
                        transPred += "(channel = * | channel = " + process.getChannel().relabel(v -> v.sameTypeWithName(name + "-" + v.toString().toLowerCase())).toString() +")";
                        transPred += ")";
                        transPred += " -> (";
                        transPred += "(TRUE";
                        for (Map.Entry<TypedVariable, Expression> entry : agent.getRelabel().entrySet()) {
                            transPred += " & (" + name + "-" + entry.getKey().getName() + " = (" + entry.getValue().relabel(v -> system.getMessageStructure().containsKey(v.getName()) ? v : v.sameTypeWithName(name + "-" + v)) + "))";
                        }
                        transPred += ")";
                        transPred += " & (TRUE";
                        for (Map.Entry<String, Expression> entry : process.getUpdate().entrySet()) {
                            transPred += " & (" + "next(" + name + "-" + entry.getKey() + ") = " + entry.getValue().relabel(v -> system.getMessageStructure().containsKey(v.getName()) ? v : v.sameTypeWithName(name + "-" + v)) + ")";
                        }
                        for (String var : agent.getStore().getAttributes().keySet()) {
                            if (!process.getUpdate().containsKey(var)) {
                                transPred += " & (" + "next(" + name + "-" + var + ") = " + name + "-" + var + ")";
                            }
                        }

                        transPred += " & ";
                        if(stateIterationExitTransitionMap.containsKey(t.getDestination())
                                && stateIterationExitTransitionMap.get(t.getDestination()) != null
                                && stateIterationExitTransitionMap.get(t.getDestination()).size() > 0){
                            Set<IterationExitTransition> destItExit = stateIterationExitTransitionMap.get(t.getDestination());
                            List<String> exitConds = new ArrayList<>();
                            transPred += "(TRUE";
                            for(IterationExitTransition tt : destItExit){
                                String exitCond = tt.getLabel().relabel(v -> v.sameTypeWithName(name + "-" + v)).toString();
                                exitConds.add(exitCond);
                                transPred += " & (" + exitCond + " -> next(" + name + ".state" + ") = " + name + "-" + t.getDestination() + ")";
                            }
                            transPred += " & (!(" + String.join(" | ", exitConds) + ") -> next(" + name + ".state" + ") = " + name + "-" + t.getDestination() + ")";
                            trans += ")";
                        } else{
                            transPred += "next(" + name + ".state" + ") = " + name + "-" + t.getDestination();
                        }

                        transPred += ")";
                        transPred += ")";
                        transPred += ")";
                        transitionReceivePreds.add(transPred);
                    }
                    stateReceivePrd +=  "(" + String.join(" xor ", transitionReceivePreds) + ")";
                    stateReceivePrd += ")\n";
                    stateReceivePreds.add(stateReceivePrd.trim());
                } else{
//                    stateReceivePrd += "next(" + name + ".state" + ") = " + name + ".state" + "";
                }

                if (sendTransitions != null && sendTransitions.size() > 0) {
                    List<String> transitionSendPreds = new ArrayList<>();

                    String stateSendPred = "";
                    for (ProcessTransition t : sendTransitions) {
                        String transPred = "";
                        SendProcess process = (SendProcess) t.getLabel();
                        transPred += "(";
                        transPred += "" + name + ".state" + " = " + name + "-" + t.getSource();
                        transPred += " & (" + process.entryCondition().relabel(v -> v.sameTypeWithName(name + "-" + v)) + ")";
                        transPred += ")";
                        transPred += " -> (";
                        transPred += " channel = " + process.getChannel().relabel(v -> v.sameTypeWithName(agent.getName() + "-" + v));
                        transPred += " & ";
                        transPred += " sendingAgent = " + name;
                        transPred += " & TRUE";

                        for (Map.Entry<String, Expression> entry : process.getMessage().entrySet()) {
                            transPred += " & (" + entry.getKey() + " = " + entry.getValue().relabel(v -> v.sameTypeWithName(name + "-" + v)) + ")";
                        }

                        transPred += " & ";

                        transPred += process.getMessageGuard().relabel(v -> system.getCommunicationVariables().containsKey(v.getName()) ? v : v.sameTypeWithName(name + "-" + v));

                        transPred += " & ";

                        if(stateIterationExitTransitionMap.containsKey(t.getDestination())
                            && stateIterationExitTransitionMap.get(t.getDestination()) != null
                            && stateIterationExitTransitionMap.get(t.getDestination()).size() > 0){
                            Set<IterationExitTransition> destItExit = stateIterationExitTransitionMap.get(t.getDestination());
                            List<String> exitConds = new ArrayList<>();
                            transPred += "(TRUE";
                            for(IterationExitTransition tt : destItExit){
                                String exitCond = tt.getLabel().relabel(v -> v.sameTypeWithName(name + "-" + v)).toString();
                                exitConds.add(exitCond);
                                transPred += " & (" + exitCond + " -> next(" + name + ".state" + ") = " + t.getDestination() + ")";
                            }
                            transPred += " & (!(" + String.join(" | ", exitConds) + ") -> next(" + name + ".state" + ") = " + name + "-" + t.getDestination() + ")";
                            trans += ")";
                        } else{
                            transPred += "next(" + name + ".state" + ") = " + name + "-" + t.getDestination();
                        }

                        transPred += " & (TRUE";
                        for (Map.Entry<String, Expression> entry : process.getUpdate().entrySet()) {
                            transPred += " & (" + "next(" + name + "-" + entry.getKey() + ") = " + entry.getValue().relabel(v -> v.sameTypeWithName(name + "-" + v)) + ")";
                        }
                        for (String var : agent.getStore().getAttributes().keySet()) {
                            if (!process.getUpdate().containsKey(var)) {
                                transPred += " & (" + "next(" + name + "-" + var + ") = " + name + "-" + var + ")";
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

            if(stateReceivePreds.size() > 0)
                agentReceivePreds.add("receive-trans-" + name + " := (" + String.join(")\n\t\t\t\t\t\t & (", stateReceivePreds) + ")");
            else
                agentReceivePreds.add("receive-trans-" + name + " := TRUE");

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
                left += " & " + s + " = " + a.getName() + "-" + s;
            }
            left += ")";
            String right = "receive-trans-" + a.getName();
            String keep = "next(" + a.getName() + ".state) = " + a.getName() + ".state";
            keep += "& TRUE";
            for(TypedVariable variable : a.getStore().getAttributes().values()){
                keep += " & next(" + a.getName() + "-" + variable.getName() + ") = " + a.getName() + "-" + variable.getName();
            }
            return "(" + (left + " -> " + right) + ") | (" + keep + ")))";
        }).toArray(String[]::new)) + "\n";

        nuxmv += vars;
        nuxmv += define;
        nuxmv += init;
        nuxmv += trans;
        nuxmv = nuxmv.replaceAll("TRUE(( )*&( )*)", "");
        nuxmv = nuxmv.replaceAll("TRUE\n(\t& )", "\t");
        nuxmv = nuxmv.replaceAll("==", " = ");
        nuxmv = nuxmv.replaceAll("\\*", anyChannel);

        return nuxmv;
    }
}
