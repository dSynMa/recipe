package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.Config;
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

import static recipe.Config.commVariableReferences;

public class SendProcess extends BasicProcessWithMessage {

    public Map<String, Expression> message;

    public Expression<Boolean> getMessageGuard() {
        return messageGuard;
    }

    public Expression<Boolean> messageGuard;

    public SendProcess(String label, Expression<Boolean> psi, Expression channel, Map<String, Expression> message, Map<String, Expression> update, Expression<Boolean> messageGuard) {
        this.label = label;
        this.psi = psi;
        this.channel = channel;
        this.message = message;
        this.update = update;
        this.messageGuard = messageGuard;
    }

    @Override
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
                        .seq(localGuard.map((Object value) -> {
                            return value;
                        }))
                        .seq(CharacterParser.of('>').trim())
                        .map((List<Object> values) -> {
                            return (Expression<Boolean>) values.get(1);
                        });

        TypingContext localAndChannelAndCommunicationContext =
                TypingContext.union(localContext, commVariableReferences(communicationContext));

        Parser messageGuard = Condition.typeParser(localAndChannelAndCommunicationContext);

        Parser messageAssignment = Parsing.assignmentListParser(messageContext, localAndChannelAndCommunicationContext);
        Parser localAssignment = Parsing.assignmentListParser(localContext, localContext);

        Parser parser = ((CharacterParser.word().plus().trim()).flatten()
                        .seq(CharacterParser.of(':').trim()).flatten()).optional().flatten()
                        .seq(delimetedCondition.trim())
                        .seq((((Enum.getEnum(Config.channelLabel).valueParser()).seq(StringParser.of("!").trim())).or(localChannelVars.variableParser().seq(StringParser.of("!").trim()))))
                        .seq(messageGuard.trim().map((Object obj) -> {
                            return obj;
                        }))
                        .seq((CharacterParser.of('(').trim()))
                        .seq(messageAssignment.map((Object obj) -> {
                            return obj;
                        }))
                        .seq((CharacterParser.of(')').trim()))
                        .seq((CharacterParser.of('[').trim()))
                        .seq(localAssignment.optional(new HashMap<String, Expression>()))
                        .seq((CharacterParser.of(']').trim()))
                        .map((List<Object> values) -> {
                            String label = ((String) values.get(0));
                            if(label != null) label = label.replace(":", "").trim();
                            int i = 1;
                            Expression<Boolean> psi = (Expression<Boolean>) values.get(i);
                            Expression channel = (Expression) ((List) values.get(i+1)).get(0);
                            Expression<Boolean> guard = (Expression<Boolean>) values.get(i+2);
                            Map<String, Expression> message = (Map<String, Expression>) values.get(i+4);
                            Map<String, Expression> update = (Map<String, Expression>) values.get(i+7);
                            SendProcess action = new SendProcess(label, psi, channel, message, update, guard);
                            return action;
                        });

        return parser;
    }
}
