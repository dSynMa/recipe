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

public class SupplyProcess extends BasicProcessWithMessage {

    public Expression<Boolean> getMessageGuard() {
        return messageGuard;
    }

    public Expression<Boolean> messageGuard;

    public SupplyProcess(String label, Expression<Boolean> psi, Expression<Boolean> messageGuard, Map<String, Expression> message, Map<String, Expression> update) {
        this.label = label;
        this.psi = psi;
        this.messageGuard = messageGuard;
        this.message = message;
        this.update = update;
    }

    public String toString() {
        return "<" + psi.toString() + ">sply@" + channel + "(" + message + ")[" + update + "]";
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
                        .seq(StringParser.of("sply@").trim())
                        .seq(messageGuard.trim())
                        .seq((CharacterParser.of('(').trim()))
                        .seq(messageAssignment)
                        .seq((CharacterParser.of(')').trim()))
                        .seq((CharacterParser.of('[').trim()))
                        .seq(localAssignment.optional(new HashMap<String, Expression>()))
                        .seq((CharacterParser.of(']').trim()))
                        .map((List<Object> values) -> {
                            String label = ((String) values.get(0));
                            Expression<Boolean> psi = (Expression<Boolean>) values.get(1);
                            Expression channel = (Expression) values.get(3);
                            Map<String, Expression> message = (Map<String, Expression>) values.get(5);
                            Map<String, Expression> update = (Map<String, Expression>) values.get(8);                    
                            
                            if(label != null) label = label.replace(":", "").trim();
                            SupplyProcess action = new SupplyProcess(label, psi, channel, message, update);
                            return action;
                        });

        return parser;
    }
    
    @Override
    public String prettyPrintLabel() {
        return ((label == null || label == "") ? getMessageGuard().toString() : label);// + " [sply]";
    }
}
