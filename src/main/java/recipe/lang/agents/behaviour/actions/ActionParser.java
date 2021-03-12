package recipe.lang.agents.behaviour.actions;

import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.agents.behaviour.RecipeParser;
import recipe.lang.agents.behaviour.actions.conditions.Condition;
import recipe.lang.agents.behaviour.actions.conditions.ConditionParser;

import java.util.List;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class ActionParser extends RecipeParser {
    private ConditionParser conditionParser;

    public ActionParser(){
        this.conditionParser = new ConditionParser();
        setParser(createParser(this.conditionParser));
    }

    public ActionParser(ConditionParser conditionParser){
        this.conditionParser = conditionParser;
        setParser(createParser(this.conditionParser));
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

        Parser variable = (StringParser.of("my.").seq(word().plus())
                        .or(word().plus())).flatten();
        Parser value = (word().plus()).flatten();

        Parser assignment = (variable.trim().seq(StringParser.of(":=").trim()).seq(value)).flatten();

        SettableParser assignmentList = SettableParser.undefined();

        assignmentList.set(((assignment
                    .seq(CharacterParser.of(',').trim())
                    .seq(assignmentList)).flatten())
                .or(assignment));

        SettableParser sendActionParser = SettableParser.undefined();

        sendActionParser.set(sendPrefix
                .seq(condition)
                .seq((CharacterParser.of('(').trim()))
                .seq(messageParser)
                .seq((CharacterParser.of(')').trim()))
                .seq((CharacterParser.of('[').trim()))
                .seq(assignmentList)
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
                .seq(assignmentList)
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
