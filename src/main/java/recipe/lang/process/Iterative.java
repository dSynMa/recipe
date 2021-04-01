package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;

import java.util.List;
import java.util.Set;

public class Iterative extends Process{
    public Process a;

    public Iterative(Process a) {
        this.a = a;
    }

    public Set<Transition> asTransitionSystem(State start, State end){
        stateSeed++;
        Set<Transition> ts = a.asTransitionSystem(start, end);

        return ts;
    }

    public static Parser parser(Parser processParser){
        Parser parser =
                (StringParser.of("do").trim()).seq(processParser).seq(StringParser.of("od").trim())
                        .map((List<Object> values) -> new Iterative((Process) values.get(1)));

        return parser;
    }
}
