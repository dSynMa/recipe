package recipe.lang.agents;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.CompositionWithSameAgentNameException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.process.Process;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.expressions.strings.StringVariable;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;
import recipe.lang.store.Store;
import recipe.lang.utils.LazyParser;
import recipe.lang.utils.Pair;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class Agent {
    private String name;
    //TODO ensure that store variables and CV do not intersect, important for purposes of closure
    private Store store;
    private HashMap<String, TypedVariable> CV;
    //TODO ensure that output store is a refinement of input store
    private Function<Pair<Store, HashMap<String, TypedVariable>>, Store> relabel;
    private Set<State> states;  //control flow

    private HashMap<String, Set<Transition>> stateToOutgoingTransitions;

    private Set<Transition> sendTransitions;
    private Set<Transition> receiveTransitions;
    private Set<Process> actions;
    private State currentState;
    private Condition receiveGuard;
    private Condition initialCondition;

    public Agent(String name) {
        this(name, new Store(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new State(new String()), Condition.FALSE);

    }

    public Agent(String name, Store store, Set<State> states, Set<Transition> sendTransitions, Set<Transition> receiveTransitions, Set<Process> actions,
                 State currentState, Condition receiveGuard) {
        this.name = name;
        this.store = store;
        this.states = new HashSet<>(states);
        this.sendTransitions = new HashSet<>(sendTransitions);
        this.receiveTransitions = new HashSet<>(receiveTransitions);
        this.actions = new HashSet<>(actions);
        this.currentState = currentState;
        this.receiveGuard = receiveGuard;
    }

    public Agent(String name, Store store, Set<State> states, Set<Transition> sendTransitions, Set<Transition> receiveTransitions, Set<Process> actions,
                 Function<Pair<Store, HashMap<String, TypedVariable>>, Store> relabel, State currentState) {
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

    public TypedValue getValue(TypedVariable attribute) throws AttributeTypeException {
        return store.getValue(attribute);
    }

    public void setValue(TypedVariable attribute, TypedValue value) throws AttributeTypeException {
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

    public void storeUpdate(Map<TypedVariable, TypedValue> update) throws AttributeTypeException {
        if (update != null) {
            for (TypedVariable att : update.keySet()) {
                store.setValue(att, update.get(att));
            }
        }

    }

    public TypedVariable getAttribute(String name) throws AttributeNotInStoreException {
        return store.getAttribute(name);
    }

    public Set<State> getStates() {
        return states;
    }

    public void setStates(Set<State> states) {
        this.states = states;
    }

    public Set<Process> getActions() {
        return actions;
    }

    public void setActions(Set<Process> actions) {
        this.actions = actions;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<String, TypedVariable> getCV() {
        return CV;
    }

    public void setCV(HashMap<String, TypedVariable> cV) {
        CV = cV;
    }

    public Function<Pair<Store, HashMap<String, TypedVariable>>, Store> getrelabel() {
        return relabel;
    }

    public void setrelabel(Function<Pair<Store, HashMap<String, TypedVariable>>, Store> relabel) {
        this.relabel = relabel;
    }

    public Store relabel() {
        Pair<Store, HashMap<String, TypedVariable>> pair = new Pair<>(store, CV);
        return relabel.apply(pair);
    }

    public State getCurrentState() {
        return currentState;
    }

    public void setCurrentState(State currentState) {
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

        newstore.setValue(new ChannelVariable("channel"), new ChannelValue(channel));
        newstore.setValue(new StringVariable("state"), new StringValue(state));
        return concrete.isSatisfiedBy(newstore);
    }


    //try
    public static void main(String[] arg) throws AttributeTypeException, AttributeNotInStoreException {

        Store newstore =new Store();
        newstore.setValue(new ChannelVariable("channel"), new ChannelValue("c"));
        newstore.setValue(new StringVariable("state"), new StringValue("pending"));
        Condition cc =new And(new IsEqualTo(new StringVariable("channel"), new StringValue("c")),
        new IsEqualTo(new StringVariable("state"), new StringValue("ending")));
        
        boolean y = cc.isSatisfiedBy(newstore);
        System.out.println(y);
    }

    public static org.petitparser.parser.Parser parser(TypingContext messageContext,
                                                              TypingContext communicationContext,
                                                              TypingContext channelContext){
        SettableParser parser = SettableParser.undefined();
        Function<TypingContext, Parser> process = (TypingContext localContext) -> Parsing.labelledParser("repeat", Process.parser(messageContext, localContext, communicationContext, channelContext));

        Parser name = word().plus().trim().flatten();

        AtomicReference<String> error = new AtomicReference<>("");
        AtomicReference<TypingContext> localContext = new AtomicReference<>(new TypingContext());

        TypingContext channelValueContext = channelContext.getSubContext(ChannelValue.class);

        parser.set(StringParser.of("agent").trim()
                    .seq(name)
                    .seq(Parsing.labelledParser("local", Parsing.typedAssignmentList(channelValueContext))
                            .mapWithSideEffects((Pair<Map, Map> values) -> {
                                localContext.get().setAll(new TypingContext(values.getLeft()));
                                return values;
                            }))
                    .seq(new LazyParser<>(((TypingContext localContext1) -> Parsing.relabellingParser(localContext1, communicationContext)), localContext.get()).trim())
                    .seq(new LazyParser<>(((TypingContext localContext1) -> Parsing.receiveGuardParser(localContext1, channelContext)), localContext.get()))
                    .seq(new LazyParser(process, localContext.get()).trim())
                .map((List<Object> values) -> {
                    //TODO
                    String agentName = (String) values.get(1);
                    Map<String, TypedVariable> localVars = ((Pair<Map, Map>) values.get(2)).getLeft();
                    Map<String, Expression> localValues = ((Pair<Map, Map>) values.get(2)).getRight();
                    Store store = null;
                    try {
                        store = new Store(localValues, localVars);
                        Map<TypedVariable, Expression> relabel = (Map<TypedVariable, Expression>) values.get(3);
                        Condition receiveGuardCondition = (Condition) values.get(4);
                        Process repeat = (Process) values.get(5);
                        State startState = new State("start");
                        Set<Transition> transitions = repeat.asTransitionSystem(startState, new State("end"));
                        Set<Transition> sendTransitions = new HashSet<>();
                        Set<Transition> receiveTransitions = new HashSet<>();
                        Set<Process> actions = new HashSet<>();
                        transitions.forEach(t -> {
                            if(t.getAction().getClass().equals(SendProcess.class)){
                                sendTransitions.add(t);
                            } else if(t.getAction().getClass().equals(ReceiveProcess.class)){
                                receiveTransitions.add(t);
                            }

                            actions.add(t.getAction());
                        });
                        Set<State> states = new HashSet<>();
                        for(Transition t : transitions){
                            states.add(t.getSource());
                            states.add(t.getDestination());
                        }

                        return new Agent(agentName,
                                store, states,
                                sendTransitions,
                                receiveTransitions,
                                actions,
                                startState,
                                receiveGuardCondition);
                    } catch (AttributeTypeException e) {
                        error.set(e.toString());
                    } catch (AttributeNotInStoreException e) {
                        error.set(e.toString());
                    }

                    return null;
                })
                .seq(Parsing.conditionalFail(!error.get().equals("")))
                    .map((List<Object> values) -> {
                        return values.get(0);
                    }));

        return parser;
    }

    public static Agent composition(Agent agent1, Agent agent2) throws AttributeTypeException, AttributeNotInStoreException, CompositionWithSameAgentNameException {
        if(agent1.getName().equals(agent2.getName())){
            throw new CompositionWithSameAgentNameException();
        }

        Agent composition = null;

        String name = agent1.name + " || " + agent2.name;

        Store store = agent1.getStore().copyWithRenaming(s -> agent1.name + "." + s);
        store.update(agent2.getStore().copyWithRenaming(s -> agent2.name + "." + s));

        Set<String> states = new HashSet<>();
        agent1.states.forEach(s -> states.add(agent1.name + "." + s));
        agent2.states.forEach(s -> states.add(agent2.name + "." + s));

        String currentState = "(" + agent1.name + "." + agent1.currentState + ", " + agent2.name + "." + agent2.currentState + ")";

        Set<String> current = new HashSet<>();
        current.add(currentState);

        Set<String> alreadyDone = new HashSet<>();

        while (current.size() != 0){
            Set<String> newCurrent = new HashSet<>();

            for (Transition t1 : agent1.sendTransitions){
                for(Transition t2 : agent2.receiveTransitions){

                }
            }

            alreadyDone.addAll(current);
            current = newCurrent;
            current.removeAll(alreadyDone);
        }

        Set<Transition> sendTransitions;
        Set<Transition> receiveTransitions;
        Set<Process> actions;
        Function<Pair<Store, HashMap<String, TypedVariable>>, Store> relabel;

        return composition;
    }
}