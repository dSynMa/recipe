package recipe.lang.agents;

import recipe.lang.expressions.predicate.Condition;
import recipe.lang.process.Iterative;
import recipe.lang.process.Process;

public class IterationExitTransition extends Transition{
    private String agent;
    private State source;
    private State destination;
    private Condition condition;

    public IterationExitTransition(State source, State destination, Condition condition){
        this.source = source;
        this.destination = destination;
        this.condition = condition;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
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

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }
}
