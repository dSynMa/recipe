package recipe.lang.agents;

import org.petitparser.context.Failure;
import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.ChoiceParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.FailureParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;
import recipe.lang.utils.LazyParser;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class AgentInstance {
    private String label;
    private Expression<Boolean> init;

    public String getLabel() {
        return label;
    }

    public Expression<Boolean> getInit() {
        return init;
    }

    public Agent getAgent() {
        return agent;
    }

    private Agent agent;

    public AgentInstance(String label, Expression<Boolean> init, Agent agent) {
        this.label = label;
        this.init = init;
        this.agent = agent;
    }

    public static Parser parser(Set<Agent> agents){
        Map<String,Agent> agentNames = new HashMap<>();
        agents.forEach((x) -> agentNames.put(x.getName(), x));

        Map<String, Map<String, TypedVariable>> agentLocalVars = new HashMap<>();
        agents.forEach((x) -> agentLocalVars.put(x.getName(), x.getStore().getAttributes()));

        AtomicReference<TypingContext> context = new AtomicReference<>(new TypingContext());

        Parser agentParser = Parsing.disjunctiveStringParser(new ArrayList(agentNames.keySet()))
                .map((String val) -> {
                    TypingContext contextt = new TypingContext();
                    for(TypedVariable tv : agentLocalVars.get(val).values()){
                        contextt.set(tv.getName(), tv.getType());
                    }
                    context.get().setAll(contextt);
                    return val;
                });

        Parser parser = (agentParser.trim()
                .seq(CharacterParser.of('(').trim())
                .seq(CharacterParser.word().star().trim().flatten())
                .seq(CharacterParser.of(',').trim())
                .seq(new LazyParser<TypingContext>((TypingContext contextt) -> {
                    try {
                        return Condition.parser(contextt);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return FailureParser.withMessage("Failed to parse initial condition in AgentInstance.");
                    }
                }, context.get()).trim())
                .seq(CharacterParser.of(')').trim())
                .map((List<Object> values) -> {
                    return new AgentInstance((String) values.get(2), (Expression<Boolean>) values.get(4), agentNames.get((String) values.get(0)));
                })).or(agentParser.map((String value) -> {
                    return new AgentInstance("", Condition.getTrue(), agentNames.get(value));
        }));

        return parser;
    }
}
