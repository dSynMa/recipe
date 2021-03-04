package recipe.lang.actions;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.conditions.Condition;
import recipe.lang.conditions.ConditionParser;

import java.util.List;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class ActionParser {

    Parser parser;
    private ConditionParser conditionParser;

    public Parser getParser(){
        return parser;
    }

    public boolean parse(String s){
        Parser start = parser.end();

        return start.accept(s);
    }

    public ActionParser(){
        this.conditionParser = new ConditionParser();
        parser = createParser(this.conditionParser);
    }

    public ActionParser(ConditionParser conditionParser){
        this.conditionParser = conditionParser;
        parser = createParser(this.conditionParser);
    }

    private Parser createParser(ConditionParser conditionParser){
        SettableParser parser = SettableParser.undefined();

        Parser equation = word().seq(StringParser.of("=").trim()).seq(word());
        SettableParser channelParser = SettableParser.undefined();
        Parser condition = this.conditionParser.getParser();

        channelParser.set(condition
                .seq(CharacterParser.of('#').trim())
                .seq(word().plus().trim()) //ch variable
                .seq(CharacterParser.of('#').trim()));

        // cond#receiver#!(v,w)[x = v, y = w]>(cond)
        SettableParser sendActionParser = SettableParser.undefined();

        sendActionParser.set(channelParser
                .seq(CharacterParser.of('!'))
                .seq((CharacterParser.of('(').trim()))
                .seq((((word().seq(StringParser.of(",").trim()))).star().seq(word().trim())).or(word()))
                .seq((CharacterParser.of(')').trim()))
                .seq((CharacterParser.of('[').trim()))
                .seq(((equation.seq(CharacterParser.of(',').trim())).star().seq(equation.trim())).or(equation))
                .seq((CharacterParser.of(']').trim()))
                .seq((CharacterParser.of('>').trim()))
                .seq(condition)
                .map((List<Object> values) -> {
                    Condition psi = (Condition) values.get(0);
                    String channel = (String) values.get(2);
                    String message = (String) values.get(6);
                    String update = (String) values.get(9);
                    Condition guard = (Condition) values.get(12);
                    SendAction action = new SendAction(psi, channel, message, update, guard);
                    return action;
                }));

        SettableParser receiveActionParser = SettableParser.undefined();

        receiveActionParser.set(channelParser
                .seq(CharacterParser.of('?'))
                .seq((CharacterParser.of('(').trim()))
                .seq((((word().seq(StringParser.of(",").trim()))).star().seq(word().trim())).or(word()))
                .seq((CharacterParser.of(')').trim()))
                .seq((CharacterParser.of('[').trim()))
                .seq(((equation.seq(CharacterParser.of(',').trim())).star().seq(equation.trim())).or(equation))
                .seq((CharacterParser.of(']').trim()))
                .map((List<Object> values) -> {
                    Condition psi = (Condition) values.get(0);
                    String channel = (String) values.get(2);
                    String message = (String) values.get(6);
                    String update = (String) values.get(9);
                    ReceiveAction action = new ReceiveAction(psi, channel, message, update);
                    return action;
                }));

        parser.set(sendActionParser.or(receiveActionParser));
        return parser;
    }

}
