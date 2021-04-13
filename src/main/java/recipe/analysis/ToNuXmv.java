package recipe.analysis;

import recipe.lang.System;
import recipe.lang.agents.*;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelValue;
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
            return "REAL";
        } else if(typedVariable.getClass().equals(ChannelVariable.class)){
            return "{" + String.join(", ", system.getChannels().stream().map(a -> a.getValue()).toArray(String[]::new)) + "}";
        } else if(typedVariable.getClass().equals(BooleanVariable.class)) {
            return "BOOL";
        } else if(typedVariable.getClass().equals(StringVariable.class)) {
            return "STRING";
        } else {
            return "UNKNOWN";
        }
    }

    public static String transform(System system) throws RelabellingTypeException {
        String nuxmv = "MODULE main\n";
        String vars = "VAR\n";
        vars += "\tsendingAgent : {" + String.join(", ", system.getAgents().stream().map(a -> a.getName().trim()).toArray(String[]::new)) + "};\n";
        vars += "\tchannel : {" + String.join(", ", system.getChannels().stream().map(a -> a.getValue()).toArray(String[]::new)) + "};\n";
        for(TypedVariable typedVariable : system.getCommunicationVariables().values()){
            vars += "\t" + typedVariable.getName() + " : " + type(typedVariable, system) + ";";
        }
        String define = "DEFINE\n";
        String init = "INIT\n";
        String trans = "TRANS\n";
        String transReceive = "TRUE";
        String transSend = "TRUE";
        List<Agent> agents = new ArrayList<>(system.getAgents());
        List<String> agentSendPreds = new ArrayList<>();
        List<String> agentReceivePreds = new ArrayList<>();
        for(int i = 0; i < agents.size(); i++){
            Agent agent = agents.get(i);
            Stream<String> allStates = agent.getStates().stream().map(s -> s.toString());
            String stateList = String.join("," , allStates.toArray(String[]::new));
            String name = agent.getName();

            vars += "\tstate" + name + " : {" + stateList + "};\n";
            init += "\tstate" + name + " = " + agent.getInitialState().toString() + ";\n";
            define += "\treceive-guard-" + name + " = " + agent.getReceiveGuard().relabel(v -> system.getCommunicationVariables().containsKey(v.getName()) ? v : v.sameTypeWithName(name + "." + v)) + ";\n";

            Map<State, Set<ProcessTransition>> stateSendTransitionMap = new HashMap<>();
            Map<State, Set<ProcessTransition>> stateReceiveTransitionMap = new HashMap<>();
            Map<State, Set<IterationExitTransition>> stateIterationExitTransitionMap = new HashMap<>();
            stateSendTransitionMap.putAll(agent.getStateTransitionMap(agent.getSendTransitions()));
            stateReceiveTransitionMap.putAll(agent.getStateTransitionMap(agent.getReceiveTransitions()));
            stateIterationExitTransitionMap.putAll(agent.getStateTransitionMap(agent.getIterationExitTransitions()));

            //TODO join with &
            //TODO " & sendingAgent != " + name +  and "receive-guard-" + name + when joining
            List<String> stateReceivePreds = new ArrayList<>();

            List<String> stateSendPreds = new ArrayList<>();
            for(State state : agent.getStates()) {
                Set<ProcessTransition> sendTransitions = stateSendTransitionMap.get(state);
                Set<ProcessTransition> receiveTransitions = stateReceiveTransitionMap.get(state);
                Set<IterationExitTransition> iterationExitTransitions = stateIterationExitTransitionMap.get(state);

                String stateReceivePrd = "(state" + name + " = " + state.toString() +  ") -> ";
                if (receiveTransitions != null && receiveTransitions.size() > 0) {
                    List<String> transitionReceivePreds = new ArrayList<>();
                    //TODO must get outgoing transition from each state, XOR them, and AND the NEG of all outgoing transition guards + receive-guard to imply remaining in same state.
                    for (Transition t : receiveTransitions) {
                        ReceiveProcess process = (ReceiveProcess) t.getLabel();
                        String transitionPred = "(";
                        transitionPred += "(";
                        transitionPred += process.getPsi().relabel(v -> v.sameTypeWithName(name + "." + v)).toString();
                        transitionPred += ")";
                        transitionPred += " -> (";
                        transitionPred += "(TRUE";
                        for (Map.Entry<TypedVariable, Expression> entry : agent.getRelabel().entrySet()) {
                            transitionPred += " & (" + name + "." + entry.getKey().getName() + " = " + entry.getValue().relabel(v -> v.sameTypeWithName(name + "." + v)) + ")";
                        }
                        transitionPred += ")";
                        transitionPred += " & (TRUE";
                        for (Map.Entry<String, Expression> entry : process.getUpdate().entrySet()) {
                            transitionPred += " & (" + "next(" + name + "." + entry.getKey() + ") = " + entry.getValue().relabel(v -> v.sameTypeWithName(name + "." + v)) + ")";
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
                    stateReceivePrd += "next(state" + name + ") = state" + name + " \n";
                }

                stateReceivePreds.add(stateReceivePrd.trim());
                agentReceivePreds.add("\treceive-trans-" + name + " := " + String.join(" & ", stateReceivePreds));

                if (sendTransitions != null && sendTransitions.size() > 0) {
                    List<String> transitionSendPreds = new ArrayList<>();

                    //TODO must get outgoing transition from each state, XOR them, and AND the NEG of all outgoing transition guards to imply remaining in same state.

                    String stateSendPred = "";
                    for (ProcessTransition t : sendTransitions) {
                        String transPred = "";
                        SendProcess process = (SendProcess) t.getLabel();
                        transPred += "(";
                        transPred += "state" + name + " = " + t.getSource();
                        transPred += " & (" + process.entryCondition().relabel(v -> v.sameTypeWithName(name + "." + v)) + ")";
                        transPred += " -> (";
                        transPred += " channel = " + process.getChannel().relabel(v -> v.sameTypeWithName(agent.getName() + "." + v));
                        transPred += " & ";
                        transPred += " sendingAgent = " + name;
                        transPred += " & ";

                        //TODO this needs to be dealt when setting the next state here
                        for (Transition tt : agent.getIterationExitTransitions()) {

                        }

                        transPred += "next(state" + name + ") = " + t.getDestination();
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
                        transPred += ")";

                        transitionSendPreds.add(transPred);
                    }
                    stateSendPred = String.join("\n xor ", transitionSendPreds);
                    stateSendPreds.add(stateSendPred);
                }
            }

            agentSendPreds.add(String.join(" & ", stateSendPreds));
        }

        define += String.join(";\n\n\t", agentReceivePreds) + "\n";

        //TODO need to encode states having the same value in the next step if no transition send activates
//        trans += "(" + transSend + "\n\txor channel = {})\n";
        trans += String.join(" | ", agentSendPreds);

        nuxmv += vars;
        nuxmv += define;
        nuxmv += init;
        nuxmv += trans;

        return nuxmv;
    }
}
