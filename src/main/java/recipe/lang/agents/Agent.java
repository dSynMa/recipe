package recipe.lang.agents;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.Config;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.process.*;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.process.Process;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Type;
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
    private Expression<Boolean> receiveGuard;
    private Expression<Boolean> initialCondition;

    public Agent(String name) throws MismatchingTypeException {
        this(name, new Store(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new State(name, new String()), new TypedValue<Boolean>(Boolean.getType(), "false"));
    }

    public Agent(String name,
                 Store store,
                 Set<State> states,
                 Set<ProcessTransition> sendTransitions,
                 Set<ProcessTransition> receiveTransitions,
                 Set<IterationExitTransition> iterationExitTransitions,
                 Set<Process> actions,
                 State initialState,
                 Expression<Boolean> receiveGuard) {
        this.name = name.trim();
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
        this.name = name.trim();
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
        this.name = name.trim();
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

    public Expression<Boolean> getReceiveGuard() {
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

    public Set<IterationExitTransition> getIterationExitTransitions() {
        return iterationExitTransitions;
    }

    public void setIterationExitTransitions(Set<IterationExitTransition> iterationExitTransitions) {
        this.iterationExitTransitions = iterationExitTransitions;
    }

    public Expression<Boolean> getInitialCondition() {
        return initialCondition;
    }

    public void setInitialCondition(Condition initialCondition) {
        this.initialCondition = initialCondition;
    }

//    public boolean isListening(String channel, String state) throws Exception {
//        Condition concrete = receiveGuard.close(store, getCV().keySet());
//        Store newstore = new Store();
//
//        recipe.lang.types.Enum channels = recipe.lang.types.Enum.getEnum("channels");
//        newstore.setValue(new TypedVariable(channels, "channel"), new TypedValue(channels, channel));
//        newstore.setValue(new StringVariable("state"), new StringValue(state));
//        return concrete.isSatisfiedBy(newstore);
//    }

    public static org.petitparser.parser.Parser parser(TypingContext messageContext,
                                                              TypingContext communicationContext,
                                                              TypingContext channelContext) throws Exception {
        SettableParser parser = SettableParser.undefined();
        Function<TypingContext, Parser> process = (TypingContext localContext) -> {
            try {
                return Parsing.labelledParser("repeat", Process.parser(messageContext, localContext, communicationContext, channelContext));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        Parser name = word().plus().trim().flatten();

        AtomicReference<String> error = new AtomicReference<>("");
        AtomicReference<TypingContext> localContext = new AtomicReference<>(new TypingContext());

        TypingContext channelValueContext = channelContext.getSubContext(recipe.lang.types.Enum.getEnum(Config.channelLabel));

        parser.set(StringParser.of("agent").trim()
                    .seq(name)
                    .seq(Parsing.labelledParser("local", Parsing.typedAssignmentList(channelValueContext))
                            .mapWithSideEffects((Pair<Map<String, TypedVariable>, Map<String, TypedValue>> values) -> {
                                Map<String, Type> varTypes = new HashMap();
                                for(Map.Entry<String, TypedVariable> entry : values.getLeft().entrySet()){
                                    varTypes.put(entry.getKey(), entry.getValue().getType());
                                }
                                localContext.get().setAll(new TypingContext(varTypes));
                                return values;
                            }))
                    .seq(new LazyParser<>(((TypingContext localContext1) -> Parsing.relabellingParser(localContext1, communicationContext)), localContext.get()).trim())
                    .seq(new LazyParser<>(((TypingContext localContext1) -> {
                        try {
                            return Parsing.receiveGuardParser(localContext1, channelContext);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }), localContext.get()))
                    .seq(new LazyParser(process, localContext.get()).trim())
                .map((List<Object> values) -> {
                    String agentName = ((String) values.get(1)).trim();
                    Map<String, TypedVariable> localVars = ((Pair<Map, Map>) values.get(2)).getLeft();
                    Map<String, TypedValue> localValues = ((Pair<Map, Map>) values.get(2)).getRight();
                    Store store = null;
                    try {
                        store = new Store(localValues, localVars);
                        Map<TypedVariable, Expression> relabel = (Map<TypedVariable, Expression>) values.get(3);
                        Expression<Boolean> receiveGuardCondition = (Expression<Boolean>) values.get(4);
                        Process repeat = (Process) values.get(5);
                        State startState = new State("start");
                        Set<Transition> transitions = repeat.asTransitionSystem(startState, startState);
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