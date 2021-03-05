package recipe.lang.agents.behaviour.actions.conditions;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;

import java.util.List;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class ConditionParser{
    Parser parser;

    public Parser getParser(){
        return parser;
    }

    public boolean parse(String s){
        Parser start = parser.end();

        return start.accept(s);
    }

    public ConditionParser(){
        parser = createParser();
    }

    private Parser createParser(){
        SettableParser parser = SettableParser.undefined();
        SettableParser bracketedCondition = SettableParser.undefined();

        bracketedCondition.set(StringParser.of("(").trim().seq(parser).seq(StringParser.of(")").trim())
                .map((List<Object> values) -> {
                    Condition cond = (Condition) values.get(1);
                    return cond;
                })
        );

        SettableParser basicCondition = SettableParser.undefined();

        basicCondition.set((CharacterParser.of('!').trim()).seq(basicCondition)
                .map((List<Object> values) -> {
                    Condition cond = (Condition) values.get(1);
                    return new Not(cond);
                })
                .or(StringParser.of("(").trim().seq(parser).seq(StringParser.of(")").trim())
                        .map((List<Object> values) -> {
                            Condition cond = (Condition) values.get(1);
                            return cond;
                        }))
                .or(word().plus().flatten()
                        .map((String value) -> {
                            return new Atom(value);
                        }))
        );

        parser.set((basicCondition.seq(CharacterParser.of('&').trim()).seq(basicCondition)
                .map((List<Object> values) -> {
                    Condition cond1 = (Condition) values.get(0);
                    Condition cond2 = (Condition) values.get(2);
                    return new And(cond1, cond2);
                }))
                .or(basicCondition.seq(StringParser.of("|").trim()).seq(basicCondition)
                        .map((List<Object> values) -> {
                            Condition cond1 = (Condition) values.get(0);
                            Condition cond2 = (Condition) values.get(2);
                            return new Or(cond1, cond2);
                        }))
                .or(basicCondition)
        );

        return parser;
    }
}
