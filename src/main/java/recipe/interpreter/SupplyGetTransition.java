package recipe.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.process.BasicProcessWithMessage;
import recipe.lang.process.GetProcess;
import recipe.lang.store.ConcreteStore;
import recipe.lang.store.Store;

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

    public Step next(Interpreter interpreter) {
        Step currentStep = interpreter.getCurrentStep();
        Map<AgentInstance, ConcreteStore> stores = currentStep.getStores();
        Map<AgentInstance, ConcreteStore> nextStores = new HashMap<>(stores);

        ConcreteStore supplierStore = stores.get(supplier);
        ConcreteStore getterStore = stores.get(getter);

        BasicProcessWithMessage supplyProcess = (BasicProcessWithMessage) supply.getLabel();
        BasicProcessWithMessage getProcess = (BasicProcessWithMessage) get.getLabel();

        Store supplyMessage = currentStep.makeMessageStore(supplierStore, supplyProcess, interpreter.sys);
        Store getMessage = currentStep.makeMessageStore(getterStore, getProcess, interpreter.sys);

        try {
            nextStores.put(supplier, supplierStore.BuildNext(supply, getMessage));
            nextStores.put(getter, getterStore.BuildNext(get, supplyMessage));
        } catch (Exception e) {
            currentStep.handleEvaluationException(e);
        }
        return new Step(nextStores, currentStep, interpreter);
    }

    @Override
    public Boolean satisfies(JSONObject constraint) {

        String supplyLbl = supply.getLabel().getLabel();
        String getLbl = get.getLabel().getLabel();
        String supplierName = supplier.getLabel();
        String getterName = getter.getLabel();
        JSONObject supplierState = constraint.optJSONObject(supplierName);
        if (supplierState == null) return true;
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
}