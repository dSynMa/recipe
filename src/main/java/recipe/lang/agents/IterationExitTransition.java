package recipe.lang.agents;

import recipe.lang.expressions.predicate.Condition;
import recipe.lang.process.Iterative;
import recipe.lang.process.Process;

public class IterationExitTransition extends Transition<Condition>{
    public IterationExitTransition(State source, State destination, Condition condition){
        super(source, destination, condition);
    }
}
