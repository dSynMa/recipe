package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.Config;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.*;

public class ReceiveProcess extends BasicProcess {

    public ReceiveProcess(String label, Expression<Boolean> psi, Expression channel, Map<String, Expression> update) {
        this.label = label;
        this.psi = psi;
        this.channel = channel;
        this.update = update;
    }

    public String toString() {
        return "<" + psi.toString() + ">" + channel + "?" + "[" + update + "]";
    }

    public Expression<Boolean> entryCondition(){
        return psi;
    }

    public void addEntryCondition(Expression<Boolean> condition){
        psi = new And(condition, psi);
    }

    public Set<Transition> asTransitionSystem(State start, State end){
        Set<Transition> ts = new HashSet<>();
        ts.add(new ProcessTransition(start, end, this));

        return ts;
    }

    public static Parser parser(TypingContext messageContext,
                                TypingContext localContext) throws Exception {
        TypingContext localChannelVars = localContext.getSubContext(Enum.getEnum(Config.channelLabel));

        TypingContext localAndChannelAndMessageContext = TypingContext.union(localContext, messageContext);
        Parser localAssignment = Parsing.assignmentListParser(localContext, localAndChannelAndMessageContext);

        Parser localGuard = Condition.typeParser(localAndChannelAndMessageContext);

        Parser delimetedCondition =
                (CharacterParser.of('<').trim())
                        .seq(localGuard)
                        .seq(CharacterParser.of('>').trim())
                        .map((List<Object> values) -> (Expression<Boolean>) values.get(1));



        Parser parser = (CharacterParser.word().plus().trim()
                        .seq(CharacterParser.of(':').trim()).flatten()).optional()
                        .seq(delimetedCondition.trim())
                        .seq((((Enum.getEnum(Config.channelLabel).valueParser()).seq(StringParser.of("?").trim())).or(localChannelVars.variableParser().seq(StringParser.of("?").trim()))))
                        .seq((CharacterParser.of('[').trim()))
                        .seq(localAssignment.optional(new HashMap<String, Expression>()))
                        .seq((CharacterParser.of(']').trim()))
                        .map((List<Object> values) -> {
                            String label = ((String) values.get(0));
                            if(label != null) label = label.replace(":", "").trim();
                            int i = 1;
                            Expression<Boolean> psi = (Expression<Boolean>) values.get(i);
                            Expression channel = (Expression) ((List) values.get(i+1)).get(0);
                            Map<String, Expression> update = (Map<String, Expression>) values.get(i+3);
                            ReceiveProcess action = new ReceiveProcess(label, psi, channel, update);
                            return action;
                        });

        return parser;
    }
}
