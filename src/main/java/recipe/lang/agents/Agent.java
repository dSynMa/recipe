package recipe.lang.agents;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.process.*;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.expressions.strings.StringVariable;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.process.Process;
import recipe.lang.store.Store;
import recipe.lang.utils.*;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class Agent {
    private String name;
    private Store store;
    private HashMap<String, TypedVariable> CV;
    private Map<TypedVariable, Expression> relabel;
    private Set<State> states;  //control flow
    private Set<ProcessTransition> sendTransitions;
    private Set<ProcessTransition> receiveTransitions;
    private Set<IterationExitTransition> iterationExitTransitions;
    private Set<Process> actions;
    private State initialState;
    private Condition receiveGuard;
    private Condition initialCondition;

    public Agent(String name) {
        this(name, new Store(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new State(name, new String()), Condition.FALSE);
    }

    public Agent(String name,
                 Store store,
                 Set<State> states,
                 Set<ProcessTransition> sendTransitions,
                 Set<ProcessTransition> receiveTransitions,
                 Set<IterationExitTransition> iterationExitTransitions,
                 Set<Process> actions,
                 State initialState,
                 Condition receiveGuard) {
        this.name = name;
        this.store = store;
        this.states = new HashSet<>(states);
        this.sendTransitions = new HashSet<>(sendTransitions);
        this.receiveTransitions = new HashSet<>(receiveTransitions);
        this.iterationExitTransitions = new HashSet<>(iterationExitTransitions);
        this.actions = new HashSet<>(actions);
        this.initialState = initialState;
        this.receiveGuard = receiveGuard;
    }

    public Agent(String name,
                 Store store,
                 Set<State> states,
                 Set<ProcessTransition> sendTransitions,
                 Set<ProcessTransition> receiveTransitions,
                 Set<IterationExitTransition> iterationExitTransitions,
                 Set<Process> actions,
                 Map<TypedVariable, Expression> relabel,
                 State initialState) {
        this.name = name;
        this.store = store;
        this.states = new HashSet<>(states);
        this.sendTransitions = new HashSet<>(sendTransitions);
        this.receiveTransitions = new HashSet<>(receiveTransitions);
        this.iterationExitTransitions = new HashSet<>(iterationExitTransitions);
        this.actions = new HashSet<>(actions);
        this.relabel = relabel;
        this.initialState = initialState;
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

    public void setCV(HashMap<String, TypedVariable> cV) throws CVAndStoreIntersectException {
        if(!Collections.disjoint(cV.keySet(), store.getAttributes().keySet())){
            throw new CVAndStoreIntersectException();
        }
        CV = CV;
    }

    public Map<TypedVariable, Expression> getRelabel() {
        return relabel;
    }

    public void setRelabel(Map<TypedVariable, Expression> relabel) {
        this.relabel = relabel;
    }

    public State getInitialState() {
        return initialState;
    }

    public void setInitialState(State initialState) {
        this.initialState = initialState;
    }

    public Condition getReceiveGuard() {
        return receiveGuard;
    }

    public void setReceiveGuard(Condition receiveGuard) {
        this.receiveGuard = receiveGuard;
    }

    public Set<ProcessTransition> getSendTransitions() {
        return sendTransitions;
    }

    public void setSendTransitions(Set<ProcessTransition> sendTransitions) {
        this.sendTransitions = sendTransitions;
    }

    public Set<ProcessTransition> getReceiveTransitions() {
        return receiveTransitions;
    }

    public void setReceiveTransitions(Set<ProcessTransition> receiveTransitions) {
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
                        Set<ProcessTransition> sendTransitions = new HashSet<>();
                        Set<ProcessTransition> receiveTransitions = new HashSet<>();
                        Set<IterationExitTransition> iterationExitTransitions = new HashSet<>();

                        Set<Process> actions = new HashSet<>();
                        transitions.forEach(tt -> {
                            if(tt.getClass().equals(ProcessTransition.class)) {
                                ProcessTransition t = (ProcessTransition) tt;
                                if (t.getLabel().getClass().equals(SendProcess.class)) {
                                    sendTransitions.add(t);
                                } else if (t.getLabel().getClass().equals(ReceiveProcess.class)) {
                                    receiveTransitions.add(t);
                                }

                                actions.add(t.getLabel());
                            } else if(tt.getClass().equals(IterationExitTransition.class)) {
                                iterationExitTransitions.add((IterationExitTransition) tt);
                            }
                        });
                        Set<State> states = new HashSet<>();
                        for(Transition tt : transitions){
                            if(tt.getClass().equals(ProcessTransition.class)) {
                                ProcessTransition t = (ProcessTransition) tt;
                                t.getSource().setAgent(agentName);
                                t.getDestination().setAgent(agentName);
                                t.setAgent(agentName);

                                states.add(t.getSource());
                                states.add(t.getDestination());
                            }
                        }

                        Agent agent = new Agent(agentName,
                                store,
                                states,
                                sendTransitions,
                                receiveTransitions,
                                iterationExitTransitions,
                                actions,
                                startState,
                                receiveGuardCondition);

                        agent.setRelabel(relabel);

                        return agent;
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

    public <T extends Transition> Map<State, Set<T>> getStateTransitionMap(Set<T> transitions) {
        HashMap<State, Set<T>> stateTransitionMap = new HashMap<>();

        for(Transition t : transitions){
            Set target;

            if(stateTransitionMap.containsKey(t.getSource())){
                target = stateTransitionMap.get(t.getSource());
            } else{
                target = new HashSet();
            }

            target.add(t);

            stateTransitionMap.put(t.getSource(), target);
        }

        return stateTransitionMap;
    }
}