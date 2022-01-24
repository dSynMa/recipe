package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.Or;
import recipe.lang.types.Boolean;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Sequence extends Process{
    public Process a;
    public Process b;

    public Sequence(Process a, Process b) {
        this.a = a;
        this.b = b;
    }

    public Expression<Boolean> entryCondition(){
        return a.entryCondition();
    }

    public void addEntryCondition(Expression<Boolean> condition) {
        a.addEntryCondition(condition);
    }

    public Set<Transition> asTransitionSystem(State start, State end){
        State intermediate = new State(getSeed());
        Set<Transition> ts = a.asTransitionSystem(start, intermediate);
        ts.addAll(this.b.asTransitionSystem(intermediate, end));

        return ts;
    }
}
