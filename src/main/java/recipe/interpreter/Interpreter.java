package recipe.interpreter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import recipe.analysis.NuXmvInteraction;
import recipe.lang.agents.Agent;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.agents.State;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.ltol.Observation;
import recipe.lang.store.ConcreteStore;
import recipe.lang.types.Type;
import recipe.lang.utils.Pair;

public class Interpreter {
    private TypedVariable channTypedVariable;
    TypedVariable getChannelTV () {
        if (channTypedVariable == null) {
            try {
                Type chanEnum = recipe.lang.types.Enum.getEnum("channel");
                channTypedVariable = new TypedVariable<Type>(chanEnum, "channel");
            } catch (Exception e) {
                System.err.println(e);
                System.err.println(e.getStackTrace());
            }
        }
        return channTypedVariable;
    }

    private Step currentStep;
    recipe.lang.System sys;
    Map<State, Set<ProcessTransition>> sends;
    Map<State, Set<ProcessTransition>> receives;
    Map<State, Set<ProcessTransition>> gets;
    Map<State, Set<ProcessTransition>> supplys;

    public Interpreter (recipe.lang.System s) {
        sys = s;
        sends = new HashMap<State, Set<ProcessTransition>>();
        receives = new HashMap<State, Set<ProcessTransition>>();
        gets = new HashMap<State, Set<ProcessTransition>>();
        supplys = new HashMap<State, Set<ProcessTransition>>();

        // Set up send/receive tables
        sys.getAgents().forEach(a -> {
            a.getSendTransitions().forEach(send -> {
                sends.putIfAbsent(send.getSource(), new HashSet<ProcessTransition>());
                sends.get(send.getSource()).add(send);
            });
            a.getReceiveTransitions().forEach(receive -> {
                receives.putIfAbsent(receive.getSource(), new HashSet<ProcessTransition>());
                receives.get(receive.getSource()).add(receive);
            });
            a.getGetTransitions().forEach(get -> {
                gets.putIfAbsent(get.getSource(), new HashSet<ProcessTransition>());
                gets.get(get.getSource()).add(get);
            });
            a.getSupplyTransitions().forEach(sply -> {
                supplys.putIfAbsent(sply.getSource(), new HashSet<ProcessTransition>());
                supplys.get(sply.getSource()).add(sply);
            });
        });
    }

    public List<JSONObject> traceToJSON () {
        List<JSONObject> result = new LinkedList<>();
        Step myStep = currentStep;
        while(myStep != null) {
            result.add(0, myStep.toJSON());
            myStep = myStep.parent;
        }
        return result;
    }

    public static Interpreter ofJSON(recipe.lang.System s, Map<String,Observation> obsMap, JSONArray json) throws Exception {
        List<JSONObject> list = new ArrayList<>(json.length());
        for (int i = 0; i < json.length(); i++) {
            list.add(json.getJSONObject(i));
        }
        return Interpreter.ofJSON(s, obsMap, list);
    }

    public static Interpreter ofJSON(recipe.lang.System s, Map<String,Observation> obsMap, List<JSONObject> states) throws Exception {
        if (states.size() == 0) {
            throw new Exception("Empty JSON");
        }
        Interpreter interpreter = new Interpreter(s);
        JSONObject initState = states.get(0);

        // Compute set of variables in the system
        Set<String> varNames = new HashSet<>(s.getCommunicationVariables().keySet());
        for (Agent a : s.getAgents()) {
            varNames.addAll(a.getStore().getAttributes().keySet());
        }

        // Create constraint on initial state & find it
        List<String> constraints = new LinkedList<String>();

        for (String agentInstance : initState.keySet()) {
            if (agentInstance.startsWith("___")) continue;
            JSONObject agentState = initState.getJSONObject(agentInstance);
            for (String var : agentState.keySet()) {
                if (varNames.contains(var)) {
                    constraints.add(String.format("%s-%s = %s", agentInstance, var, agentState.get(var)));
                }
            }
        }
        String initConstraint = String.join(" & ", constraints);
        interpreter.init(initConstraint);
        // Walk the rest of the trace
        for (int i = 1; i < states.size(); i++) {
            JSONObject nextConstraint = states.get(i);
            JSONObject currConstraint = states.get(i-1);
            // We got to a state where progress = false, all the rest is irrelevant
            
            if (currConstraint.has("___STUCK___")) break;
            // If we are deadlocked, but no variable is changing in trace, then it's all right
            if (interpreter.isDeadlocked()) {
                boolean hasVars = false;
                JSONObject jo;
                if (nextConstraint instanceof CompositeJSON) jo = ((CompositeJSON) nextConstraint).getDiff();
                else jo = nextConstraint;
                OuterLoop:
                for (String key : jo.keySet()) {
                    if (key.startsWith("___")) continue;
                    // key is an agentInstance's name
                    if (initState.keySet().contains(key)) {
                        for (String x : jo.getJSONObject(key).keySet()) {
                            if (varNames.contains(x)) {
                                hasVars = true;
                                break OuterLoop;
                            }
                        }
                    }
                }
                if (!hasVars) break;
            }
            if (!interpreter.findNext(obsMap, nextConstraint, currConstraint)) {
                throw new Exception(String.format("[ofTrace] something wrong at step %d", i));
            }
        }
        return interpreter;
    }

