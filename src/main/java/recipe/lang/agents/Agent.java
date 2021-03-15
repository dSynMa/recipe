package recipe.lang.agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import recipe.lang.actions.Action;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.expressions.StringValue;
import recipe.lang.expressions.StringVariable;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Attribute;
import recipe.lang.store.Store;
import recipe.lang.utils.Pair;

public class Agent {
    private String name;
    //TODO ensure that store variables and CV do not intersect, important for purposes of closure
    private Store store;
    private HashMap<String, Attribute<?>> CV;
    //TODO ensure that output store is a refinement of input store
    private Function<Pair<Store, HashMap<String, Attribute<?>>>, Store> relabel;
    private Set<String> states;  //control flow

    private HashMap<String, Set<Transition>> stateToOutgoingTransitions;

    private Set<Transition> sendTransitions;
    private Set<Transition> receiveTransitions;
    private Set<Action> actions;
    private String currentState;
    private Condition receiveGuard;
    private Condition initialCondition;

    public Agent(String name) {
        this(name, new Store(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new String(), Condition.FALSE);

    }

    public Agent(String name, Store store, Set<String> states, Set<Transition> sendTransitions, Set<Transition> receiveTransitions, Set<Action> actions,
                 String currentState, Condition receiveGuard) {
        this.name = name;
        this.store = store;
        this.states = new HashSet<>(states);
        this.sendTransitions = new HashSet<>(sendTransitions);
        this.receiveTransitions = new HashSet<>(receiveTransitions);
        this.actions = new HashSet<>(actions);
        this.currentState = currentState;
        this.receiveGuard = receiveGuard;
    }

    public Agent(String name, Store store, Set<String> states, Set<Transition> sendTransitions, Set<Transition> receiveTransitions, Set<Action> actions,
                 Function<Pair<Store, HashMap<String, Attribute<?>>>, Store> relabel, String currentState) {
        this.name = name;
        this.store = store;
        this.states = new HashSet<>(states);
        this.sendTransitions = new HashSet<>(sendTransitions);
        this.receiveTransitions = new HashSet<>(receiveTransitions);
        this.actions = new HashSet<>(actions);
        this.relabel = relabel;
        this.currentState = currentState;
    }

    public String getName() {
        return this.name;
    }

    public <T> T getValue(Attribute<T> attribute) throws AttributeTypeException {
        return store.getValue(attribute);
    }

    public <T> void setValue(Attribute<T> attribute, T value) throws AttributeTypeException {
        store.setValue(attribute, value);
    }

    public Store getStore() {
        return store;
    }

    protected void setStore(Store store) {
        this.store = store;
    }

    @Override
    public String toString() {
        return name + ":" + store.toString();
    }

    public <T> void storeUpdate(Map<Attribute<?>, Object> update) throws AttributeTypeException {
        if (update != null) {
            for (Attribute<?> att : update.keySet()) {
                store.setValue(att, update.get(att));
            }
        }

    }

    public Attribute<?> getAttribute(String name) {
        return store.getAttribute(name);
    }

   

    public Set<String> getStates() {
        return states;
    }

    public void setStates(Set<String> states) {
        this.states = states;
    }

    public Set<Action> getActions() {
        return actions;
    }

    public void setActions(Set<Action> actions) {
        this.actions = actions;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<String, Attribute<?>> getCV() {
        return CV;
    }

    public void setCV(HashMap<String, Attribute<?>> cV) {
        CV = cV;
    }

    public Function<Pair<Store, HashMap<String, Attribute<?>>>, Store> getrelabel() {
        return relabel;
    }

    public void setrelabel(Function<Pair<Store, HashMap<String, Attribute<?>>>, Store> relabel) {
        this.relabel = relabel;
    }

    public Store relabel() {
        Pair<Store, HashMap<String, Attribute<?>>> pair = new Pair<>(store, CV);
        return relabel.apply(pair);
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public Condition getReceiveGuard() {
        return receiveGuard;
    }

    public void setReceiveGuard(Condition receiveGuard) {
        this.receiveGuard = receiveGuard;
    }

    public Set<Transition> getSendTransitions() {
        return sendTransitions;
    }

    public void setSendTransitions(Set<Transition> sendTransitions) {
        this.sendTransitions = sendTransitions;
    }

    public Set<Transition> getReceiveTransitions() {
        return receiveTransitions;
    }

    public void setReceiveTransitions(Set<Transition> receiveTransitions) {
        this.receiveTransitions = receiveTransitions;
    }

    public Condition getInitialCondition() {
        return initialCondition;
    }

    public void setInitialCondition(Condition initialCondition) {
        this.initialCondition = initialCondition;
    }

    public boolean isListening(String channel, String state) throws AttributeTypeException, AttributeNotInStoreException {
        Condition concrete = receiveGuard.close(store, getCV().keySet());
        Store newstore = new Store();

        newstore.setValue(new Attribute<>("channel", String.class), channel);
        newstore.setValue(new Attribute<>("state", String.class), state);
        return concrete.isSatisfiedBy(newstore);
    }


    //try
    public static void main(String[] arg) throws AttributeTypeException, AttributeNotInStoreException {

        Store newstore =new Store();
        newstore.setValue(new Attribute<>("channel", String.class), "c");
        newstore.setValue(new Attribute<>("state", String.class), "pending");
        Condition cc =new And(new IsEqualTo(new StringVariable("channel"), new StringValue("c")),
        new IsEqualTo(new StringVariable("state"), new StringValue("ending")));
        
        boolean y = cc.isSatisfiedBy(newstore);
        System.out.println(y);
    }
}