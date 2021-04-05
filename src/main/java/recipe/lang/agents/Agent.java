package recipe.lang.agents;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.CompositionException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.process.Iterative;
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
import recipe.lang.utils.*;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class Agent {
    private String name;
    //TODO ensure that store variables and CV do not intersect, important for purposes of closure
    private Store store;
    private HashMap<String, TypedVariable> CV;
    //TODO ensure that output store is a refinement of input store
    private Function<Pair<Store, HashMap<String, TypedVariable>>, Store> relabel;
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
                 Function<Pair<Store, HashMap<String, TypedVariable>>, Store> relabel,
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
                        //TODO
                        Map<TypedVariable, Expression> relabel = (Map<TypedVariable, Expression>) values.get(3);
                        Condition receiveGuardCondition = (Condition) values.get(4);
                        Process repeat = (Process) values.get(5);
                        State startState = new State("start");
                        Set<Transition> transitions = repeat.asTransitionSystem(startState, new State("end"));
                        Set<ProcessTransition> sendTransitions = new HashSet<>();
                        Set<ProcessTransition> receiveTransitions = new HashSet<>();
                        //TODO
                        Set<IterationExitTransition> iterationExitTransitions = new HashSet<>();

                        Set<Process> actions = new HashSet<>();
                        transitions.forEach(tt -> {
                            if(tt.getClass().equals(ProcessTransition.class)) {
                                ProcessTransition t = (ProcessTransition) tt;
                                if (t.getAction().getClass().equals(SendProcess.class)) {
                                    sendTransitions.add(t);
                                } else if (t.getAction().getClass().equals(ReceiveProcess.class)) {
                                    receiveTransitions.add(t);
                                }

                                actions.add(t.getAction());
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

                        return new Agent(agentName,
                                store,
                                states,
                                sendTransitions,
                                receiveTransitions,
                                iterationExitTransitions,
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

    public static Agent composition(List<Agent> agents) throws AttributeTypeException, AttributeNotInStoreException, CompositionException {
        //TODO ensure agents have unique name below at level of parsing
        Agent composition = null;
        ;

        if(agents.size() == 0) throw new CompositionException("Empty list of agents.");
        if(agents.size() == 1) return agents.get(0);

        String[] names = new String[agents.size()];
        Store store = new Store();
        Set<State> states = new HashSet<>();
        State[] initialStates = new State[agents.size()];
        Map<State, Set<ProcessTransition>> stateSendTransitionMap = new HashMap<>();
        Map<State, Set<ProcessTransition>> stateReceiveTransitionMap = new HashMap<>();
        Map<State, Set<IterationExitTransition>> stateIterationExitTransitionMap = new HashMap<>();

        for(int i = 0; i < agents.size(); i++){
            Agent agent = agents.get(i);
            names[i] = agent.getName();
            store.update(agent.getStore().copyWithRenaming(s -> agent.name + "." + s));
            states.addAll(agent.getStates());
            initialStates[i] = agent.initialState;
            stateSendTransitionMap.putAll(agent.getStateTransitionMap(agent.sendTransitions));
            stateReceiveTransitionMap.putAll(agent.getStateTransitionMap(agent.receiveTransitions));
            stateIterationExitTransitionMap.putAll(agent.getStateTransitionMap(agent.iterationExitTransitions));
        }

        String name = String.join(" || ", names);

        State initialState = new State(initialStates);

        Set<ProcessTransition> sendTransitions = new HashSet<>();
        Set<ProcessTransition> receiveTransitions = new HashSet<>();
        Set<IterationExitTransition> iterationExitTransitions = new HashSet<>();

        Set<State<State[]>> current = new HashSet<>();
        current.add(initialState);

        Set<State<State[]>> alreadyDone = new HashSet<>();

        while (current.size() != 0){
            Set<State<State[]>> nextStates = new HashSet<>();

            for(State<State[]> state : current) {
                State[] individualStates = state.getLabel();

                for(int i = 0; i < individualStates.length; i++) {
                    State state1 = individualStates[i];

                    for (int j = 0; j < individualStates.length; j++) {
                        State state2 = individualStates[j];

                        for(IterationExitTransition iterationExitTransition : stateIterationExitTransitionMap.get(state2)){
                            State[] next = individualStates.clone();
                            next[j] = iterationExitTransition.getDestination();
                            State nextState = new State(name, next);
                            nextStates.add(nextState);

                            iterationExitTransitions.add(new IterationExitTransition(state, nextState, iterationExitTransition.getCondition()));
                        }

                        //Ensure this is below above loop, since the loop must be executed for state1 too.
                        if (state1 == state2) continue;

                        for (ProcessTransition t1 : stateSendTransitionMap.get(state1)) {
                            //if guard of sendtransition is true
                            for (ProcessTransition t2 : stateReceiveTransitionMap.get(state2)) {
                                if (t2.getSource().equals(state2)) {
                                    //if local guard of receive transition true
                                    //and (if receive-guard conjuncted with 'channel == receiveTransition.channel') is true
                                    //         or channel is *)
                                    //and the message guard is true on agent 2
                                    // then combine transitions

                                    //three possible transitions:
                                    //send and not listen
                                    //send and listen and have a receive transition and satisfy the send predicate
                                    //send and not satisfy the send predicate when channel is broadcast
                                }
                            }
                        }
                    }
                }
            }

            alreadyDone.addAll(current);
            current = nextStates;
            current.removeAll(alreadyDone);
        }

        Set<Process> actions;
        Function<Pair<Store, HashMap<String, TypedVariable>>, Store> relabel;

        composition = new Agent(name);

        return composition;
    }
}