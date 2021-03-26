package recipe.lang.agents;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.StringParser;
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
import recipe.lang.store.Store;
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
    private Set<String> states;  //control flow

    private HashMap<String, Set<Transition>> stateToOutgoingTransitions;

    private Set<Transition> sendTransitions;
    private Set<Transition> receiveTransitions;
    private Set<Process> actions;
    private String currentState;
    private Condition receiveGuard;
    private Condition initialCondition;

    public Agent(String name) {
        this(name, new Store(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new String(), Condition.FALSE);

    }

    public Agent(String name, Store store, Set<String> states, Set<Transition> sendTransitions, Set<Transition> receiveTransitions, Set<Process> actions,
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

    public Agent(String name, Store store, Set<String> states, Set<Transition> sendTransitions, Set<Transition> receiveTransitions, Set<Process> actions,
                 Function<Pair<Store, HashMap<String, TypedVariable>>, Store> relabel, String currentState) {
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

    public Set<String> getStates() {
        return states;
    }

    public void setStates(Set<String> states) {
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
        Function<TypingContext, Parser> process = (TypingContext localContext) -> Process.parser(messageContext, localContext, communicationContext, channelContext);

        Function<TypingContext, Parser> relabeling = (TypingContext localContext) -> (Parsing.expressionParser(communicationContext).trim()
                .seq(StringParser.of("<-").trim())
                .seq(Parsing.expressionParser(localContext)))
                .flatten();

        Function<TypingContext, Parser> receiveGuard = (TypingContext localContext) -> (Condition.parser(localContext).trim()
                .seq(StringParser.of("->").trim())
                .seq(Parsing.expressionParser(channelContext)))
                .flatten();

        Parser name = word().plus().trim();

        AtomicReference<TypingContext> localContext = new AtomicReference<>();

        parser.set(StringParser.of("agent").trim()
                    .seq(name)
                    .seq(Parsing.labelledParser("local", Parsing.typedAssignmentList())
                            .map((Map values) -> {
                                localContext.set(new TypingContext(values));
                                return values;
                            }))
                    .seq(Parsing.labelledParser("relabel", relabeling.apply(localContext.get())))
                    .seq(Parsing.labelledParser("receive-guard", receiveGuard.apply(localContext.get())))
                    .seq(Parsing.labelledParser("repeat", process.apply(localContext.get())))
                .map((List<Object> values) -> {
                    //TODO
                    return null;
                }));

        return parser;
    }
}