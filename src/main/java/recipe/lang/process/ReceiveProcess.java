package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReceiveProcess extends Process {
    public Condition psi;
    public ChannelExpression channel;
    public Map<String, Expression> update;

    public ReceiveProcess(Condition psi, ChannelExpression channel, Map<String, Expression> update) {
        this.psi = psi;
        this.channel = channel;
        this.update = update;
    }

    public String toString() {
        return "<" + psi.toString() + ">" + channel + "?" + "[" + update + "]";
    }

    public Set<Transition> asTransitionSystem(State start, State end){
        Set<Transition> ts = new HashSet<>();
        ts.add(new Transition(start, end, this));

        return ts;
    }

    public static Parser parser(TypingContext messageContext,
                                TypingContext localContext,
                                TypingContext channelContext){
        TypingContext localAndChannelContext = TypingContext.union(localContext, channelContext);
        TypingContext channelContextWithLocalChannelVars = TypingContext.union(localContext.getSubContext(ChannelVariable.class), channelContext);

        Parser localGuard = Condition.typeParser(localAndChannelContext);

        Parser delimetedCondition =
                (CharacterParser.of('<').trim())
                        .seq(localGuard)
                        .seq(CharacterParser.of('>').trim())
                        .map((List<Object> values) -> (Condition) values.get(1));


        TypingContext localAndChannelAndMessageContext = TypingContext.union(TypingContext.union(localContext, channelContext), messageContext);
        Parser localAssignment = Parsing.assignmentListParser(localContext, localAndChannelAndMessageContext);

        Parser parser =
                delimetedCondition
                        .seq(ChannelExpression.parser(channelContextWithLocalChannelVars))
                        .seq(CharacterParser.of('?'))
                        .seq((CharacterParser.of('[').trim()))
                        .seq(localAssignment)
                        .seq((CharacterParser.of(']').trim()))
                        .map((List<Object> values) -> {
                            Condition psi = (Condition) values.get(0);
                            ChannelExpression channel = (ChannelExpression) values.get(1);
                            Map<String, Expression> update = (Map<String, Expression>) values.get(4);
                            ReceiveProcess action = new ReceiveProcess(psi, channel, update);
                            return action;
                        });

        return parser;
    }
}
