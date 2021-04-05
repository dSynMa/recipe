package recipe.lang.agents;

import recipe.lang.expressions.predicate.Condition;
import recipe.lang.process.Iterative;
import recipe.lang.process.Process;

public class IterationExitTransition extends Transition{
    private String agent;
    private Condition condition;

    public IterationExitTransition(State source, State destination, Condition condition){
        super(source, destination);
        this.condition = condition;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public Condition getCondition() {
        return condition;
    }

}
