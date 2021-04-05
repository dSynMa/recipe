package recipe.lang.agents;

import recipe.lang.process.BasicProcess;
import recipe.lang.process.Process;

import java.util.HashSet;
import java.util.Set;

public class ProcessTransition extends Transition{
    // three attributes of transition: source,action,destination
    private String agent;
    private BasicProcess action;

    public ProcessTransition(State source, State destination, BasicProcess action) {
        super(source, destination);
        this.action = action;
    }

    public ProcessTransition(String agent, State source, State destination, BasicProcess action) {
        super(source, destination);
        this.agent=agent;
        this.action = action;
    }


    public BasicProcess getAction() {
        return action;
    }

    public void setAction(BasicProcess action) {
        this.action = action;
    }

    // rewrite toString function
    public String toString() {
        return this.getSource() + "        -" + this.getAction() + "->       " + this.getDestination();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + ((destination == null) ? 0 : destination.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProcessTransition other = (ProcessTransition) obj;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equals(other.action))
            return false;
        if (destination == null) {
            if (other.destination != null)
                return false;
        } else if (!destination.equals(other.destination))
            return false;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        return true;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public static Set<ProcessTransition> getTransitionsFrom(Set<ProcessTransition> transitions, State state){
        Set<ProcessTransition> fromState = new HashSet<>();
        for(ProcessTransition t : transitions){
            if(t.getSource().equals(state)){
                fromState.add(t);
            }
        }

        return fromState;
    }

    public static Set<ProcessTransition> copyAndChangeSourceTo(Set<ProcessTransition> transitions, State state){
        Set<ProcessTransition> fromState = new HashSet<>();
        for(ProcessTransition t : transitions){
            new ProcessTransition(t.agent, state, t.getDestination(), t.getAction());
        }

        return fromState;
    }

    public static Set<ProcessTransition> getTransitionsTo(Set<ProcessTransition> transitions, State state){
        Set<ProcessTransition> fromState = new HashSet<>();
        for(ProcessTransition t : transitions){
            if(t.getDestination().equals(state)){
                fromState.add(t);
            }
        }

        return fromState;
    }

    public static Set<ProcessTransition> copyAndChangeDestinationTo(Set<ProcessTransition> transitions, State state){
        Set<ProcessTransition> fromState = new HashSet<>();
        for(ProcessTransition t : transitions){
            new ProcessTransition(t.agent, t.getSource(), state, t.getAction());
        }

        return fromState;
    }
}
