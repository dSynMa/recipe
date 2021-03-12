package recipe.lang.agents.behaviour;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.process.ActionParser;
import recipe.lang.process.SendBasicProcess;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.ConditionParser;

import java.util.List;

public class AgentBehaviourParser extends RecipeParser{

    public AgentBehaviourParser(){
        ConditionParser conditionParser = new ConditionParser();
        parser = createParser(conditionParser, new ActionParser(conditionParser));
    }

    public AgentBehaviourParser(ConditionParser conditionParser, ActionParser actionParser){
        parser = createParser(conditionParser, actionParser);
    }

    private static Parser createParser(ConditionParser conditionParser, ActionParser actionParser){
        SettableParser parser = SettableParser.undefined();
        SettableParser basic = SettableParser.undefined();
        Parser condition = conditionParser.getParser();
        Parser action = actionParser.getParser();

        basic.set((CharacterParser.of('(').trim()).seq(action).seq((CharacterParser.of(')').trim()))
                .map((List<Object> values) -> {
                    return (SendBasicProcess) values.get(0);
                }));

        parser.set((basic.seq(StringParser.of("+").trim()).seq(parser))
                .map((List<Object> values) -> {
                    return new Choice((AgentBehaviour) values.get(0), (AgentBehaviour) values.get(2));
                })
                .or(basic.seq(StringParser.of(";").trim()).seq(parser)
                        .map((List<Object> values) -> {
                            return new Sequence((AgentBehaviour) values.get(0), (AgentBehaviour) values.get(2));
                        }))
                .or((StringParser.of("<").trim().seq(condition).seq(StringParser.of(">").trim())).seq(parser)
                        .map((List<Object> values) -> {
                            return new Guarded((Condition) values.get(1), (AgentBehaviour) values.get(3));
                        }))
                .or(basic.seq(parser)
                        .map((List<Object> values) -> {
                            return (SendBasicProcess) values.get(0);
                        }))
                .or(action));
        return parser;
    }

}
