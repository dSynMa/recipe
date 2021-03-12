package recipe.lang;

import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.parser.combinators.SettableParser;
import recipe.lang.agents.AgentParser;
import recipe.lang.agents.behaviour.AgentBehaviour;
import recipe.lang.agents.behaviour.AgentBehaviourParser;
import recipe.lang.agents.behaviour.RecipeParser;
import recipe.lang.agents.behaviour.actions.ActionParser;
import recipe.lang.agents.behaviour.actions.conditions.*;
import static org.petitparser.parser.primitive.CharacterParser.word;

import java.util.List;

public class ProcessDefinitionParser extends RecipeParser {

    public ProcessDefinitionParser(){
        AgentBehaviourParser agentBehaviourParser = new AgentBehaviourParser();
        AgentParser agentParser = new AgentParser(agentBehaviourParser);
        setParser(createParser(agentParser));
    }

    public ProcessDefinitionParser(AgentParser agentParser){
        setParser(createParser(agentParser));
    }

    private static Parser createParser(AgentParser agentParser){
        SettableParser parser = SettableParser.undefined();
        Parser agent = agentParser.getParser();
        Parser condition = ConditionParser.getStaticParser();

        //TODO there needs to be a separate variable class and parser
        Parser type = (word().plus()).flatten();
        Parser variable = (word().plus()).flatten();
        Parser value = (word().plus()).flatten();

        Parser typedVariable = variable.trim()
                .seq(CharacterParser.of(':').trim())
                .seq(type.trim())
                .flatten();

        Parser agentName = (word().plus());
        Parser guardDefinition = StringParser.of("guard").trim()
                .seq(agentName)
                .seq(CharacterParser.of('(').trim())
                .seq(typedVariable.separatedBy(typedVariable.trim()))
                .seq(CharacterParser.of(')').trim())
                .seq(StringParser.of(":=").trim())
                .seq(condition.trim())
                .map((List<Object> values) -> {
                    return null;
                });

//        channels: a,b,c
//        message structure: int d1, bool d2
//        communication variables: int f, bool g
//
//
//        guard g(a : int) := ....
//
//        agent B
//          local:
//              b : Int := 0
//              c : Int := 0
//          relabel:
//              f <- c
//              g <- b != 0
//        behaviour: (<b == 0> c! (my.f = f) (d1 := b + 1, d2 := false)[b++])
//
//        agent A
//          local:
//              a : Int := 0
//          relabel:
//              f <- a
//              g <- a == 0
//        behaviour: B + (a == 0 ? c ....)
//
//        system: A | B

        parser.set((StringParser.of("channels").trim().seq(CharacterParser.of(':').trim())
                        .seq(variable.separatedBy(CharacterParser.of(',').trim())))
                .seq(StringParser.of("message-structure").trim().seq(CharacterParser.of(':').trim())
                    .seq(typedVariable.separatedBy(CharacterParser.of(',').trim())))
                .seq(StringParser.of("communication-variables").trim().seq(CharacterParser.of(':').trim())
                    .seq(typedVariable.separatedBy(CharacterParser.of(',').trim())))
                .seq(guardDefinition.separatedBy(CharacterParser.of('\n').trim()))
                .seq(agent.separatedBy(CharacterParser.of('\n').trim()))
                .seq(StringParser.of("system").trim())
                    .seq(CharacterParser.of(':').trim())
                    .seq(agentName.delimitedBy(StringParser.of("|").trim()))
                .map((List<Object> values) -> {
                    return null;
                })
        );

        return parser;
    }
}