    /**
     * Loads a nuXmv trace into a new intepreter and returns it
     *
     * @param s the r-check system
     * @param obsMap a map from string to LTOL observations
     * @param trace a nuXmv trace
     * @return an instance of Interpreter
     */
    public static Interpreter ofTrace(recipe.lang.System s, Map<String,Observation> obsMap, String trace) throws Exception {
        // Find states that are loop-starts
        Matcher m = Pattern.compile(
            "-- Loop starts here[\s\n]*-> State: 1.([0-9]+)",
            Pattern.DOTALL
        ).matcher(trace);
        Set<Integer> loopingStates = new HashSet<>();
        while (m.find()) {
            // We do -1 since nuXmv numbers states from 1
            loopingStates.add(Integer.valueOf(m.group(1))-1);
        }

        // Remove loop annotations
        trace = trace.replaceAll("-- Loop starts here\n", "");
        String sentinel = "Trace Type: Counterexample";
        int startPos = trace.indexOf(sentinel) + sentinel.length();
        trace = trace.substring(startPos);

        NuXmvInteraction nuxmv = new NuXmvInteraction(s);
        nuxmv.stopNuXmvThread(); // We ain't going to need it
        String[] split = trace.split("->", 0);
        List<JSONObject> states = new ArrayList<>(split.length - 1);
        for (String string : split) {
            // Skip stuff that does not contain a state (shouldn't happen)
            if (!string.contains("<-")) continue;

            JSONObject state = nuxmv.outputToJSON(string);
            if (loopingStates.contains(states.size())) {
                state.put("___LOOP___", true);
            }
            if (states.isEmpty()) { states.add(state); }
            else { states.add(new CompositeJSON(states.get(states.size()-1) , state)); }
        }
        return Interpreter.ofJSON(s, obsMap, states);
    }

    private void rootStep(String constraint) throws IOException, Exception {
        HashMap<AgentInstance, ConcreteStore> rootStores = new HashMap<AgentInstance, ConcreteStore>();
        NuXmvInteraction nuxmv = new NuXmvInteraction(sys);
        Pair<Boolean, String> s0 = nuxmv.simulation_pick_init_state(constraint);
        JSONObject initValues = nuxmv.outputToJSON(s0.getRight());
        nuxmv.stopNuXmvThread();

        sys.getAgentInstances().forEach((x) -> {
            String name = x.getLabel();
            if (!initValues.has(name)) {
                System.err.print(">>> ");
                System.err.println(initValues.toString(0));
            }
            JSONObject jObj = initValues.getJSONObject(name);
            ConcreteStore ist = new ConcreteStore(jObj, x);
            rootStores.put(x, ist);
        });
        this.currentStep = new Step(rootStores, null, this);
    }

    public void init(String constraint) throws IOException, Exception {
            assert sys != null;
            this.rootStep(constraint);
    }

    public boolean isDeadlocked() {
        return currentStep.transitions.size() == 0;
    }

    public boolean findNext(Map<String,Observation> obsMap, JSONObject nextConstraint, JSONObject currConstraint) {
        Step candidate;
        System.out.printf("(%d) constraint: %s, transitions: %s\n", currentStep.depth, nextConstraint, currentStep.transitions.size());
        for (int i = 0; i < currentStep.transitions.size(); i++) {
            if (!currentStep.transitions.get(i).satisfies(currConstraint)) continue;
            candidate = currentStep.next(i, this);
            if (candidate.satisfies(obsMap, nextConstraint)) {
                candidate.loadAnnotations(nextConstraint);
                currentStep = candidate;
                return true;
            }
        }
        return false;
    }

    public void next(int index) {
        if (isDeadlocked()) {
            // Deadlocked state, do nothing
            return;
        }
        currentStep = currentStep.next(index, this);
    }

    public void backtrack() {
        currentStep = currentStep.parent;
    }

    public void setSystem(recipe.lang.System s) {
        sys = s;
    }

    public Step getCurrentStep() {
        return currentStep;
    }
}
