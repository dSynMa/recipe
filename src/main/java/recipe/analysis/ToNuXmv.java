package recipe.analysis;

import recipe.lang.Config;
import recipe.lang.System;
import recipe.lang.agents.*;
import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;
import recipe.lang.types.Enum;
import recipe.lang.types.Type;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ToNuXmv {

    public static void nuxmvModelChecking(System system) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter("translation.smv"));
        String script = transform(system);
        writer.write(script);
        writer.close();
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("nuxmv translation.smv");

        new Thread(() -> {
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = null;

            try {
                while ((line = input.readLine()) != null) {
                    java.lang.System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        pr.waitFor();
    }

    public static String type(TypedVariable typedVariable){
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

    //Semantics: If there is an agent is listening to sent-on non-broadcast channel that does not have a mathcing transition then the sending cannot occur.
    // - broadcast doesn t send on every channel
    // - A broadcast channel is just a channel that cannot be blocked
    // - keep-all-agent when no mathcing broadcast transition
    //      or not satisfying the send guard,
    //      or when receive-guard is false
    //      (ch=broadcast & (!send_guard | no_broadcast_transition)) | !listening
    public static String transform(System system) throws Exception {
        String nuxmv = "MODULE main\n";
        String vars = "VAR\n";
        String broadcastChannel = "broadcast";

        String define = "DEFINE\n";
        String init = "INIT\n";
        String trans = "";

        List<Agent> agents = new ArrayList<>(system.getAgents());
        List<String> agentSendPreds = new ArrayList<>();
        Set<String> sendProcessNames = new HashSet<>();
        Set<String> receiveProcessNames = new HashSet<>();
        List<String> keepFunctions = new ArrayList<>();

        String keepAll = "keep-all := TRUE";
//        String keepAllLabels = "keep-all-labels := TRUE";
//
//        for(int i = 0; i < agents.size(); i++) {
////            for(Transition t : agents.get(i).getSendTransitions()){
////                String label = ((SendProcess) t.getLabel()).getLabel();
////                if (label != null){
////                    keepAllLabels += " & next(" + agents.get(i).getName() + "-" + label + ") = " + agents.get(i).getName() + "-" + label;
////                    sendProcessNames.add(agents.get(i).getName() + "-" + label);
////                }
////            }
//            for(Transition t : agents.get(i).getReceiveTransitions()){
//                String label = ((ReceiveProcess) t.getLabel()).getLabel();
//                if (label != null && !label.equals("")){
//                    keepAllLabels += " & next(" + agents.get(i).getName() + "-" + label + ") = " + agents.get(i).getName() + "-" + label;
//                    receiveProcessNames.add(agents.get(i).getName() + "-" + label);
//                    String keep = "keep-local-not-" + agents.get(i).getName() + "-" + label + " := TRUE";
//                    for(Transition tt : agents.get(i).getReceiveTransitions()){
//                        String labell = ((ReceiveProcess) tt.getLabel()).getLabel();
//                        if(!label.equals(labell)){
//                            keep += " & next(" + agents.get(i).getName() + "-" + labell + ") = " + agents.get(i).getName() + "-" + labell;
//                        }
//                    }
//                    keepFunctions.add(keep);
//                }
//            }
//        }
//
//        keepFunctions.add(keepAllLabels);

        for(String sendName : sendProcessNames){
            String keep = "keep-not-" + sendName + " := TRUE";
            for(String sendName2 : sendProcessNames){
                if(!sendName.equals(sendName2)){
                    keep += " & next(" + sendName2 + ") = " + sendName2;
                }
            }

            keepFunctions.add(keep);
        }

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

            for(Transition t : agents.get(i).getReceiveTransitions()){
                String label = ((ReceiveProcess) t.getLabel()).getLabel();
                if (label != null && !label.equals("")){
                    keepThis += " & next(" + agents.get(i).getName() + "-" + label + ") = FALSE";
                    keepAll += " & next(" + agents.get(i).getName() + "-" + label + ") = FALSE";
                    receiveProcessNames.add(agents.get(i).getName() + "-" + label);

                    String falsifyAllLabelsExceptThis = "falsify-not-" + agents.get(i).getName() + "-" + label + " := TRUE";
                    for(Transition tt : agents.get(i).getReceiveTransitions()){
                        String labell = ((ReceiveProcess) tt.getLabel()).getLabel();
                        if(!label.equals(labell) && label != null && !label.equals("")){
                            falsifyAllLabelsExceptThis += " & next(" + agents.get(i).getName() + "-" + labell + ") = FALSE";
                        }
                    }
                    keepFunctions.add(falsifyAllLabelsExceptThis);
                }
            }

            keepAll += " & keep-all-" + name;

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
                vars += "\t" + name + "-" + typedVariable.getName() + " : " + type(typedVariable) + ";\n";
            }

            vars += "\t" + name + ".state" + " : {" + stateList + "};\n";
            init += "\t& " + name + ".state" + " = " + name + "-" + agent.getInitialState().toString() + "\n";

            init += "\t& " + agent.getInit().relabel(v -> ((TypedVariable) v).sameTypeWithName(name + "-" + v)) + "\n";

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
                            next += "\n & next(" + name + "-" + entry.getKey() + ") = (" + entry.getValue().relabel(v -> ((TypedVariable) v).sameTypeWithName(name + "-" + v)) + ")";
                        }
                        for (String var : agent.getStore().getAttributes().keySet()) {
                            if (!process.getUpdate().containsKey(var)) {
                                next += "\n & " + "next(" + name + "-" + var + ") = " + name + "-" + var;
                            }
                        }

                        List<String> agentReceivePreds = new ArrayList<>();

                        for (int j = 0; j < agents.size(); j++) {
                            if (i != j) {
                                Agent receiveAgent = agents.get(j);
                                String receiveName = receiveAgent.getName();

                                String receiveGuard = receiveAgent.getReceiveGuard().relabel(v -> v.sameTypeWithName(v.toString().equals(Config.channelLabel) ? v.toString() : receiveName + "-" + v)).toString().replaceAll(Config.channelLabel, channel);
                                receiveGuard = "(" + receiveGuard + ") | " + channel + " = " + broadcastChannel;

                                String sendGuard = "(" + process.getMessageGuard().relabel(v -> {
                                    try {
                                        return system.getCommunicationVariables().containsKey(v.getName()) ? receiveAgent.getRelabel().get(v).relabel(vv -> ((TypedVariable) vv).sameTypeWithName(receiveName + "-" + vv)) : v.sameTypeWithName(name + "-" + v);
                                    } catch (RelabellingTypeException | MismatchingTypeException e) {
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
                                            relabelledMessage.put(entry.getKey(), entry.getValue().relabel(v -> ((TypedVariable) v).sameTypeWithName(name + "-" + ((TypedVariable) v).getName())));
                                        }

                                        for (Map.Entry<String, Expression> entry : receiveProcess.getUpdate().entrySet()) {
                                            receiveNext += "\n & " + "next(" + receiveName + "-" + entry.getKey() + ") = " + entry.getValue().relabel(v -> system.getMessageStructure().containsKey(((TypedVariable) v).getName()) ? relabelledMessage.get(((TypedVariable) v).getName()) : ((TypedVariable) v).sameTypeWithName(receiveName + "-" + v));
                                        }
                                        for (String var : receiveAgent.getStore().getAttributes().keySet()) {
                                            if (!receiveProcess.getUpdate().containsKey(var)) {
                                                receiveNext += "\n & " + "next(" + receiveName + "-" + var + ") = " + receiveName + "-" + var;
                                            }
                                        }

                                        if(receiveProcess.getLabel() != null && !receiveProcess.equals("")){
                                            receiveNext += "\n & "  + "next(" + receiveName + "-" + receiveProcess.getLabel() + ") = TRUE";
                                            receiveNext += "\n & falsify-not-" + receiveName + "-" + receiveProcess.getLabel() + "";
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

                                String agentReceivePred = "(" + receiveGuard + ")\n & \n" + indent("((" + sendGuard + "\n" + indent(indent("& (" + stateReceivePrd) + "))"));
                                agentReceivePred += indent("\n | (!(" + receiveGuard + ") & keep-all-" + receiveName + ")");
                                agentReceivePred += indent("\n | (" + channel + " = " + broadcastChannel + " & " + "!(" + sendGuard + ") & keep-all-" + receiveName + ")");
                                agentReceivePred += "\n)";
                                agentReceivePreds.add(agentReceivePred);
                            }
                        }

                        String logic = now + indent(indent(indent(next))) + "\n" + indent("& (" + String.join("\n & ", agentReceivePreds)+ ")");

                        if(process.getLabel() != null) {
                            define +=  "\t" + name + "-" + process.getLabel() + " := " + logic + ";\n";
                            transitionSendPreds.add(name + "-" + process.getLabel());
                        } else {
                            transitionSendPreds.add(logic);
                        }
                    }
                    stateSendPreds.addAll(transitionSendPreds);
                }

                agentSendPreds.addAll(stateSendPreds);
            }
        }

        trans += "((" + String.join(")\n| (", agentSendPreds) + "));\n";
//        trans += "\n\t\t| (!((" + String.join(") | (", sendNows) + ")) & keep-all))";

        for(String name : receiveProcessNames){
            vars += "\t" + name + " : " + "boolean;\n";
        }
        nuxmv += vars;
        define += "\ttransition := " + trans;
        nuxmv += define;
        nuxmv += "CONSTANTS\n\t";
        nuxmv +=  String.join(", ", Enum.getEnum(Config.channelLabel).getValues());
        nuxmv += ";\n";

//        for(String name : sendProcessNames){
//            init += "\t& " + name + " = " + "FALSE\n";
//        }

        for(String name : receiveProcessNames){
            init += "\t& " + name + " = " + "FALSE\n";
        }

        nuxmv += init;
        nuxmv += "TRANS\n";
        nuxmv += "\ttransition | (!transition & keep-all)\n";
        nuxmv = nuxmv.replaceAll("&( |\n)*TRUE(( )*&( )*)( |\n)*", "");
        nuxmv = nuxmv.replaceAll("TRUE(( )*&( )*)", "");
//        nuxmv = nuxmv.replaceAll("TRUE\n(\t& )", "\t");
        nuxmv = nuxmv.replaceAll("==", " = ");
        nuxmv = nuxmv.replaceAll("\\*", broadcastChannel);

        nuxmv += String.join("\n", system.getLtlspec());

        return nuxmv;
    }

}
