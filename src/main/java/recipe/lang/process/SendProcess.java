package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class SendProcess extends Process {
    public Condition psi;
    public ChannelExpression channel;
    public Map<String, Expression> message;
    public Map<String, Expression> update;
    public Condition guard;

    public SendProcess(Condition psi, ChannelExpression channel, Map<String, Expression> message, Map<String, Expression> update, Condition guard) {
        this.psi = psi;
        this.channel = channel;
        this.message = message;
        this.update = update;
        this.guard = guard;
    }

    public String toString() {
        return "<" + psi.toString() + ">" + channel + "!" + "(" + guard.toString() + ")" + "(" + message + ")[" + update + "]";
    }

    public Set<Transition> asTransitionSystem(State start, State end){
        Set<Transition> ts = new HashSet<>();
        ts.add(new Transition(start, end, this));

        return ts;
    }

    public Condition entryCondition(){
        return psi;
    }

    public void addEntryCondition(Condition condition){
        psi = new And(condition, psi);
    }

    public static Parser parser(TypingContext messageContext,
                         TypingContext localContext,
                         TypingContext communicationContext,
                         TypingContext channelContext){
        TypingContext localAndChannelContext = TypingContext.union(localContext, channelContext);
        TypingContext channelContextWithLocalChannelVars = TypingContext.union(localContext.getSubContext(ChannelVariable.class), channelContext);

        Parser localGuard = Condition.typeParser(localAndChannelContext);

        Parser delimetedCondition =
                (CharacterParser.of('<').trim())
                        .seq(localGuard)
                        .seq(CharacterParser.of('>').trim())
                        .map((List<Object> values) -> (Condition) values.get(1));

        TypingContext localAndChannelAndCommunicationContext =
                TypingContext.union(localAndChannelContext, communicationContext);

        Parser messageGuard = Condition.typeParser(localAndChannelAndCommunicationContext);

        Parser messageAssignment = Parsing.assignmentListParser(messageContext, localAndChannelAndCommunicationContext);
        Parser localAssignment = Parsing.assignmentListParser(localContext, localAndChannelContext);

        Parser parser =
                delimetedCondition
                        .seq(ChannelExpression.parser(channelContextWithLocalChannelVars))
                        .seq(CharacterParser.of('!'))
                        .seq(messageGuard)
                        .seq((CharacterParser.of('(').trim()))
                        .seq(messageAssignment)
                        .seq((CharacterParser.of(')').trim()))
                        .seq((CharacterParser.of('[').trim()))
                        .seq(localAssignment)
                        .seq((CharacterParser.of(']').trim()))
                        .map((List<Object> values) -> {
                            Condition psi = (Condition) values.get(0);
                            ChannelExpression channel = (ChannelExpression) values.get(1);
                            Condition guard = (Condition) values.get(3);
                            Map<String, Expression> message = (Map<String, Expression>) values.get(5);
                            Map<String, Expression> update = (Map<String, Expression>) values.get(8);
                            SendProcess action = new SendProcess(psi, channel, message, update, guard);
                            return action;
                        });

        return parser;
    }
}
