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

public class AgentBehaviourParser {
    Parser parser;
    private ConditionParser conditionParser;
    private ActionParser actionParser;

    public Parser getParser(){
        return parser;
    }

    public boolean parse(String s){
        Parser start = parser.end();

        return start.accept(s);
    }

    public AgentBehaviourParser(){
        conditionParser = new ConditionParser();
        actionParser = new ActionParser(conditionParser);
        parser = createParser(conditionParser, actionParser);
    }

    public AgentBehaviourParser(ConditionParser conditionParser, ActionParser actionParser){
        this.conditionParser = conditionParser;
        this.actionParser = actionParser;
        parser = createParser(this.conditionParser, this.actionParser);
    }

    private Parser createParser(ConditionParser conditionParser, ActionParser actionParser){
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
