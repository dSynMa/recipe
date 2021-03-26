package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;

import java.util.List;

public class Choice extends Process{
    public Process a;
    public Process b;

    public Choice(Process a, Process b) {
        this.a = a;
        this.b = b;
    }

    public static Parser parser(Parser processParser){
        Parser parser =
                processParser.seq(CharacterParser.of('+').trim()).seq(processParser)
                        .map((List<Object> values) -> new Sequence((Process) values.get(0), (Process) values.get(2)));

        return parser;
    }
}