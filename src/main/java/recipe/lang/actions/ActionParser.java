package recipe.lang.actions;

import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.ConditionParser;

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
        Result r = start.parse(s);
        if(r.isFailure()) {
            System.out.println(r.getMessage());
        }

        return r.isSuccess();
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

        Parser channelParser = (word().plus().trim()).flatten();
        Parser condition = this.conditionParser.getParser();

        Parser delimetedCondition = (CharacterParser.of('<').trim())
                            .seq(condition)
                            .seq(CharacterParser.of('>').trim())
                                .map((List<Object> values) -> {
                                    return (Condition) values.get(1);
                                });

        Parser sendPrefix = delimetedCondition
                .seq(channelParser)
                .seq(CharacterParser.of('!'));

        SettableParser messageParser = SettableParser.undefined();

        messageParser.set((word().plus().trim().flatten()
                    .seq(CharacterParser.of(',').trim())
                    .seq(messageParser).flatten())
                .or(word().plus().trim().flatten()));

        Parser assignment = (word().seq(StringParser.of(":=").trim()).seq(word())).flatten();

        SettableParser assignmentParser = SettableParser.undefined();

        assignmentParser.set(((assignment
                    .seq(CharacterParser.of(',').trim())
                    .seq(assignmentParser)).flatten())
                .or(assignment));

        SettableParser sendActionParser = SettableParser.undefined();

        sendActionParser.set(sendPrefix
                .seq(condition)
                .seq((CharacterParser.of('(').trim()))
                .seq(messageParser)
                .seq((CharacterParser.of(')').trim()))
                .seq((CharacterParser.of('[').trim()))
                .seq(assignmentParser)
                .seq((CharacterParser.of(']').trim()))
                .map((List<Object> values) -> {
                    Condition psi = (Condition) values.get(0);
                    String channel = (String) values.get(1);
                    Condition guard = (Condition) values.get(3);
                    String message = (String) values.get(5);
                    String update = (String) values.get(8);
                    SendAction action = new SendAction(psi, channel, message, update, guard);
                    return action;
                })
        );

        Parser receivePrefix = delimetedCondition
                .seq(channelParser)
                .seq(CharacterParser.of('?'));

        SettableParser receiveActionParser = SettableParser.undefined();

        receiveActionParser.set(receivePrefix
                .seq((CharacterParser.of('(').trim()))
                .seq(messageParser)
                .seq((CharacterParser.of(')').trim()))
                .seq((CharacterParser.of('[').trim()))
                .seq(assignmentParser)
                .seq((CharacterParser.of(']').trim()))
                .map((List<Object> values) -> {
                    Condition psi = (Condition) values.get(0);
                    String channel = (String) values.get(1);
                    String message = (String) values.get(4);
                    String update = (String) values.get(7);
                    ReceiveAction action = new ReceiveAction(psi, channel, message, update);
                    return action;
                }));

        parser.set(sendActionParser.or(receiveActionParser));
        return parser;
    }

}
