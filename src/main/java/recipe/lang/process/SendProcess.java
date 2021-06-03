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

import static org.petitparser.parser.primitive.CharacterParser.word;

public class SendProcess extends BasicProcess {

    public Map<String, Expression> message;

    public Expression<Boolean> getMessageGuard() {
        return messageGuard;
    }

    public Expression<Boolean> messageGuard;

    public SendProcess(Expression<Boolean> psi, Expression channel, Map<String, Expression> message, Map<String, Expression> update, Expression<Boolean> messageGuard) {
        this.psi = psi;
        this.channel = channel;
        this.message = message;
        this.update = update;
        this.messageGuard = messageGuard;
    }

    public Map<String, Expression> getMessage() {
        return message;
    }

    public String toString() {
        return "<" + psi.toString() + ">" + channel + "!" + "(" + messageGuard.toString() + ")" + "(" + message + ")[" + update + "]";
    }

    public Set<Transition> asTransitionSystem(State start, State end){
        Set<Transition> ts = new HashSet<>();
        ts.add(new ProcessTransition(start, end, this));

        return ts;
    }

    public Expression<Boolean> entryCondition(){
        return psi;
    }

    public void addEntryCondition(Expression<Boolean> condition){
        psi = new And(condition, psi);
    }

    public static Parser parser(TypingContext messageContext,
                         TypingContext localContext,
                         TypingContext communicationContext) throws Exception {
        TypingContext localChannelVars = localContext.getSubContext(Enum.getEnum(Config.channelLabel));

        Parser localGuard = Condition.typeParser(localContext);

        Parser delimetedCondition =
                (CharacterParser.of('<').trim())
                        .seq(localGuard)
                        .seq(CharacterParser.of('>').trim())
                        .map((List<Object> values) -> {
                            return (Expression<Boolean>) values.get(1);
                        });

        TypingContext localAndChannelAndCommunicationContext =
                TypingContext.union(localContext, communicationContext);

        Parser messageGuard = Condition.typeParser(localAndChannelAndCommunicationContext);

        Parser messageAssignment = Parsing.assignmentListParser(messageContext, localAndChannelAndCommunicationContext);
        Parser localAssignment = Parsing.assignmentListParser(localContext, localContext);

        Parser parser =
                delimetedCondition
                        .seq(localChannelVars.valueParser().or(localChannelVars.variableParser()))
                        .seq(CharacterParser.of('!'))
                        .seq(messageGuard)
                        .seq((CharacterParser.of('(').trim()))
                        .seq(messageAssignment)
                        .seq((CharacterParser.of(')').trim()))
                        .seq((CharacterParser.of('[').trim()))
                        .seq(localAssignment)
                        .seq((CharacterParser.of(']').trim()))
                        .map((List<Object> values) -> {
                            Expression<Boolean> psi = (Expression<Boolean>) values.get(0);
                            Expression channel = (Expression) values.get(1);
                            Expression<Boolean> guard = (Expression<Boolean>) values.get(3);
                            Map<String, Expression> message = (Map<String, Expression>) values.get(5);
                            Map<String, Expression> update = (Map<String, Expression>) values.get(8);
                            SendProcess action = new SendProcess(psi, channel, message, update, guard);
                            return action;
                        });

        return parser;
    }
}
