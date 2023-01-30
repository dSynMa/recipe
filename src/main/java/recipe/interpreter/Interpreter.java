package recipe.interpreter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import recipe.Config;
import recipe.analysis.NuXmvInteraction;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.agents.State;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;
import recipe.lang.store.ConcreteStore;
import recipe.lang.store.Store;
import recipe.lang.types.Type;
import recipe.lang.utils.Pair;
import recipe.lang.utils.exceptions.MismatchingTypeException;

public class Interpreter {
    private static TypedVariable channTypedVariable;
    private TypedVariable getChannelTV () {
        if (channTypedVariable == null) {
            try {
                Type chanEnum = recipe.lang.types.Enum.getEnum("channel");
                channTypedVariable = new TypedVariable<Type>(chanEnum, "channel");
            } catch (Exception e) {
                System.err.println(e);
                System.err.println(e.getStackTrace());
            }
        }
        return channTypedVariable;
    }

    private class Transition {
        private AgentInstance sender;
        private ProcessTransition send;
        private Map<AgentInstance, ProcessTransition> receivers;
        
        public Transition() {
            receivers = new HashMap<AgentInstance, ProcessTransition>();
        }

        public JSONObject toJSON() {
            JSONObject result = new JSONObject();
            List<String> receiverNames = new ArrayList<>(receivers.size());
            for (AgentInstance receiver : receivers.keySet()) {
                receiverNames.add(receiver.getLabel());
            }

            result.put("sender", sender.getLabel());
            result.put("send", send.getLabel().toString());
            result.put("receivers", receiverNames);
            return result;
        }

        public void prettyPrint() { prettyPrint(System.out); }

        public void prettyPrint(PrintStream stream) {
            stream.println("--- transition ---");
            stream.print("Sender:\n");
            stream.printf("\n%s (%s)\t\t%s\n", sender.getLabel(), sender.getAgent().getName(), send);
            stream.print("Receivers:\n");
            for (AgentInstance receiver : receivers.keySet()) {
                stream.printf("%s (%s)\t\t%s\n", receiver.getLabel(), receiver.getAgent().getName(), receivers.get(receiver));
            }
            stream.println("------------------");
        }


        public SendProcess getSendProcess() {
            return (SendProcess) send.getLabel();
        }

        public void setSender(AgentInstance instance) {
            sender = instance;
        }
        public void setSend(ProcessTransition transition) {
            send = transition;
        }
        public void pushReceiver(AgentInstance instance, ProcessTransition receive) {
            receivers.put(instance, receive);
        }
    }

    public class Step {
        private Map<AgentInstance,ConcreteStore> stores;
        private Map<TypedValue, Set<AgentInstance>> listeners;
        private List<Transition> transitions;
        private Transition chosenTransition;
        private Step parent;

        protected void handleEvaluationException(Exception e) {
            // TODO
            System.err.println(e);
            System.err.println(e.getStackTrace());
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
            result.put("state", jStores);
            result.put("transitions", jTransitions);
            return result;
        }

        public Step next(int index, Interpreter interpreter) {
            assert index < transitions.size();
            this.chosenTransition = transitions.get(index);
            // System.out.printf("Chosen: %d, available: %d\n", index, transitions.size());
            chosenTransition.prettyPrint(System.out);

            Map<AgentInstance,ConcreteStore> nextStores = new HashMap<AgentInstance,ConcreteStore>(stores);
            
            try {
                // Update sender
                ConcreteStore nextSenderStore = stores.get(chosenTransition.sender).BuildNext(chosenTransition.send);
                nextStores.put(chosenTransition.sender, nextSenderStore);

                Pair<Store, TypedValue> msgPair = MakeMessageStore(stores.get(chosenTransition.sender), chosenTransition.getSendProcess(), interpreter);
                for (AgentInstance receiver : chosenTransition.receivers.keySet()) {
                    ProcessTransition receive = chosenTransition.receivers.get(receiver);
                    ConcreteStore nextReceiverStore = stores.get(receiver).BuildNext(receive, msgPair.getLeft());
                    nextStores.put(receiver, nextReceiverStore);
                }
            } catch (Exception e) {
                handleEvaluationException(e);
            }
            Step next = new Step(nextStores, this, interpreter);
            return next;
        }

