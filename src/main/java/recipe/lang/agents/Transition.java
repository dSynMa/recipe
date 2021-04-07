package recipe.lang.agents;

import java.util.HashSet;
import java.util.Set;

public abstract class Transition<T> {
    protected State source;
    protected T label;
    protected State destination;
    protected String agent;

    public Transition(State source, State destination, T label){
        this.source = source;
        this.destination = destination;
        this.label = label;
    }

    public Transition(State source, State destination, T label, String agent){
        this.source = source;
        this.destination = destination;
        this.label = label;
        this.agent = agent;
    }


    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public T getLabel() {
        return label;
    }

    public void setLabel(T label) {
        this.label = label;
    }

    public State getSource() {
        return source;
    }

    public void setSource(State source) {
        this.source = source;
    }

    public State getDestination() {
        return destination;
    }

    public void setDestination(State destination) {
        this.destination = destination;
    }

    @Override
    public String toString() {
        return this.getSource() + "        -" + this.getLabel() + "->       " + this.getDestination();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
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
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
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
            new ProcessTransition(t.agent, state, t.getDestination(), t.getLabel());
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
            new ProcessTransition(t.agent, t.getSource(), state, t.getLabel());
        }

        return fromState;
    }
}
