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
import recipe.lang.utils.Pair;
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
                AgentInstance sender = this.inboundTransition.getProducer();
                System.out.println(observation.toString());
                System.out.println(sender.getLabel());
                Store senderStore = this.parent.stores.get(sender);
                try {
                    SendProcess sendProcess = (SendProcess) this.inboundTransition.getProducerTransition().getLabel();
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
                    TypedVariable var = inst.getAgent().getStore().getAttribute(varName);
                    TypedValue value = this.stores.get(inst).getValue(var);
                    if (value != null) {
                        if (value.getType() instanceof recipe.lang.types.Number) {
                            // Go for numerical comparison
                            try {
                                Double x = Double.valueOf(instanceConstraint.get(varName).toString());
                                Double y = Double.valueOf(value.getValue().toString());
                                double delta = Math.abs(x - y);
                                if (delta > Double.MIN_VALUE) {
                                    System.out.printf(
                                        ">>%s-%s: Expected %s, got %s (%s)\n",
                                        inst.getLabel(), varName, x, y, delta);
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

            Pair<Store, TypedValue> msgPair = makeMessageStore(stores.get(initiator), chosenTransition.getProducerProcess(), sys);
            for (AgentInstance receiver : chosenTransition.getConsumers()) {
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

    protected Pair<Store, TypedValue> makeMessageStore(Store senderStore, BasicProcessWithMessage procWithMsg, recipe.lang.System sys) {
        // Add message and channel to a new map
        Map<TypedVariable, TypedValue> msgMap = new HashMap<TypedVariable, TypedValue>();
        TypedValue chan = null;
        try {
            procWithMsg.getMessage().forEach((msgVar, msgExpr) -> {
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
            if (procWithMsg instanceof SendProcess) {
                Expression chanExpr = procWithMsg.getChannel();
                chan = chanExpr.valueIn(senderStore);
                msgMap.put(interpreter.getChannelTV(), chan);
            }
            return new Pair<Store, TypedValue>(new ConcreteStore(msgMap), chan);
        } catch (Exception e) {
            handleEvaluationException(e);
        }
        return null;
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
                                    newTransitions[i].setProducer(sender, tr);
                                }
                                // System.err.printf("Generated %d transition objects\n", transitionCount);
                                // Populate transitions with the cartesian product of receives
                                for (AgentInstance receiver : receivesMap.keySet()) {
                                    // System.err.printf("Adding %s\n", receiver);
                                    int i = 0;
                                    while (i < transitionCount) {
                                        for (ProcessTransition transition : receivesMap.get(receiver)) {
                                            newTransitions[i].pushConsumer(receiver, transition);
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
                            Pair<Store, TypedValue> msgPair = makeMessageStore(supplierStore, splyProc, sys);
                            Store instStore = getterStore.push(msgPair.getLeft());
                            GetProcess getProc = (GetProcess) get.getLabel();
                            Expression getPsi = getProc.getPsi();
                            boolean getPsiSat = Condition.getTrue().equals(getPsi.valueIn(instStore));
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
                            if (!(splyGuard instanceof NamedLocation) && !Condition.getTrue().equals(splyGuard.valueIn(getterStore))) continue;
                            if (!(getGuard instanceof NamedLocation) && !Condition.getTrue().equals(getGuard.valueIn(supplierStore))) continue;

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