        protected Pair<Store, TypedValue> MakeMessageStore(Store senderStore, SendProcess sendProcess, Interpreter interpreter) {
            // Add message and channel to a new map
            Map<TypedVariable, TypedValue> msgMap = new HashMap<TypedVariable, TypedValue>();
            Expression chanExpr = sendProcess.getChannel();
            try {
                TypedValue chan = chanExpr.valueIn(senderStore);
                sendProcess.getMessage().forEach((msgVar, msgExpr) -> {
                    try {
                        TypedValue msgVal = msgExpr.valueIn(senderStore);
                        Type msgType = interpreter.sys.getMessageStructure().get(msgVar);
                        if (msgType != msgVal.getType()) {
                            throw new MismatchingTypeException(
                                String.format("Mismatch type for message variable %s (expected %s, got %s)", 
                                msgVar,
                                msgType.toString(), 
                                msgVal.getType().toString()));
                        }
                        TypedVariable tv = new TypedVariable(msgType, msgVar);
                        msgMap.put(tv, msgVal);
                    } catch (Exception e) {
                        handleEvaluationException(e);
                    }
                });
                msgMap.put(getChannelTV(), chan);
                return new Pair<Store, TypedValue>(new ConcreteStore(msgMap), chan);
            } catch (Exception e) {
                handleEvaluationException(e);
            }
            return null;
        } 

