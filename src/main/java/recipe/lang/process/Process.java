package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.tools.ExpressionBuilder;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.expressions.arithmetic.Floor;
import recipe.lang.expressions.predicate.IsLessOrEqualThan;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Number;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.types.Boolean;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.of;

public abstract class Process {

    public static int stateSeed = 0;

    public int getSeed(){
        stateSeed++;
        return stateSeed;
    }

    public abstract Set<Transition> asTransitionSystem(State start, State end);

    public abstract Expression<Boolean> entryCondition();

    public abstract void addEntryCondition(Expression<Boolean> condition);

    private static void constrainNumericAssignments(BasicProcess action,
            java.util.Map<String, Expression> assignments, TypingContext context) {
        assignments.forEach((name, value) -> {
            if (context.get(name) instanceof recipe.lang.types.Integer) {
                try {
                    action.addEntryCondition(new IsEqualTo(value, new Floor(value)));
                } catch (MismatchingTypeException e) {
                    throw new IllegalArgumentException("Non-numeric assignment for " + name, e);
                }
            }
            if (context.get(name) instanceof BoundedInteger) {
                BoundedInteger type = (BoundedInteger) context.get(name);
                try {
                    action.addEntryCondition(boundedAssignmentCondition(type, value));
                } catch (MismatchingTypeException e) {
                    throw new IllegalArgumentException("Invalid bounds for " + name, e);
                }
            }
        });
    }

    /** Require the assigned value to stay within its target's declared bounds. */
    private static Expression<Boolean> boundedAssignmentCondition(
            BoundedInteger type, Expression<Number> value) throws MismatchingTypeException {
        TypedValue<Number> min = new TypedValue<>(recipe.lang.types.Integer.getType(), String.valueOf(type.getMin()));
        TypedValue<Number> max = new TypedValue<>(recipe.lang.types.Integer.getType(), String.valueOf(type.getMax()));
        Expression<Boolean> aboveMinimum = new IsLessOrEqualThan(min, value);
        Expression<Boolean> belowMaximum = new IsLessOrEqualThan(value, max);
        return new And(aboveMinimum, belowMaximum);
    }

    public static Parser parser(TypingContext messageContext,
                                TypingContext localContext,
                                TypingContext communicationContext) throws Exception {
        Parser receiveProcess = ReceiveProcess.parser(messageContext, localContext);
        Parser sendProcess = SendProcess.parser(messageContext, localContext, communicationContext);
        Parser getProcess = GetProcess.parser(messageContext, localContext, communicationContext);
        Parser supplyProcess = SupplyProcess.parser(messageContext, localContext, communicationContext);

        ExpressionBuilder builder = new ExpressionBuilder();
        builder.group()
                .primitive(sendProcess.or(receiveProcess).or(getProcess).or(supplyProcess).trim()
                        .map((BasicProcess action) -> {
                            // Both updates and outgoing data must fit their declared domains.
                            // Keep feasibility in psi so progress agrees with legal transitions.
                            constrainNumericAssignments(action, action.getUpdate(), localContext);
                            if (action instanceof BasicProcessWithMessage) {
                                constrainNumericAssignments(action,
                                    ((BasicProcessWithMessage) action).getMessage(), messageContext);
                            }
                            return action;
                        }))
                .wrapper(of('(').trim(), of(')').trim(),
                        (List<Process> values) -> values.get(1));
        
        // repetition is a prefix operator
        builder.group()
                .prefix(StringParser.of("rep").seq(CharacterParser.word().not()).trim(), (List<Process> values) -> new Iterative(values.get(1)));

        // sequence is right- and left-associative
        builder.group()
                .right(of(';').trim(), (List<Process> values) -> new Sequence(values.get(0), values.get(2)))
                .left(of(';').trim(), (List<Process> values) -> new Sequence(values.get(0), values.get(2)));

        // choice is right- and left-associative
        builder.group()
                .right(of('+').trim(), (List<Process> values) -> new Choice(values.get(0), values.get(2)))
                .left(of('+').trim(), (List<Process> values) -> new Choice(values.get(0), values.get(2)));

        return builder.build();
    }
}
