package recipe.lang;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.parser.combinators.SettableParser;
import recipe.lang.agents.behaviour.AgentBehaviour;
import recipe.lang.agents.behaviour.AgentParser;
import recipe.lang.process.ActionParser;
import recipe.lang.expressions.predicate.ConditionParser;

import java.util.List;

public class ProcessDefinitionParser {
    Parser parser;
    ConditionParser conditionParser;
    ActionParser actionParser;
    AgentParser agentParser;

    public Parser getParser(){
        return parser;
    }

    public boolean parse(String s){
        Parser start = parser.end();

        return start.accept(s);
    }

    public ProcessDefinitionParser(){
        conditionParser = new ConditionParser();
        actionParser = new ActionParser(conditionParser);
        agentParser = new AgentParser(conditionParser, actionParser);
        parser = createParser(agentParser);
    }

    public ProcessDefinitionParser(AgentParser agentParser){
        this.agentParser = agentParser;
        parser = createParser(agentParser);
    }

    private Parser createParser(AgentParser agentParser){
        SettableParser parser = SettableParser.undefined();
        Parser agent = agentParser.getParser();

        parser.set((StringParser.of("rec").trim()
                    .seq(StringParser.of(".").trim())
                    .seq(agent))
                        .map((List<Object> values) -> {
                            return new ProcessDefinition((AgentBehaviour) values.get(2));
                        })
               );
        return parser;
    }
}

