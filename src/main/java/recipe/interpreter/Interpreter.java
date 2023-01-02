package recipe.interpreter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import recipe.analysis.NuXmvInteraction;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.agents.State;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.process.SendProcess;
import recipe.lang.store.Store;
import recipe.lang.utils.Pair;
import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.NotImplementedYetException;

public class Interpreter {

    private class Transition {
        private AgentInstance sender;
        private ProcessTransition send;
        private Map<AgentInstance, ProcessTransition> receivers;
    }

    


    public class Step {
        private Map<AgentInstance, InstanceStore> stores;
        private Map<Expression, Set<AgentInstance>> listeners;
        private Set<Transition> transitions;
        private Transition chosenTransition;
        private Step parent;

        private State<Integer> makeState (AgentInstance inst, Map<AgentInstance, Map<String, String>> eval) {
            return new State<Integer>(
                inst.getAgent().getName(),
                Integer.valueOf(eval.get(inst).get("state"))
            );
        }


        public Step(Map<AgentInstance, InstanceStore> stores, Step parent, Interpreter i) {
            this.parent = parent;
            this.stores = stores;
     
            // TODO compute listeners on every channel

            // TODO Compute available transitions
            i.sys.getAgentInstances().forEach(inst -> {
                Map<Expression, Set<ProcessTransition>> instSends = i.sends.get(stores.get(inst).getState());

                if (instSends != null) {
                    instSends.forEach((chan, sends) -> {
                        sends.forEach(tr -> {
                            SendProcess lbl = (SendProcess) tr.getLabel();
                            Expression<recipe.lang.types.Boolean> psi = lbl.getPsi();
                            try {
                                boolean psiSat = psi.valueIn(stores.get(inst)).equals(Condition.getTrue());
                                System.out.println(lbl.getMessageGuard());
                                System.out.println(lbl.getMessage());

                                // If psisat, check for all possible receivers
                                // (grow their Store with the message to allow evaluating the psi)

                            } catch (Exception e) {
                                // TODO
                                System.out.println(e);
                            }
                        });
                    });    
                }
            });
            
            // 1. Filter out sends where psi does not hold
            // 2. For each MC send over c, check that all listeners on c
            //    can do a matching receive
            // 3. Populate the transitions set


        }
        
    }

    public Step initialStep;
    private recipe.lang.System sys;


    // State -> Channel -> 2^ProcessTransition
    private Map<State<Integer>, Map<Expression, Set<ProcessTransition>>> sends;
    private Map<State<Integer>, Map<Expression, Set<ProcessTransition>>> receives;
    private Map<String, Map<Integer, State<Integer>>> states;

    public Interpreter (recipe.lang.System s) {
        sys = s;
        sends = new HashMap<State<Integer>, Map<Expression, Set<ProcessTransition>>>();
        receives = new HashMap<State<Integer>, Map<Expression, Set<ProcessTransition>>>();

        // Set up send/receive tables
        sys.getAgents().forEach(a -> {
            a.getSendTransitions().forEach(send -> {
                sends.putIfAbsent(send.getSource(), new HashMap<Expression, Set<ProcessTransition>>());
                sends.get(send.getSource()).putIfAbsent(send.getLabel().getChannel(), new HashSet<>());
                sends.get(send.getSource()).get(send.getLabel().getChannel()).add(send);
            });
            a.getReceiveTransitions().forEach(receive -> {
                receives.putIfAbsent(receive.getSource(), new HashMap<Expression, Set<ProcessTransition>>());
                receives.get(receive.getSource()).putIfAbsent(receive.getLabel().getChannel(), new HashSet<>());
                receives.get(receive.getSource()).get(receive.getLabel().getChannel()).add(receive);
            });
        });

        System.out.println(sends);
    }


    private void rootStep(String constraint) throws IOException, Exception {
        HashMap<AgentInstance, InstanceStore> rootStores = new HashMap<AgentInstance, InstanceStore>();
        NuXmvInteraction nuxmv = new NuXmvInteraction(sys);
        Pair<Boolean, String> s0 = nuxmv.simulation_pick_init_state(constraint);
        JSONObject initValues = nuxmv.outputToJSON(s0.getRight());

        sys.getAgentInstances().forEach((x) -> {
            String name = x.getLabel();
            JSONObject jObj = initValues.getJSONObject(name);
            InstanceStore ist = new InstanceStore(jObj, x.getAgent());
            System.out.println(ist);
            rootStores.put(x, ist);
        });
        System.out.println(rootStores);
        this.initialStep = new Step(rootStores, null, this);
    }

    public void init (String constraint) throws IOException, Exception {
            assert sys != null;
            this.rootStep(constraint);
            // NuXmvInteraction nuxmv = new NuXmvInteraction(sys);
            // Pair<Boolean, String> s0 = nuxmv.simulation_pick_init_state(constraint);       
    }



    public void setSystem(recipe.lang.System s) {
        sys = s;
    }
}