        public Step(Map<AgentInstance,ConcreteStore> stores, Step parent, Interpreter interpreter) {
            this.parent = parent;
            this.stores = stores;
            this.transitions = new LinkedList<>();
            this.listeners = new HashMap<>();

            // Evaluate which channels each agent is currently listening to
            try {
                Type chanEnum = recipe.lang.types.Enum.getEnum("channel");
                for (AgentInstance instance : interpreter.sys.getAgentInstances()) {
                    ConcreteStore store = stores.get(instance);
                    for (Object objChan : chanEnum.getAllValues()) {
                        TypedValue chan = (TypedValue) objChan;
                        Store s = store.push(getChannelTV(), chan);
                        boolean isListening = Condition.getTrue().equals(instance.getAgent().getReceiveGuard().valueIn(s));
                        if (isListening) {
                            listeners.putIfAbsent(chan, new HashSet<>());
                            listeners.get(chan).add(instance);
                        }
                    }
                }
                System.err.println(listeners);
            } catch (Exception e) {
                handleEvaluationException(e);
            }

            // Compute available transitions
            interpreter.sys.getAgentInstances().forEach(sender -> {
                ConcreteStore senderStore = stores.get(sender);
                Set<ProcessTransition> instSends = interpreter.sends.get(senderStore.getState());

                if (instSends != null) {
                    instSends.forEach(tr -> {
                        SendProcess sendProcess = (SendProcess) tr.getLabel();
                        Expression chanExpr = sendProcess.getChannel();
                        Expression<recipe.lang.types.Boolean> psi = sendProcess.getPsi();
                        try {
                            boolean psiSat = psi.valueIn(stores.get(sender)).equals(Condition.getTrue());

                            if (psiSat) {
                                // Add message and channel to a new map
                                Pair<Store, TypedValue> msgPair = MakeMessageStore(senderStore, sendProcess, interpreter);
                                Store msgStore = msgPair.getLeft();
                                TypedValue chan = msgPair.getRight();

                                // Set up map of receivers
                                // A receiver must
                                // a) not be the sender
                                // b) be listening to channel "chan"
                                // c) satisfy send guard
                                Map<AgentInstance, Set<ProcessTransition>> receivesMap = new HashMap<AgentInstance, Set<ProcessTransition>>();
                                for (AgentInstance inst : listeners.get(chan)) {
                                    if (inst != sender) {
                                        try {
                                            Store store = stores.get(inst).push(msgStore);
                                            boolean sendGuardOk = Condition.getTrue().equals(sendProcess.getMessageGuard().valueIn(store));
                                            // System.out.printf("sendGuard: %s\n", sendGuardOk);
                                            boolean receiveGuardOk = Condition.getTrue().equals(inst.getAgent().getReceiveGuard().valueIn(store));
                                            // System.out.printf("receiveGuard: %s\n", receiveGuardOk);
                                            if (sendGuardOk && receiveGuardOk) {
                                                receivesMap.put(inst, new HashSet<ProcessTransition>());
                                                // System.out.printf("%s can receive\n", inst);
                                            }
                                        } catch (Exception e) {
                                            handleEvaluationException(e);
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
                                                    Expression<recipe.lang.types.Boolean> recPsi = recLbl.getPsi();
                                                    boolean recPsiSat = Condition.getTrue().equals(recPsi.valueIn(store));
                                                    // System.err.printf("%s evaluates to %s", recPsi, recPsiSat);
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

                                // Compute total number of transitions.
                                int transitionCount = 1;
                                Set<AgentInstance> instancesToRemove = new HashSet<AgentInstance>();
                                for (AgentInstance ins : receivesMap.keySet()) {
                                    int receivesCount = receivesMap.get(ins).size();
                                    // System.err.printf("%s has %d receives over %s\n", ins, receivesCount, chan);
                                    if (!chan.toString().equals(Config.broadcast)) {
                                        transitionCount *= receivesCount;
                                    } else {
                                        // If this receiver has no receives
                                        // but the communication is broadcast,
                                        // just remove him from receivesMap
                                        transitionCount *= Math.max(transitionCount, 1);
                                        if (receivesCount == 0) {
                                            // System.err.printf("removing %s from bcast\n", ins);
                                            instancesToRemove.add(ins);
                                        }
                                    }
                                }
                                for (AgentInstance instance : instancesToRemove) {
                                    receivesMap.remove(instance);
                                }

                                // If nobody is blocking we can create transitions
                                if (transitionCount > 0) {
                                    Transition[] newTransitions = new Transition[transitionCount];
                                    for (int i = 0; i < transitionCount; i++) {
                                        newTransitions[i] = new Transition();
                                        newTransitions[i].setSender(sender);
                                        newTransitions[i].setSend(tr);
                                    }
                                    // System.err.printf("Generated %d transition objects\n", transitionCount);
                                    // Populate transitions with the cartesian product of receives
                                    for (AgentInstance receiver : receivesMap.keySet()) {
                                        // System.err.printf("Adding %s\n", receiver);
                                        int i = 0;
                                        while (i < transitionCount) {
                                            for (ProcessTransition transition : receivesMap.get(receiver)) {
                                                newTransitions[i].pushReceiver(receiver, transition);
                                                i++;
                                            }
                                        }
                                    }
                                    for (Transition t : newTransitions) {
                                        transitions.add(t);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            handleEvaluationException(e);
                        }
                    });    
                }
            });

        }
        
    }

    private Step currentStep;
    private recipe.lang.System sys;
    private Map<State, Set<ProcessTransition>> sends;
    private Map<State, Set<ProcessTransition>> receives;

    public Interpreter (recipe.lang.System s) {
        sys = s;
        sends = new HashMap<State, Set<ProcessTransition>>();
        receives = new HashMap<State, Set<ProcessTransition>>();

        // Set up send/receive tables
        sys.getAgents().forEach(a -> {
            a.getSendTransitions().forEach(send -> {
                sends.putIfAbsent(send.getSource(), new HashSet<ProcessTransition>());
                sends.get(send.getSource()).add(send);
            });
            a.getReceiveTransitions().forEach(receive -> {
                receives.putIfAbsent(receive.getSource(), new HashSet<ProcessTransition>());
                receives.get(receive.getSource()).add(receive);
            });
        });
    }

    private void rootStep(String constraint) throws IOException, Exception {
        HashMap<AgentInstance, ConcreteStore> rootStores = new HashMap<AgentInstance, ConcreteStore>();
        NuXmvInteraction nuxmv = new NuXmvInteraction(sys);
        Pair<Boolean, String> s0 = nuxmv.simulation_pick_init_state(constraint);
        JSONObject initValues = nuxmv.outputToJSON(s0.getRight());

        sys.getAgentInstances().forEach((x) -> {
            String name = x.getLabel();
            JSONObject jObj = initValues.getJSONObject(name);
            ConcreteStore ist = new ConcreteStore(jObj, x.getAgent());
            // System.out.println(ist);
            rootStores.put(x, ist);
        });
        // System.out.println(rootStores);
        this.currentStep = new Step(rootStores, null, this);
    }

    public void init(String constraint) throws IOException, Exception {
            assert sys != null;
            this.rootStep(constraint);
    }

    public void next(int index) {
        if (currentStep.transitions.size() == 0) {
            // TODO handle deadlocked states
        }
        currentStep = currentStep.next(index, this);
    }

    public void backtrack() {
        currentStep = currentStep.parent;
    }

    public void setSystem(recipe.lang.System s) {
        sys = s;
    }

    public Step getCurrentStep() {
        return currentStep;
    }
}
