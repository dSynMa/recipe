package recipe.analysis;

import recipe.lang.agents.*;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.CompositionException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.process.BasicProcess;
import recipe.lang.process.Process;
import recipe.lang.process.SendProcess;
import recipe.lang.store.Store;
import recipe.lang.utils.Pair;

import java.util.*;
import java.util.function.Function;

public class Composition {
    Store store;
    Set<Transition<Pair<Condition, Map<String, Expression>>>> transitions;

    public Composition(List<Agent> agents) throws AttributeTypeException, AttributeNotInStoreException, CompositionException, RelabellingTypeException {
        if(agents.size() == 0) throw new CompositionException("Empty list of agents for composition.");

        store = new Store();
        transitions = new HashSet<>();

        String[] names = new String[agents.size()];
        Store store = new Store();
        Set<State> states = new HashSet<>();
        State[] initialStates = new State[agents.size()];
        Map<State, Set<ProcessTransition>> stateSendTransitionMap = new HashMap<>();
        Map<State, Set<ProcessTransition>> stateReceiveTransitionMap = new HashMap<>();
        Map<State, Set<IterationExitTransition>> stateIterationExitTransitionMap = new HashMap<>();

        for(int i = 0; i < agents.size(); i++){
            Agent agent = agents.get(i);
            if(Arrays.asList(names).contains(agent.getName())){
                throw new CompositionException("Multiple agents with name: " + agent.getName() + ".");
            }
            names[i] = agent.getName();
            store.update(agent.getStore().copyWithRenaming(s -> agent.getName() + "." + s));
            states.addAll(agent.getStates());
            initialStates[i] = agent.getInitialState();
            stateSendTransitionMap.putAll(agent.getStateTransitionMap(agent.getSendTransitions()));
            stateReceiveTransitionMap.putAll(agent.getStateTransitionMap(agent.getReceiveTransitions()));
            stateIterationExitTransitionMap.putAll(agent.getStateTransitionMap(agent.getIterationExitTransitions()));
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
                    Agent agent1 = agents.get(i);
                    State state1 = individualStates[i];

                    for (int j = 0; j < individualStates.length; j++) {
                        Agent agent2 = agents.get(j);
                        State state2 = individualStates[j];

                        for(IterationExitTransition iterationExitTransition : stateIterationExitTransitionMap.get(state2)){
                            State[] next = individualStates.clone();
                            next[j] = iterationExitTransition.getDestination();
                            State nextState = new State(name, next);
                            nextStates.add(nextState);

                            iterationExitTransitions.add(new IterationExitTransition(state, nextState, iterationExitTransition.getLabel()));
                        }

                        //Ensure this is below above loop, since the loop must be executed for state1 too.
                        if (state1 == state2) continue;

                        for (ProcessTransition t1 : stateSendTransitionMap.get(state1)) {
                            BasicProcess oldProcess1 = t1.getLabel();
                            State[] next = individualStates.clone();
                            next[i] = t1.getDestination();
                            List<ProcessTransition> newSendTransitions;
                            Condition newGuard1 = oldProcess1.entryCondition().relabel(v -> v.sameTypeWithName(agent1.getName() + "." + v));

                            for (ProcessTransition t2 : stateReceiveTransitionMap.get(state2)) {
                                //if local guard of receive transition true
                                //and (if receive-guard conjuncted with 'channel == receiveTransition.channel') is true
                                //         or channel is *)
                                //and the message guard is true on agent 2
                                // then combine transitions

                                BasicProcess oldProcess2 = t2.getLabel();

                                //Check if both transitions use channel values directly, and if they are not the same then
                                if(oldProcess1.getChannel().getClass().equals(ChannelValue.class)){
                                    if(oldProcess2.getChannel().getClass().equals(ChannelValue.class)) {
                                        //TODO @Yehia is below correct?
                                        if(!oldProcess1.getChannel().equals(oldProcess2.getChannel())
                                                && !oldProcess1.getChannel().equals(new ChannelValue("*"))
                                                && !oldProcess2.getChannel().equals(new ChannelValue("*"))){
                                            continue;
                                        }
                                    }
                                }

                                next[j] = t2.getDestination();


                                Condition newGuard =
                                        new And(newGuard1,
                                                oldProcess2.entryCondition().relabel(v -> v.sameTypeWithName(agent2.getName() + "." + v)));

                                //relabelling associates a communication variable to an expression
                                //relabel expression such that v -> name[j] + "." + v
                                //relabel guard such that v -> v is a cv ? relabelled-expr : name[i] + "." + v
                                Function<TypedVariable, Expression> messageGuardRelabelling =
                                        (v -> {
                                            //if v is a communicating variable
                                            if(agent1.getRelabel().containsKey(v)){
                                                Expression expr = agent1.getRelabel().get(v);
                                                try {
                                                    expr.relabel((vv -> {
                                                        return vv.sameTypeWithName(agent2.getName() + "." + vv.getName());
                                                    }));
                                                } catch (RelabellingTypeException e) {
                                                    e.printStackTrace();
                                                }
                                                //TODO should never reach here
                                                return null;
                                            } else{
                                                return v.sameTypeWithName(agent1.getName() + "." + v.getName());
                                            }
                                        });
                                ChannelExpression newChannel = oldProcess1.getChannel();
                                Condition newMessageGuard = ((SendProcess) oldProcess1).getMessageGuard().relabel(messageGuardRelabelling);
                                Map newMessage = new HashMap();
                                Map newUpdate = new HashMap();
                                for(Map.Entry<String, Expression> val : oldProcess1.getUpdate().entrySet()){
                                    newUpdate.put(agent1.getName() + "." + val.getKey(),
                                            val.getValue().relabel(v -> v.sameTypeWithName(agent1.getName() + "." + v)));
                                }

                                Map<String, Expression> oldMessage = ((SendProcess) t1.getLabel()).message;
                                for(Map.Entry<String, Expression> val : oldProcess2.getUpdate().entrySet()){
                                    newUpdate.put(agent2.getName() + "." + val.getKey(),
                                            val.getValue().relabel(v -> {
                                                if(oldMessage.containsKey(v)){
                                                    try {
                                                        Expression toReplaceWith = oldMessage.get(v).relabel(vv -> vv.sameTypeWithName(agent1.getName() + "." + vv));
                                                        return toReplaceWith;
                                                    } catch (RelabellingTypeException e) {
                                                        e.printStackTrace();
                                                    }
                                                    //TODO should never reach here
                                                    return null;
                                                } else{
                                                    return v.sameTypeWithName(agent2.getName() + "." + v);
                                                }
                                            }));
                                }

                                //three possible transitions:
                                //send and not listen
                                //send and listen and have a receive transition and satisfy the send predicate
                                //send and not satisfy the send predicate when channel is broadcast
                            }


                            State nextState = new State(name, next);
                            nextStates.add(nextState);
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
    }

    public String toNuXMV(){
        String nuXmv = "";

        return nuXmv;
    }
}
