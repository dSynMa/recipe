package recipe.lang.agents;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.CharacterParser.*;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.agents.behaviour.AgentBehaviourParser;
import recipe.lang.agents.behaviour.RecipeParser;
import recipe.lang.agents.behaviour.actions.ActionParser;
import recipe.lang.agents.behaviour.actions.SendAction;
import recipe.lang.agents.behaviour.actions.conditions.Condition;
import recipe.lang.agents.behaviour.actions.conditions.ConditionParser;
import static org.petitparser.parser.primitive.CharacterParser.word;

import java.util.List;

public class AgentParser extends RecipeParser {
    private AgentBehaviourParser agentBehaviourParser;

    public AgentParser(){
        ConditionParser conditionParser = new ConditionParser();
        ActionParser actionParser = new ActionParser(conditionParser);
        agentBehaviourParser = new AgentBehaviourParser(conditionParser, actionParser);
        setParser(createParser(agentBehaviourParser));
    }

    public AgentParser(AgentBehaviourParser agentBehaviourParser){
        this.agentBehaviourParser = agentBehaviourParser;
        setParser(createParser(this.agentBehaviourParser));
    }

    private Parser createParser(AgentBehaviourParser agentBehaviourParser){
        SettableParser parser = SettableParser.undefined();
        SettableParser basic = SettableParser.undefined();
        Parser agentBehaviour = agentBehaviourParser.getParser();

        //TODO there needs to be a separate variable class and parser
        Parser type = (word().plus()).flatten();
        Parser variable = (word().plus()).flatten();
        Parser value = (word().plus()).flatten();

        Parser typedVariable = variable.trim()
                .seq(CharacterParser.of(':').trim())
                .seq(type.trim())
                .flatten();

        Parser typedAssignment = (typedVariable.trim()
                .seq(StringParser.of(":=").trim())
                .seq(value))
                .flatten();

        Parser relabeling = (variable.trim()
                .seq(StringParser.of("<-").trim())
                .seq(value))
                .flatten();
//    agent B
//      local:
//      b : Int := 0
//      c : Int := 0
//    relabel:
//      f <- c
//      g <- b != 0
//    behaviour: (<b == 0> c! (my.f = f) (d1 := b + 1, d2 := false)[b++])

        parser.set((StringParser.of("agent").trim())
                .seq(word().plus().trim())
                .seq(StringParser.of("local").trim())
                .seq(CharacterParser.of(':').trim())
                .seq(typedAssignment.delimitedBy(CharacterParser.of('\n')).trim())
                .seq(StringParser.of("relabel").trim())
                .seq(CharacterParser.of(':').trim())
                .seq(relabeling.delimitedBy(CharacterParser.of('\n')).trim())
                .seq(StringParser.of("behaviour").trim())
                .seq(CharacterParser.of(';').trim())
                .seq(agentBehaviour.trim())
                    .map((List<Object> values) -> {
                        return null;
                    }));

        return parser;
    }

}
