package recipe.lang.process;

import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.Parser;
import recipe.lang.expressions.predicate.Condition;

import java.util.List;

public class ProcessParser extends Parser {

//    public ProcessParser(){
//        ConditionParser conditionParser = new ConditionParser();
//        setParser(createParser(conditionParser, new BasicProcessParser(conditionParser)));
//    }
//
//    public ProcessParser(ConditionParser conditionParser, BasicProcessParser basicProcessParser){
//        setParser(createParser(conditionParser, basicProcessParser));
//    }
//
//    private static org.petitparser.parser.Parser createParser(ConditionParser conditionParser, BasicProcessParser basicProcessParser){
//        SettableParser parser = SettableParser.undefined();
//        SettableParser basic = SettableParser.undefined();
//        org.petitparser.parser.Parser condition = conditionParser.getParser();
//        org.petitparser.parser.Parser action = basicProcessParser.getParser();
//
//        basic.set((CharacterParser.of('(').trim()).seq(action).seq((CharacterParser.of(')').trim()))
//                .map((List<Object> values) -> {
//                    return (SendProcess) values.get(0);
//                }));
//
//        parser.set((basic.seq(StringParser.of("+").trim()).seq(parser))
//                .map((List<Object> values) -> {
//                    return new Choice((Process) values.get(0), (Process) values.get(2));
//                })
//                .or(basic.seq(StringParser.of(";").trim()).seq(parser)
//                        .map((List<Object> values) -> {
//                            return new Sequence((Process) values.get(0), (Process) values.get(2));
//                        }))
//                .or((StringParser.of("<").trim().seq(condition).seq(StringParser.of(">").trim())).seq(parser)
//                        .map((List<Object> values) -> {
//                            return new Guarded((Condition) values.get(1), (Process) values.get(3));
//                        }))
//                .or(basic.seq(parser)
//                        .map((List<Object> values) -> {
//                            return (SendProcess) values.get(0);
//                        }))
//                .or(action));
//        return parser;
//    }

}
