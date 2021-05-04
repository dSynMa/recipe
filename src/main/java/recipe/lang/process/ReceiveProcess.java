package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReceiveProcess extends BasicProcess {

    public ReceiveProcess(Expression<Boolean> psi, Expression channel, Map<String, Expression> update) {
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
                                TypingContext localContext,
                                TypingContext channelContext) throws Exception {
        TypingContext localAndChannelContext = TypingContext.union(localContext, channelContext);
        TypingContext channelContextWithLocalChannelVars = TypingContext.union(localContext.getSubContext(Enum.getEnum(Config.channelLabel)), channelContext);

        Parser localGuard = Condition.typeParser(localAndChannelContext);

        Parser delimetedCondition =
                (CharacterParser.of('<').trim())
                        .seq(localGuard)
                        .seq(CharacterParser.of('>').trim())
                        .map((List<Object> values) -> (Expression<Boolean>) values.get(1));


        TypingContext localAndChannelAndMessageContext = TypingContext.union(TypingContext.union(localContext, channelContext), messageContext);
        Parser localAssignment = Parsing.assignmentListParser(localContext, localAndChannelAndMessageContext);

        Parser parser =
                delimetedCondition
                        .seq(channelContextWithLocalChannelVars.valueParser().or(channelContextWithLocalChannelVars.variableParser()))
                        .seq(CharacterParser.of('?'))
                        .seq((CharacterParser.of('[').trim()))
                        .seq(localAssignment)
                        .seq((CharacterParser.of(']').trim()))
                        .map((List<Object> values) -> {
                            Expression<Boolean> psi = (Expression<Boolean>) values.get(0);
                            Expression channel = (Expression) values.get(1);
                            Map<String, Expression> update = (Map<String, Expression>) values.get(4);
                            ReceiveProcess action = new ReceiveProcess(psi, channel, update);
                            return action;
                        });

        return parser;
    }
}
