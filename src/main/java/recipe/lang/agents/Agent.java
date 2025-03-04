package recipe.lang.agents;

import static org.petitparser.parser.primitive.CharacterParser.word;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;
import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.FailureParser;
import org.petitparser.parser.primitive.StringParser;

import recipe.Config;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.GuardReference;
import recipe.lang.process.BasicProcess;
import recipe.lang.process.GetProcess;
import recipe.lang.process.Process;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;
import recipe.lang.process.SupplyProcess;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Type;
import recipe.lang.utils.Deserialization;
import recipe.lang.utils.LazyParser;
import recipe.lang.utils.Pair;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;

public class Agent {
    private String name;
    private Store store;
    private Expression<Boolean> init;
    private HashMap<String, TypedVariable> CV;
    private Map<TypedVariable, Expression> relabel;
    private Set<State> states; // control flow
    private Set<ProcessTransition> sendTransitions;
    private Set<ProcessTransition> receiveTransitions;
    private Set<ProcessTransition> getTransitions;
    private Set<ProcessTransition> supplyTransitions;
    private Set<Process> actions;
    private State initialState;
    private Expression<Boolean> receiveGuard;
    private Expression<Boolean> initialCondition;

    public Agent(String name) throws MismatchingTypeException {
        this(name, new Store(), Condition.getTrue(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(),
                new State(name, new String()), new TypedValue<Boolean>(Boolean.getType(), "false"), new HashSet<>(),
                new HashSet<>());
    }

    public Agent(String name,
            Store locals,
            Expression<Boolean> init,
            Set<State> states,
            Set<ProcessTransition> sendTransitions,
            Set<ProcessTransition> receiveTransitions,
            Set<Process> actions,
            State initialState,
            Expression<Boolean> receiveGuard,
            Set<ProcessTransition> getTransitions,
            Set<ProcessTransition> supplyTransitions) {
        this.name = name.trim();
        this.store = locals;
        this.init = init;
        this.states = new HashSet<>(states);
        this.sendTransitions = new HashSet<>(sendTransitions);
        this.receiveTransitions = new HashSet<>(receiveTransitions);
        this.actions = new HashSet<>(actions);
        this.initialState = initialState;
        this.receiveGuard = receiveGuard;
        this.getTransitions = new HashSet<>(getTransitions);
        this.supplyTransitions = new HashSet<>(supplyTransitions);
    }

    public Agent(String name,
            Store store,
            Expression<Boolean> init,
            Set<State> states,
            Set<ProcessTransition> sendTransitions,
            Set<ProcessTransition> receiveTransitions,
            Set<ProcessTransition> getTransitions,
            Set<ProcessTransition> supplyTransitions,
            Set<Process> actions,
            Map<TypedVariable, Expression> relabel,
            State initialState) {
        this.name = name.trim();
        this.store = store;
        this.init = init;
        this.states = new HashSet<>(states);
        this.sendTransitions = new HashSet<>(sendTransitions);
        this.receiveTransitions = new HashSet<>(receiveTransitions);
        this.getTransitions = new HashSet<>(getTransitions);
        this.supplyTransitions = new HashSet<>(supplyTransitions);
        this.actions = new HashSet<>(actions);
        this.relabel = relabel;
        this.initialState = initialState;
    }

    public static Agent ofRepeat(
            String name, Store store, Expression<Boolean> init, Map<TypedVariable, Expression> relabel,
            Expression<Boolean> receiveGuardCondition, Process repeat) {

        State startState = new State<Integer>(Integer.valueOf(0));
        // State startState = new State("0");
        Process.stateSeed = 0;
        Set<Transition> transitions = repeat.asTransitionSystem(startState, startState);
        Set<ProcessTransition> sendTransitions = new HashSet<>();
        Set<ProcessTransition> receiveTransitions = new HashSet<>();
        Set<ProcessTransition> getTransitions = new HashSet<>();
        Set<ProcessTransition> supplyTransitions = new HashSet<>();

        Set<Process> actions = new HashSet<>();
        transitions.forEach(tt -> {
            if (tt.getClass().equals(ProcessTransition.class)) {
                ProcessTransition t = (ProcessTransition) tt;
                if (t.getLabel().getClass().equals(SendProcess.class)) {
                    sendTransitions.add(t);
                } else if (t.getLabel().getClass().equals(ReceiveProcess.class)) {
                    receiveTransitions.add(t);
                } else if (t.getLabel().getClass().equals(GetProcess.class)) {
                    getTransitions.add(t);
                } else if (t.getLabel().getClass().equals(SupplyProcess.class)) {
                    supplyTransitions.add(t);
                }

                actions.add(t.getLabel());
            }
        });
        Set<State> states = new HashSet<>();
        for (Transition tt : transitions) {
            if (tt.getClass().equals(ProcessTransition.class)) {
                ProcessTransition t = (ProcessTransition) tt;
                t.getSource().setAgent(name);
                t.getDestination().setAgent(name);
                t.setAgent(name);
                states.add(t.getSource());
                states.add(t.getDestination());
            }
        }
        Agent agent = new Agent(name,
                store,
                init,
                states,
                sendTransitions,
                receiveTransitions,
                actions,
                startState,
                receiveGuardCondition,
                getTransitions,
                supplyTransitions);

        agent.setRelabel(relabel);
        return agent;
    }

    public String getName() {
        return this.name;
    }

    public Store getStore() {
        return store;
    }

    public Expression<Boolean> getInit() {
        return init;
    }

    protected void setInit(Expression<Boolean> init) {
        this.init = init;
    }

    @Override
    public String toString() {
        return name + ":" + init.toString();
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

    public Set<ProcessTransition> getGetTransitions() {
        return getTransitions;
    }

    public Set<ProcessTransition> getSupplyTransitions() {
        return supplyTransitions;
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

    public Expression<Boolean> getInitialCondition() {
        return initialCondition;
    }

    public void setInitialCondition(Condition initialCondition) {
        this.initialCondition = initialCondition;
    }

    public static org.petitparser.parser.Parser parser(TypingContext messageContext,
            TypingContext communicationContext,
            TypingContext guardDefinitionContext,
            recipe.lang.types.Enum locationEnum) throws Exception {
        SettableParser parser = SettableParser.undefined();
        Function<TypingContext, Parser> process = (TypingContext localContext) -> {
            try {
                return Parsing.labelledParser("repeat",
                        Process.parser(messageContext, localContext, communicationContext));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };

        AtomicReference<String> nameString = new AtomicReference<>("");

        Parser name = word().plus().trim().flatten().map((String val) -> {
            nameString.set("agent " + val.trim());
            return val;
        });

        AtomicReference<String> error = new AtomicReference<>("");
        AtomicReference<TypingContext> localContext = new AtomicReference<>(new TypingContext());

        parser.set((StringParser.of("agent").trim().or(FailureParser.withMessage("Could not parse agent definition.")))
                .seq(name.or(FailureParser.withMessage("Could not parse agent name.")))
                .seq(Parsing.labelledParser("local", Parsing.typedVariableList().optional(new ArrayList<>()))
                        .mapWithSideEffects((List<TypedVariable> values) -> {
                            Map<String, TypedVariable> vars = new HashMap<>();
                            try {
                                values.add(new TypedVariable(locationEnum, Config.myselfKeyword));
                                localContext.get().clear();
                                localContext.get().setAll(new TypingContext(values));
                                for (TypedVariable var : values) {
                                    vars.put(var.getName(), new TypedVariable(var.getType(), var.getName()));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return vars;
                        }).optional(new HashMap<>())) // .or(LazyParser.failingParser(nameString, "Could not parse
                                                      // agent's local definition."))))
                .seq(Parsing.labelledParser("init", new LazyParser<TypingContext>((TypingContext context) -> {
                    try {
                        return Condition.parser(context).map((Object v) -> {
                            return v;
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }, localContext.get())).or(StringParser.of("init").not().map((Object val) -> Condition.getTrue()))
                        .or(LazyParser.failingParser(nameString, "Could not parse agent's init definition.")))
                .seq(new LazyParser<TypingContext>(((TypingContext localContext1) -> {
                    try {
                        return Parsing.relabellingParser(localContext1, communicationContext);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }), localContext.get()).trim().or(StringParser.of("relabel").not().map((Object val) -> new HashMap()))
                        .or(LazyParser.failingParser(nameString, "Could not parse agent's relabel definition.")))
                .seq(new LazyParser<TypingContext>(((TypingContext context) -> {
                    try {
                        return Parsing.receiveGuardParser(context);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }), localContext.get())
                        .or(LazyParser.failingParser(nameString, "Could not parse agent's receive-guard definition.")))
                .seq(new LazyParser<Pair<TypingContext, TypingContext>>(
                        (Pair<TypingContext, TypingContext> guardAndLocalContext) -> {
                            return process.apply(TypingContext.union(guardAndLocalContext.getLeft(),
                                    guardAndLocalContext.getRight()));
                        }, new Pair(guardDefinitionContext, localContext.get())).trim()
                        .or(LazyParser.failingParser(nameString, "Could not parse agent's process definition.")))
                .map((List<Object> values) -> {
                    String agentName = ((String) values.get(1)).trim();
                    Map<String, TypedVariable> localVars = (Map<String, TypedVariable>) values.get(2);
                    Store store = null;
                    try {
                        store = new Store(localVars);
                        Expression<Boolean> init = null;
                        init = (Expression<Boolean>) values.get(3);

                        Map<TypedVariable, Expression> relabel = (Map<TypedVariable, Expression>) values.get(4);
                        Expression<Boolean> receiveGuardCondition = (Expression<Boolean>) values.get(5);
                        Process repeat = (Process) values.get(6);
                        return Agent.ofRepeat(agentName, store, init, relabel, receiveGuardCondition, repeat);
                    } catch (AttributeTypeException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
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

        for (Transition t : transitions) {
            Set target;

            if (stateTransitionMap.containsKey(t.getSource())) {
                target = stateTransitionMap.get(t.getSource());
            } else {
                target = new HashSet();
            }

            target.add(t);

            stateTransitionMap.put(t.getSource(), target);
        }

        return stateTransitionMap;
    }

    public String toDOT() {
        GuardReference.resolve = false;
        String dot = "";
        dot += "\tgraph [rankdir=TB,ranksep=0.2,nodesep=0.4];\n"
                + "node [shape=circle];\n";
        for (State state : this.states) {
            if (state.equals(this.initialState)) {
                dot += "\t" + state.toString() + "[peripheries=2];\n";
            } else {
                dot += "\t" + state.toString() + ";\n";
            }
        }

        for (Transition t : this.sendTransitions) {
            String sourceLabel = t.getSource().toString();
            String destLabel = t.getDestination().toString();

            BasicProcess label = (BasicProcess) t.getLabel();
            String textLabel = label.getLabel();
            if (textLabel == null || textLabel.equals("")) {
                textLabel = label.prettyPrintLabel();
            }

            dot += "\t" + sourceLabel + " -> " + destLabel + "[label=\"" + textLabel + "\",labeltooltip=\"" + label
                    + "\",width=1]" + ";\n";
        }

        for (Transition t : this.receiveTransitions) {
            String sourceLabel = t.getSource().toString();
            String destLabel = t.getDestination().toString();

            BasicProcess label = (BasicProcess) t.getLabel();
            String textLabel = label.getLabel();
            if (textLabel == null || textLabel.equals("")) {
                textLabel = label.getChannel().toString();
            }
            textLabel += "?";

            dot += "\t" + sourceLabel + " -> " + destLabel + "[label=\"" + textLabel + "\",labeltooltip=\"" + label
                    + "\",width=1]" + ";\n";
        }

        for (Transition t : this.getTransitions) {
            String sourceLabel = t.getSource().toString();
            String destLabel = t.getDestination().toString();

            BasicProcess label = (BasicProcess) t.getLabel();
            String textLabel = label.prettyPrintLabel();
            if (textLabel == null || textLabel.equals("")) {
                textLabel = "[GET]";
            }
            dot += "\t" + sourceLabel + " -> " + destLabel + "[label=\"" + textLabel + "\",labeltooltip=\"" + label
                    + "\",width=1]" + ";\n";
        }

        for (Transition t : this.supplyTransitions) {
            String sourceLabel = t.getSource().toString();
            String destLabel = t.getDestination().toString();

            BasicProcess label = (BasicProcess) t.getLabel();
            String textLabel = label.getLabel();
            if (textLabel == null || textLabel.equals("")) {
                textLabel = "[SUPPLY]";
            }
            dot += "\t" + sourceLabel + " -> " + destLabel + "[label=\"" + textLabel + "\",labeltooltip=\"" + label
                    + "\",width=1]" + ";\n";
        }

        return dot;
    }

    public void labelAllTransitions() {
        HashSet<ProcessTransition> transitions = new HashSet(this.receiveTransitions);
        transitions.addAll(this.sendTransitions);
        // Map<State, Set<ProcessTransition>> stateTransitionMap =
        // getStateTransitionMap(transitions);
        // int seed = 0;

        // for(State state : stateTransitionMap.keySet()){

        // }
    }

    public List<TypedVariable<Boolean>> getAllTransitionLabels() {
        int sz = (
            this.sendTransitions.size() + this.receiveTransitions.size() +
            this.supplyTransitions.size() + this.getTransitions.size());
        List<ProcessTransition> allTransitions = new ArrayList<>(sz);
        allTransitions.addAll(sendTransitions);
        allTransitions.addAll(receiveTransitions);
        allTransitions.addAll(supplyTransitions);
        allTransitions.addAll(getTransitions);
        List<TypedVariable<Boolean>> labels = new ArrayList<>(sz);
        for (ProcessTransition process : allTransitions) {
            String label = process.getLabel().getLabel();
            if (label != null && !label.equals("")) {
                labels.add(new TypedVariable<Boolean>(Boolean.getType(), label));
            }
        }
        return labels;
    }

    public boolean isSymbolic() {
        for (TypedVariable tv : store.getAttributes().values()) {
            if (tv.getType().getClass().equals(recipe.lang.types.Integer.class)
                    || tv.getType().getClass().equals(recipe.lang.types.Real.class)) {
                return true;
            }
        }

        return false;
    }

    public static Agent deserialize(JSONObject jAgent, TypingContext parentContext) throws Exception {
        Deserialization.checkType(jAgent, "Agent");
        String name = jAgent.getString("name");
        Map<String, TypedVariable> localVars = new HashMap<>();
        TypingContext context = new TypingContext();
        if (parentContext != null) {
            context.setAll(parentContext);
        }

        // Add myself as an (immutable) local var
        Type locationEnum = recipe.lang.types.Enum.getEnum(Config.locationLabel);
        localVars.put(Config.myselfKeyword, new TypedVariable<Type>(locationEnum, Config.myselfKeyword));
        context.set(Config.myselfKeyword, locationEnum);

        if (jAgent.has("locals")) {
            JSONArray jLocals = jAgent.getJSONArray("locals");
            for (int i = 0; i < jLocals.length(); i++) {
                JSONObject local = jLocals.getJSONObject(i);
                String locName = local.getString("name");
                Type type = Deserialization.deserializeType(local);
                context.set(locName, type);
                localVars.put(locName, new TypedVariable<Type>(type, locName));
            }
        }
        Map<TypedVariable, Expression> relabel = new HashMap<>();
        if (jAgent.has("relabels")) {
            JSONArray jRelabels = jAgent.getJSONArray("relabels");
            for (int i = 0; i < jRelabels.length(); i++) {
                JSONObject jRelabel = jRelabels.getJSONObject(i);
                String varName = jRelabel.getJSONObject("var").getString("$refText");
                Type t = context.get(varName);
                Expression expr = Deserialization.deserializeExpr(jRelabel, context);
                relabel.put(new TypedVariable<Type>(t, varName), expr);
            }
        }
        Store store = new Store(localVars);
        Expression init = Deserialization.deserializeExpr(jAgent.getJSONObject("init"), context);
        Expression recvGuard = Condition.getTrue();
        if (jAgent.has("recvguard")) {    
            recvGuard = Deserialization.deserializeExpr(jAgent.getJSONObject("recvguard"), context);
        }
        Process repeat = Process.deserialize(jAgent.getJSONObject("repeat"), context);

        return Agent.ofRepeat(name, store, init, relabel, recvGuard, repeat);
    }
}