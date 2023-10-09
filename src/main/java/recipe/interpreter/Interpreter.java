package recipe.interpreter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import recipe.Config;
import recipe.analysis.NuXmvInteraction;
import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.agents.State;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
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
import recipe.lang.utils.Pair;
import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.MismatchingTypeException;

public class Interpreter {
    private TypedVariable channTypedVariable;
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

    private interface Transition {
        public AgentInstance getInitiator();
        public Set<AgentInstance> getResponders();
        public ProcessTransition getInitiatorTransition();
        public BasicProcessWithMessage getInitiatorProcess();
        public ProcessTransition findTransitionForAgent(AgentInstance instance);
        public void setInitiator(AgentInstance instance, ProcessTransition transition) throws Exception;
        public void pushResponder(AgentInstance instance, ProcessTransition transition) throws Exception;
        public JSONObject toJSON();
    }

    private class SupplyGetTransition implements Transition {
        private AgentInstance supplier;
        private AgentInstance getter;
        private ProcessTransition supply;
        private ProcessTransition get;

        @Override
        public ProcessTransition getInitiatorTransition() {
            return supply;
        }

        @Override
        public BasicProcessWithMessage getInitiatorProcess() {
            return (BasicProcessWithMessage) supply.getLabel();
        }

        @Override
        public ProcessTransition findTransitionForAgent(AgentInstance instance) {
            String label = instance.getLabel();
            if (label == supplier.getLabel()) return supply;
            else if (label == getter.getLabel()) return get;
            // The agent did not take part in the transition
            else return null;
        }

        @Override
        public void setInitiator(AgentInstance instance, ProcessTransition transition) throws Exception {
            this.supplier = instance;
            this.supply = transition;
        }

        @Override
        public JSONObject toJSON() {
            // TODO Auto-generated method stub
            JSONObject result = new JSONObject();
            List<String> receiverNames = new ArrayList<>(1);
            receiverNames.add(getter.getLabel());
            result.put("sender", supplier.getLabel());
            result.put("send", supply.getLabel().toString());
            result.put("receivers", receiverNames);
            return result;
        }



        @Override
        public AgentInstance getInitiator() {
            return supplier;
        }

        @Override
        public Set<AgentInstance> getResponders() {
            Set<AgentInstance> result = new HashSet<>();
            result.add(getter);
            return result;
        }

        @Override
        public void pushResponder(AgentInstance instance, ProcessTransition transition) throws Exception {
            if (getter != null) {

            }
            if (!transition.getLabel().getClass().equals(GetProcess.class)) {
                throw new Exception("getter's transition must contain a GetProcess");
            }
            this.getter = instance;
            this.get = transition;
        }
    }

    private class SendReceiveTransition implements Transition {
        private AgentInstance sender;
        private ProcessTransition send;
        private Map<AgentInstance, ProcessTransition> receivers;
        
        public SendReceiveTransition() {
            receivers = new HashMap<AgentInstance, ProcessTransition>();
        }

        @Override
        public ProcessTransition findTransitionForAgent(AgentInstance instance) {
            if (instance.getLabel() == sender.getLabel()) {
                return send;
            }
            for (AgentInstance receiver : receivers.keySet()) {
                if (receiver.getLabel() == instance.getLabel()) {
                    return receivers.get(receiver);
                }
            }
            // The agent did not take part in the transition
            return null;
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

        // public void prettyPrint(PrintStream stream) {
        //     stream.println("--- transition ---");
        //     stream.print("Sender:\n");
        //     stream.printf("\n%s (%s)\t\t%s\n", sender.getLabel(), sender.getAgent().getName(), send);
        //     stream.print("Receivers:\n");
        //     for (AgentInstance receiver : receivers.keySet()) {
        //         stream.printf("%s (%s)\t\t%s\n", receiver.getLabel(), receiver.getAgent().getName(), receivers.get(receiver));
        //     }
        //     stream.println("------------------");
        // }

        // @Override
        // public SendProcess getInitiatorProcess() {
        //     return (SendProcess) send.getLabel();
        // }

        @Override
        public void setInitiator(AgentInstance instance, ProcessTransition transition) throws Exception {
            sender = instance;
            send = transition;
            if (!transition.getLabel().getClass().equals(SendProcess.class)) {
                throw new Exception("sender's transition must contain a SendProcess");
            }
        }
        
        public void pushResponder(AgentInstance instance, ProcessTransition receive) throws Exception {
            if (!receive.getLabel().getClass().equals(ReceiveProcess.class)) {
                throw new Exception("receiver's transition must contain a ReceiveProcess");
            }
            receivers.put(instance, receive);
        }

        @Override
        public ProcessTransition getInitiatorTransition() {
            return send;
        }

        @Override
        public BasicProcessWithMessage getInitiatorProcess() {
            return (BasicProcessWithMessage) send.getLabel();
        }

        @Override
        public AgentInstance getInitiator() {
            return sender;
        }

        @Override
        public Set<AgentInstance> getResponders() {
            return receivers.keySet();
        }
    }

