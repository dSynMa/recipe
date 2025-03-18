package recipe.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import recipe.Config;
import recipe.analysis.ToNuXmv;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.location.Location;
import recipe.lang.process.BasicProcess;
import recipe.lang.process.BasicProcessWithMessage;
import recipe.lang.process.GetProcess;
import recipe.lang.process.SupplyProcess;
import recipe.lang.store.ConcreteStore;
import recipe.lang.store.Store;
import recipe.lang.types.Type;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

class SupplyGetTransition implements Transition {
    private AgentInstance supplier;
    private AgentInstance getter;
    private ProcessTransition supply;
    private ProcessTransition get;

    @Override
    public ProcessTransition getProducerTransition() {
        return supply;
    }

    @Override
    public BasicProcessWithMessage getProducerProcess() {
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
    public void setProducer(AgentInstance instance, ProcessTransition transition) throws Exception {
        this.supplier = instance;
        this.supply = transition;
    }

    @Override
    public JSONObject toJSON() {
        // TODO Auto-generated method stub
        JSONObject result = new JSONObject();
        List<String> receiverNames = new ArrayList<>(1);
        receiverNames.add(getter.getLabel());
        result.put("___get-supply___", true);
        result.put("sender", supplier.getLabel());
        result.put("send", supply.getLabel().toString());
        result.put("receivers", receiverNames);
        return result;
    }


    @Override
    public AgentInstance getProducer() {
        return supplier;
    }

    @Override
    public Set<AgentInstance> getConsumers() {
        Set<AgentInstance> result = new HashSet<>();
        result.add(getter);
        return result;
    }

    @Override
    public void pushConsumer(AgentInstance instance, ProcessTransition transition) throws Exception {
        if (getter != null) {

        }
        if (!transition.getLabel().getClass().equals(GetProcess.class)) {
            throw new Exception("getter's transition must contain a GetProcess");
        }
        this.getter = instance;
        this.get = transition;
    }

    @Override
    public Boolean satisfies(JSONObject constraint) {
        String supplyLbl = supply.getLabel().getLabel();
        String getLbl = get.getLabel().getLabel();
        String supplierName = supplier.getLabel();
        String getterName = getter.getLabel();
        JSONObject supplierState = constraint.getJSONObject(supplierName);
        for (String key : supplierState.keySet()) {
            if (!supplierState.get(key).equals("TRUE")) continue;
            String[] pieces = key.split("-");
            if (pieces.length != 3) continue;
            Boolean check = pieces[1].equals(getterName);
            if (!pieces[0].contains("unlabelled_supply"))
                check &= pieces[0].equals(supply.getLabel().getLabel());
            else
                check &= (supplyLbl == null || supplyLbl.isBlank());
            if (!pieces[2].contains("unlabelled_get"))
                check &= pieces[2].equals(get.getLabel().getLabel());
            else
                check &= (getLbl == null || getLbl.isBlank());
            return check;
        }
        return true;
    }

    @Override
    public AgentInstance getInitiator() {
        return getter;
    }

    @Override
    public Expression<recipe.lang.types.Boolean> getSpecializedObservation(Map<String, Type> cvs, Expression<recipe.lang.types.Boolean> obs) throws Exception {
        Type agentType = Config.getAgentType();
        TypedValue supplierTV = new TypedValue<Type>(agentType, supplier.getLabel());
        TypedValue getterTV = new TypedValue<Type>(agentType, getter.getLabel());
        SupplyProcess supplyProcess = (SupplyProcess) getProducerProcess();
        GetProcess getProcess = (GetProcess) get.getLabel();
        
        // Handle message guard
        Location getterLoc = getProcess.getLocation();
        Expression<recipe.lang.types.Boolean> getterPredicate = getterLoc.getPredicate(supplierTV);
        
        getterPredicate = getterPredicate.relabel(v -> {
            //relabelling local variables to those of the sending agents
            return cvs.containsKey("@" + v.getName())
                ? v
                : getter.getAgent().getStore().getAttributes().containsKey(v.getName()) 
                ? v.sameTypeWithName(getter.getLabel() + "-" + v)
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
                return cvs.containsKey("@" + v.getName())
                        ? supplier.getAgent().getRelabel().get(v).relabel(vv -> ((TypedVariable) vv).sameTypeWithName(supplier.getLabel() + "-" + vv))
                        : v;
            } catch (RelabellingTypeException | MismatchingTypeException e) {
                e.printStackTrace();
            }
            return null;
        }).simplify();

        return ToNuXmv.specialiseObservationToSupplyTransition(
            cvs, obs, getterPredicate, supplyProcess.getMessage(), supplierTV, getterTV, Config.getNoAgent(), supplier.getAgent());
    }

    @Override
    public Step next(Interpreter interpreter) {
        Step currentStep = interpreter.getCurrentStep();
        Map<AgentInstance, ConcreteStore> stores = currentStep.getStores();
        Map<AgentInstance,ConcreteStore> nextStores = new HashMap<AgentInstance,ConcreteStore>(stores);

        ConcreteStore supplierStore = stores.get(supplier);
        ConcreteStore getterStore = stores.get(getter);

        BasicProcessWithMessage sp = (BasicProcessWithMessage) supply.getLabel();
        BasicProcessWithMessage gp = (BasicProcessWithMessage) get.getLabel();
        
        Store splyMsgStore = currentStep.makeMessageStore(supplierStore, sp, interpreter.sys).getLeft();
        Store getMsgStore = currentStep.makeMessageStore(getterStore, gp, interpreter.sys).getLeft();

        try {
            ConcreteStore nextSupplierStore = supplierStore.BuildNext(supply, getMsgStore);
            ConcreteStore nextGetterStore = getterStore.BuildNext(get, splyMsgStore);
            nextStores.put(supplier, nextSupplierStore);
            nextStores.put(getter, nextGetterStore);
        } catch (Exception e) {
            currentStep.handleEvaluationException(e);
        }
        Step next = new Step(nextStores, currentStep, interpreter);
        return next;
    }

    @Override
    public Set<AgentInstance> getUnhappyConsumers(Interpreter interpreter) {
        Set<AgentInstance> result = new HashSet<>();
        Map<String, Type> struct =  interpreter.sys.getMessageStructure();

        Set<String> supplierWants = get.getLabel().wantedData(struct);
        System.err.printf("supplier wants %s\n", supplierWants.toString());
        BasicProcessWithMessage gp = (GetProcess) get.getLabel();
        if (!gp.getMessage().keySet().containsAll(supplierWants)){
            result.add(getter);
        }

        BasicProcessWithMessage sp = (GetProcess) get.getLabel();
        Set<String> getterWants = get.getLabel().wantedData(struct);
        System.err.printf("getter wants %s\n", supplierWants.toString());
        if (!sp.getMessage().keySet().containsAll(getterWants)){
            result.add(supplier);
        }
        return result;
    }

}