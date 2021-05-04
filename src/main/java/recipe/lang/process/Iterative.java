package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.agents.IterationExitTransition;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.Not;
import recipe.lang.types.Boolean;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Iterative extends Process{
    public Process a;

    public Iterative(Process a) {
        //This prevents immediate nesting of iteration
        if(a.getClass().equals(Iterative.class)){
            this.a = ((Iterative) a).a;
        }else{
            this.a = a;
        }
    }

    public Set<Transition> asTransitionSystem(State start, State end){
        Set<Transition> ts = new HashSet<>();
        State intermediateStart = new State(getSeed());
        State intermediateEnd = new State(getSeed());

        Set<Transition> loop = a.asTransitionSystem(start, end);

        IterationExitTransition iterationExitTransition = new IterationExitTransition(start, end, new Not(a.entryCondition()));

        ts.addAll(loop);
        ts.add(iterationExitTransition);
        return ts;
    }

    public Expression<Boolean> entryCondition(){
        return a.entryCondition();
    }

    public void addEntryCondition(Expression<Boolean> condition){
        a.addEntryCondition(condition);
    }

    public static Parser parser(Parser processParser){
        Parser parser =
                (StringParser.of("do").trim()).seq(processParser).seq(StringParser.of("od").trim())
                        .map((List<Object> values) -> new Iterative((Process) values.get(1)));

        return parser;
    }
}