    public class Step {
        private Map<AgentInstance,ConcreteStore> stores;
        private Map<TypedValue, Set<AgentInstance>> listeners;
        private List<Transition> transitions;
        private Transition chosenTransition;
        private recipe.lang.System sys;
        // The transition that led to this state
        private Transition inboundTransition;
        private int depth;
        private Step parent;
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
                    Expression<recipe.lang.types.Boolean> observation = obsMap.get(obsVar).getObservation();
                    AgentInstance sender = this.inboundTransition.getInitiator();
                    System.out.println(observation.toString());
                    System.out.println(sender.getLabel());
                    Store senderStore = this.parent.stores.get(sender);
                    try {
                        SendProcess sendProcess = (SendProcess) this.inboundTransition.getInitiatorTransition().getLabel();
                        Expression chanExpr = sendProcess.getChannel();
                        TypedValue chan = chanExpr.valueIn(senderStore);
                        Map<TypedVariable, TypedValue> mp = new HashMap<>();
                        mp.put(new TypedVariable<Type>(chan.getType(), "channel"), chan);
                        recipe.lang.types.Enum senderEnum = recipe.lang.types.Enum.getEnum(sender.getAgent().getName());
                        TypedValue senderName = null;
                        for (TypedValue tv : senderEnum.getAllValues()) {
                            // In some cases the agent instance name gets
                            // treated like a variable. So we just add variables
                            mp.put(new TypedVariable<Type>(tv.getType(), tv.toString()), tv);
                            if (tv.toString().equals(sender.getLabel())) {
                                senderName = tv;
                                break;
                            }
                        }
                        mp.put(new TypedVariable<Type>(Config.getAgentType(), "sender"), senderName);
                        Pair<Store, TypedValue> msgPair = makeMessageStore(senderStore, sendProcess, this.sys);
                        Store store = new ConcreteStore(mp).push(msgPair.getLeft());
                        boolean isObserved = Condition.getTrue().equals(observation.valueIn(store));
                        if (isObserved != ltol.get(obsVar).equals("TRUE")) {
                            System.out.printf(">> %s (%s): expected %s, got %s\n", obsVar, observation, ltol.get(obsVar), isObserved);
                            return false;
                        }
                    } catch (Exception e) {
                        handleEvaluationException(e);
                        // If the observation was supposed to be satisfied
                        // then something's wrong
                        if (ltol.get(obsVar).equals("TRUE"))
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
                        System.out.printf(">> Found annotation %s : %s\n", agentInstanceName, constraint.get(agentInstanceName));
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
                        TypedVariable var = inst.getAgent().getStore().getAttribute(varName);
                        TypedValue value = this.stores.get(inst).getValue(var);
                        if (value != null) {
                            if (value.getType() instanceof recipe.lang.types.Number) {
                                // Go for numerical comparison
                                try {
                                    Double x = Double.valueOf(instanceConstraint.get(varName).toString());
                                    Double y = Double.valueOf(value.getValue().toString());
                                    if (x - y > Double.MIN_VALUE) {
                                        System.out.printf(">> Expected %s, got %s (%s)\n", x, y, x-y);
                                        return false;
                                    }
                                } catch (NumberFormatException e) {
                                    handleEvaluationException(e);
                                    return false;
                                }
                                
                            }
                            else if (value.getType() instanceof recipe.lang.types.Boolean) {
                                // Go for Boolean comparison
                                boolean isConstraintTrue = instanceConstraint.get(varName).equals("TRUE");
                                if (!value.getValue().equals(isConstraintTrue)) {
                                    System.out.printf(">> Expected %s, got %s\n", isConstraintTrue, value.getValue());
                                    return false;
                                }
                            }

                            else if (!value.getValue().toString().equals(instanceConstraint.get(varName).toString())) {
                                System.out.printf(">> Expected %s, got %s\n", instanceConstraint.get(varName), value.getValue());
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
                AgentInstance initiator = chosenTransition.getInitiator();
                ConcreteStore nextSenderStore = stores.get(initiator).BuildNext(chosenTransition.getInitiatorTransition());
                nextStores.put(initiator, nextSenderStore);

                Pair<Store, TypedValue> msgPair = makeMessageStore(stores.get(initiator), chosenTransition.getInitiatorProcess(), sys);
                for (AgentInstance receiver : chosenTransition.getResponders()) {
                    ProcessTransition receive = chosenTransition.findTransitionForAgent(receiver);
                    ConcreteStore nextReceiverStore = stores.get(receiver).BuildNext(receive, msgPair.getLeft());
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

        protected Pair<Store, TypedValue> makeMessageStore(Store senderStore, BasicProcessWithMessage sendProcess, recipe.lang.System sys) {
            return makeMessageStore(senderStore, sendProcess, sys, false);
        }

        protected Pair<Store, TypedValue> makeMessageStore(Store senderStore, BasicProcessWithMessage sendProcess, recipe.lang.System sys, boolean ignoreChan) {
            // Add message and channel to a new map
            Map<TypedVariable, TypedValue> msgMap = new HashMap<TypedVariable, TypedValue>();
            Expression chanExpr = sendProcess.getChannel();
            TypedValue chan = null;
            try {
                sendProcess.getMessage().forEach((msgVar, msgExpr) -> {
                    try {
                        TypedValue msgVal = msgExpr.valueIn(senderStore);
                        Type msgType = sys.getMessageStructure().get(msgVar);
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
                if (!ignoreChan) {
                    chan = chanExpr.valueIn(senderStore);
                    msgMap.put(getChannelTV(), chan);
                }
                return new Pair<Store, TypedValue>(new ConcreteStore(msgMap), chan);
            } catch (Exception e) {
                handleEvaluationException(e);
            }
            return null;
        } 

        public Step(Map<AgentInstance,ConcreteStore> stores, Step parent, Interpreter interpreter) {
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
                        Store s = store.push(getChannelTV(), chan);
                        TypedValue evalGuard = instance.getAgent().getReceiveGuard().valueIn(s);
                        boolean isListening = Condition.getTrue().equals(evalGuard);
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
                        Expression<recipe.lang.types.Boolean> psi = sendProcess.getPsi();
                        try {
                            TypedValue psiEval = psi.valueIn(senderStore);
                            boolean psiSat = Condition.getTrue().equals(psiEval);

                            if (psiSat) {
                                // Add message and channel to a new map
                                Pair<Store, TypedValue> msgPair = makeMessageStore(senderStore, sendProcess, sys);
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
                                            Store instStore = stores.get(inst).push(msgStore);
                                            // Local variables get evaluated over the sender,
                                            // CVs get evaluated over the receiver (inst)
                                            CompositeStore sendGuardStore = new CompositeStore(sys);
                                            sendGuardStore.push(senderStore);
                                            sendGuardStore.pushReceiverStore(instStore);

                                            TypedValue sendGuard = sendProcess.getMessageGuard().valueIn(sendGuardStore);
                                            boolean sendGuardOk = Condition.getTrue().equals(sendGuard);

                                            TypedValue receiveGuard = inst.getAgent().getReceiveGuard().valueIn(instStore);
                                            boolean receiveGuardOk = Condition.getTrue().equals(receiveGuard);
                                            // System.out.printf("sendguard: %s (%s)\n", sendGuard, sendGuardOk);
                                            // System.out.printf("receiveguard: %s (%s)\n", receiveGuard, receiveGuardOk);

                                            if (sendGuardOk && receiveGuardOk) {
                                                receivesMap.put(inst, new HashSet<ProcessTransition>());
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
                                    SendReceiveTransition[] newTransitions = new SendReceiveTransition[transitionCount];
                                    for (int i = 0; i < transitionCount; i++) {
                                        newTransitions[i] = new SendReceiveTransition();
                                        newTransitions[i].setInitiator(sender, tr);
                                    }
                                    // System.err.printf("Generated %d transition objects\n", transitionCount);
                                    // Populate transitions with the cartesian product of receives
                                    for (AgentInstance receiver : receivesMap.keySet()) {
                                        // System.err.printf("Adding %s\n", receiver);
                                        int i = 0;
                                        while (i < transitionCount) {
                                            for (ProcessTransition transition : receivesMap.get(receiver)) {
                                                newTransitions[i].pushResponder(receiver, transition);
                                                i++;
                                            }
                                        }
                                    }
                                    for (SendReceiveTransition t : newTransitions) {
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

            //Compute available transitions (get/supply)
            interpreter.sys.getAgentInstances().forEach(supplier -> {
                ConcreteStore supplierStore = stores.get(supplier);
                Set<ProcessTransition> splys = interpreter.supplys.get(supplierStore.getState());
                if (splys == null) return;
                for (ProcessTransition sply : splys) {
                    SupplyProcess splyProc = (SupplyProcess) sply.getLabel();
                    Expression<recipe.lang.types.Boolean> psi = splyProc.getPsi();
                    try {
                        TypedValue psiEval = psi.valueIn(supplierStore);
                        boolean psiSat = Condition.getTrue().equals(psiEval);
                        if (!psiSat) continue;
                        for (AgentInstance getter : interpreter.sys.getAgentInstances()) {
                            if (getter == supplier) continue;
                            ConcreteStore getterStore = stores.get(getter);
                            Set<ProcessTransition> gets = interpreter.gets.get(getterStore.getState());
                            if (gets == null) continue;
                            for (ProcessTransition get : gets) {
                                Pair<Store, TypedValue> msgPair = makeMessageStore(supplierStore, splyProc, sys, true);
                                Store instStore = getterStore.push(msgPair.getLeft());
                                GetProcess getProc = (GetProcess) get.getLabel();
                                Expression getPsi = getProc.getPsi();
                                boolean getPsiSat = Condition.getTrue().equals(getPsi.valueIn(instStore));
                                if (!getPsiSat) continue;
                                // Check if predicates match
                                // TODO this only works for predicates, extend to instance names
                                if (!Condition.getTrue().equals(splyProc.getMessageGuard().valueIn(getterStore))) continue;
                                if (!Condition.getTrue().equals(getProc.getMessageGuard().valueIn(supplierStore))) continue;

                                // System.out.println(sply.toString() + "   " + get.toString());
                                Transition tr = new SupplyGetTransition();
                                tr.setInitiator(supplier, sply);
                                tr.pushResponder(getter, get);
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

    private Step currentStep;
    private recipe.lang.System sys;
    private Map<State, Set<ProcessTransition>> sends;
    private Map<State, Set<ProcessTransition>> receives;
    private Map<State, Set<ProcessTransition>> gets;
    private Map<State, Set<ProcessTransition>> supplys;

    public Interpreter (recipe.lang.System s) {
        sys = s;
        sends = new HashMap<State, Set<ProcessTransition>>();
        receives = new HashMap<State, Set<ProcessTransition>>();
        gets = new HashMap<State, Set<ProcessTransition>>();
        supplys = new HashMap<State, Set<ProcessTransition>>();

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
            a.getGetTransitions().forEach(get -> {
                gets.putIfAbsent(get.getSource(), new HashSet<ProcessTransition>());
                gets.get(get.getSource()).add(get);
            });
            a.getSupplyTransitions().forEach(sply -> {
                supplys.putIfAbsent(sply.getSource(), new HashSet<ProcessTransition>());
                supplys.get(sply.getSource()).add(sply);
            });
        });
    }

    public List<JSONObject> traceToJSON () {
        List<JSONObject> result = new LinkedList<>();
        Step myStep = currentStep;
        while(myStep != null) {
            result.add(0, myStep.toJSON());
            myStep = myStep.parent;
        }
        return result;
    }

    public static Interpreter ofJSON(recipe.lang.System s, Map<String,Observation> obsMap, JSONArray json) throws Exception {
        List<JSONObject> list = new ArrayList<>(json.length());
        for (int i = 0; i < json.length(); i++) {
            list.add(json.getJSONObject(i));
        }
        return Interpreter.ofJSON(s, obsMap, list);
    }

    public static Interpreter ofJSON(recipe.lang.System s, Map<String,Observation> obsMap, List<JSONObject> states) throws Exception {
        if (states.size() == 0) {
            throw new Exception("Empty JSON");
        }
        Interpreter interpreter = new Interpreter(s);
        JSONObject initState = states.get(0);

        // Compute set of variables in the system
        Set<String> varNames = new HashSet<>(s.getCommunicationVariables().keySet());
        for (Agent a : s.getAgents()) {
            varNames.addAll(a.getStore().getAttributes().keySet());
        }

        // Create constraint on initial state & find it
        List<String> constraints = new LinkedList<String>();
        
        for (String agentInstance : initState.keySet()) {
            if (agentInstance.startsWith("___")) continue;
            JSONObject agentState = initState.getJSONObject(agentInstance);
            for (String var : agentState.keySet()) {
                if (varNames.contains(var)) {
                    constraints.add(String.format("%s-%s = %s", agentInstance, var, agentState.get(var)));
                }
            }
        }
        String initConstraint = String.join(" & ", constraints);
        interpreter.init(initConstraint);
        // Walk the rest of the trace 
        for (int i = 1; i < states.size(); i++) {
            JSONObject constraint = states.get(i);
            if (interpreter.isDeadlocked()) {
                // If we are deadlocked, but all constraints are on LTOL
                boolean onlyNotes = true;
                for (String key : constraint.keySet()) {
                    onlyNotes &= key.startsWith("___");
                } 
                if (onlyNotes) break;
            }
            if (!interpreter.findNext(obsMap, constraint)) {
                throw new Exception(String.format("[ofTrace] something wrong at step %d", i));
            }
        }
        return interpreter;
    }

    /**
     * Loads a nuXmv trace into a new intepreter and returns it
     * 
     * @param s the r-check system
     * @param obsMap a map from string to LTOL observations
     * @param trace a nuXmv trace
     * @return an instance of Interpreter
     */
    public static Interpreter ofTrace(recipe.lang.System s, Map<String,Observation> obsMap, String trace) throws Exception {
        // Find states that are loop-starts
        Matcher m = Pattern.compile(
            "-- Loop starts here[\s\n]*-> State: 1.([0-9]+)",
            Pattern.DOTALL
        ).matcher(trace);
        Set<Integer> loopingStates = new HashSet<>();
        while (m.find()) {
            // We do -1 since nuXmv numbers states from 1
            loopingStates.add(Integer.valueOf(m.group(1))-1);
        }

        // Remove loop annotations
        trace = trace.replaceAll("-- Loop starts here\n", "");
        String sentinel = "Trace Type: Counterexample";
        int startPos = trace.indexOf(sentinel) + sentinel.length();
        trace = trace.substring(startPos);
        
        NuXmvInteraction nuxmv = new NuXmvInteraction(s);
        nuxmv.stopNuXmvThread(); // We ain't going to need it
        String[] split = trace.split("->", 0);
        List<JSONObject> states = new ArrayList<>(split.length - 1);
        for (String string : split) {
            // Skip stuff that does not contain a state (shouldn't happen)
            if (!string.contains("<-")) continue; 

            JSONObject state = nuxmv.outputToJSON(string);
            if (loopingStates.contains(states.size())) {
                state.put("___LOOP___", true);
            }
            states.add(state);
        }
        return Interpreter.ofJSON(s, obsMap, states);
    }

    private void rootStep(String constraint) throws IOException, Exception {
        HashMap<AgentInstance, ConcreteStore> rootStores = new HashMap<AgentInstance, ConcreteStore>();
        NuXmvInteraction nuxmv = new NuXmvInteraction(sys);
        Pair<Boolean, String> s0 = nuxmv.simulation_pick_init_state(constraint);
        JSONObject initValues = nuxmv.outputToJSON(s0.getRight());
        nuxmv.stopNuXmvThread();

        sys.getAgentInstances().forEach((x) -> {
            String name = x.getLabel();
            if (!initValues.has(name)) {
                System.err.print(">>> ");
                System.err.println(initValues.toString(0));
            }
            JSONObject jObj = initValues.getJSONObject(name);
            ConcreteStore ist = new ConcreteStore(jObj, x);
            rootStores.put(x, ist);
        });
        this.currentStep = new Step(rootStores, null, this);
    }

    public void init(String constraint) throws IOException, Exception {
            assert sys != null;
            this.rootStep(constraint);
    }

    public boolean isDeadlocked() {
        return currentStep.transitions.size() == 0;
    }

    public boolean findNext(Map<String,Observation> obsMap, JSONObject constraint) {
        Step candidate;
        System.out.printf("(%d) constraint: %s, transitions: %s\n", currentStep.depth, constraint, currentStep.transitions.size());
        for (int i = 0; i < currentStep.transitions.size(); i++) {
            candidate = currentStep.next(i, this); 
            // TODO here we just pick the 1st transition to a target state
            // that satisfies the constraint. Is this always enough?
            if (candidate.satisfies(obsMap, constraint)) {
                candidate.loadAnnotations(constraint);
                currentStep = candidate;
                // System.out.println(currentStep.toJSON());
                return true;
            }
        }
        return false;
    }

    public void next(int index) {
        if (isDeadlocked()) {
            // Deadlocked state, do nothing
            return;
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
