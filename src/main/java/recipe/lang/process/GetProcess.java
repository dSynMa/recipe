package recipe.lang.process;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;

import recipe.lang.agents.ProcessTransition;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.NamedLocation;
import recipe.lang.types.Boolean;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import static recipe.Config.commVariableReferences;

public class GetProcess extends BasicProcess {

    public Expression<Boolean> messageGuard;

    public Expression<Boolean> getMessageGuard() {
        return messageGuard;
    }

    public GetProcess(String label, Expression<Boolean> psi, Expression<Boolean> messageGuard, Map<String, Expression> update) {
        this.label = label;
        this.psi = psi;
        this.messageGuard = messageGuard;
        this.update = update;
    }

    public String toString() {
        return "<" + psi.toString() + ">GET@(" + messageGuard + ")[" + update + "]";
    }

    @Override
    public Set<Transition> asTransitionSystem(State start, State end) {
        Set<Transition> ts = new HashSet<>();
        ts.add(new ProcessTransition(start, end, this));
        return ts;
    }

    @Override
    public Expression<Boolean> entryCondition() {
        return psi;
    }

    @Override
    public void addEntryCondition(Expression<Boolean> condition) {
        this.psi = new And(condition, psi);
    }

    public static Parser parser(TypingContext messageContext,
                                TypingContext localContext,
                                TypingContext communicationContext) throws Exception {
        TypingContext localAndChannelAndMessageContext = TypingContext.union(localContext, messageContext);
        TypingContext localAndChannelAndCommunicationContext =
                TypingContext.union(localContext, commVariableReferences(communicationContext));
        Parser localAssignment = Parsing.assignmentListParser(localContext, localAndChannelAndMessageContext);
        Parser localGuard = Condition.typeParser(localAndChannelAndMessageContext);
        Parser messageGuard = Condition.typeParser(localAndChannelAndCommunicationContext);
        Parser namedLocationParser = NamedLocation.parser();

        Parser delimetedCondition =
                (CharacterParser.of('<').trim())
                        .seq(localGuard)
                        .seq(CharacterParser.of('>').trim())
                        .map((List<Object> values) -> {
                            return (Expression<Boolean>) values.get(1);
                        });

        Parser parser = (CharacterParser.word().plus().trim()
                        .seq(CharacterParser.of(':').trim()).flatten()).optional()
                        .seq(delimetedCondition.trim())
                        // .seq((((Enum.getEnum(Config.channelLabel).valueParser()).seq(StringParser.of("?").trim())).or(localChannelVars.variableParser().seq(StringParser.of("?").trim()))))
                        .seq((StringParser.of("GET@").trim()))
                        .seq(messageGuard.trim().or(namedLocationParser))
                        .seq((CharacterParser.of('[').trim()))
                        .seq(localAssignment.optional(new HashMap<String, Expression>()))
                        .seq((CharacterParser.of(']').trim()))
                        .map((List<Object> values) -> {
                            String label = (String) values.get(0);
                            Expression<Boolean> psi = (Expression<Boolean>) values.get(1);
                            Expression<Boolean> locationPredicate = (Expression<Boolean>) values.get(3);
                            Map<String, Expression> update = (Map<String, Expression>) values.get(5);
                            if(label != null) label = label.replace(":", "").trim();
                            GetProcess action = new GetProcess(label, psi, locationPredicate, update);
                            return action;
                        });
        return parser;
    }

    @Override
    public String prettyPrintLabel() {
        return ((label == null || label == "") ? getMessageGuard().toString() : label);// + " [get]";
    }
}
