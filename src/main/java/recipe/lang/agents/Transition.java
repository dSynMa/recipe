package recipe.lang.agents;

public abstract class Transition {
    protected State source;
    protected State destination;

    public Transition(State source, State destination){
        this.source = source;
        this.destination = destination;
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
}
