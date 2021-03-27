package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.agents.Transition;

import java.util.List;
import java.util.Set;

public class Sequence extends Process{
    public Process a;
    public Process b;

    public Sequence(Process a, Process b) {
        this.a = a;
        this.b = b;
    }

    public Set<Transition> asTransitionSystem(String start, String end){
        String intermediate = stateSeed + "";
        stateSeed++;
        Set<Transition> ts = a.asTransitionSystem(start, intermediate);
        ts.addAll(this.b.asTransitionSystem(intermediate, end));

        return ts;
    }

    public static Parser parser(Parser processParser){
        Parser parser =
                processParser.seq(CharacterParser.of(';').trim()).seq(processParser)
                .map((List<Object> values) -> new Sequence((Process) values.get(0), (Process) values.get(2)));

        return parser;
    }
}
