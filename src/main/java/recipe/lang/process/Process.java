package recipe.lang.process;

import org.json.JSONArray;
import org.json.JSONObject;
import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.tools.ExpressionBuilder;
import recipe.Config;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.expressions.location.AnyLocation;
import recipe.lang.expressions.location.Location;
import recipe.lang.expressions.location.NamedLocation;
import recipe.lang.expressions.location.PredicateLocation;
import recipe.lang.expressions.location.SelfLocation;
import recipe.lang.expressions.arithmetic.Floor;
import recipe.lang.expressions.predicate.IsLessOrEqualThan;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Enum;
import recipe.lang.types.Number;
import recipe.lang.types.Type;
import recipe.lang.utils.Deserialization;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.ParsingException;
import recipe.lang.types.Boolean;
import recipe.lang.utils.TypingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Apply the semantic feasibility constraints every action must satisfy.
     *
     * <p>This adds definedness conditions (for example, disabling transitions
     * whose expressions divide by zero) and numeric-domain constraints for
     * assignments and message payloads to the action's entry condition.
     *
     * <p>Both {@link #parser} and deserializers must call this exactly once
     * after building an action. The type contexts are the same ones used to
     * build the action: {@code localContext} describes the acting agent's
     * local variables, {@code messageContext} the message structure.
     *
     * @param action the parsed or deserialized action
     * @param localContext types of the acting agent's local variables
     * @param messageContext types of the declared message variables
     * @return the same action, with feasibility conditions added
     */
    public static BasicProcess applyFeasibilityConstraints(BasicProcess action,
                                                           TypingContext localContext,
                                                           TypingContext messageContext) {
        try {
            Expression<Boolean> defined = action.getPsi().definednessCondition();
            for (Expression<?> update : action.getUpdate().values()) {
                defined = new And(defined, update.definednessCondition());
            }
            if (action instanceof BasicProcessWithMessage messageAction) {
                for (Expression<?> payload : messageAction.getMessage().values()) {
                    defined = new And(defined, payload.definednessCondition());
                }
                constrainNumericAssignments(action, messageAction.getMessage(), messageContext);
            }
            // Feasibility belongs in psi so progress agrees with legal transitions.
            constrainNumericAssignments(action, action.getUpdate(), localContext);
            defined = defined.simplify();
            if (!defined.equals(recipe.lang.expressions.predicate.Condition.getTrue())) {
                action.addEntryCondition(defined);
            }
        } catch (Exception invalidExpression) {
            throw new IllegalArgumentException("Cannot determine action feasibility", invalidExpression);
        }
        return action;
    }

    /**
     * Build a process from the JSON AST produced by the JavaScript parser CLI.
     *
     * <p>Both GET and SUPPLY payloads are deserialized; get/supply therefore
     * carry message maps in both directions.
     */
    public static Process deserialize(JSONObject jProc, TypingContext ctx) throws Exception {
        String type = jProc.getString("$type");
        String label = jProc.optString("name");
        if (type.equals("Send") || type.equals("Receive") || type.equals("Get") || type.equals("Supply")) {
            Expression psi = Deserialization.deserializeExpr(jProc.getJSONObject("psi"), ctx);
            Map<String, Expression> update = new HashMap<>();
            JSONArray jUpdate = jProc.getJSONArray("update");
            for (int i = 0; i < jUpdate.length(); i++) {
                JSONObject jAssign = jUpdate.getJSONObject(i);
                String lhs = jAssign.getJSONObject("left").getString("$refText");
                Expression rhs = Deserialization.deserializeExpr(jAssign.getJSONObject("right"), ctx);
                update.put(lhs, rhs);
            }

            Expression chanExpr = null;
            JSONObject jChanExpr = jProc.optJSONObject("chanExpr");
            if (jChanExpr != null) {
                if (jChanExpr.has("bcast")) {
                    chanExpr = new TypedValue<Enum>(Enum.getEnum(Config.channelLabel), Config.broadcast);
                } else {
                    chanExpr = Deserialization.deserializeRef(
                        jChanExpr.getJSONObject("channel").getString("$refText"), ctx);
                }
            }

            Location location = new AnyLocation();
            JSONObject jWhere = jProc.optJSONObject("where");
            if (jWhere != null) {
                if (jWhere.has(Config.myselfKeyword)) {
                    location = new SelfLocation();
                } else if (jWhere.has("any")) {
                    location = new AnyLocation();
                } else if (jWhere.has("predicate")) {
                    Expression predicate = Deserialization.deserializeExpr(
                        jWhere.getJSONObject("predicate"), ctx);
                    if (predicate instanceof TypedVariable) {
                        TypedVariable tv = (TypedVariable) predicate;
                        if (tv.getType() != null
                                && tv.getType().equals(Enum.getEnum(Config.locationLabel))) {
                            location = new NamedLocation(tv);
                        } else {
                            location = new PredicateLocation((Expression<Boolean>) predicate);
                        }
                    } else {
                        location = new PredicateLocation((Expression<Boolean>) predicate);
                    }
                } else if (jWhere.has("location")) {
                    String name = jWhere.getJSONObject("location").getString("$refText");
                    if (name.equals(Config.myselfKeyword)) {
                        location = new SelfLocation();
                    } else if (name.equals("any")) {
                        location = new AnyLocation();
                    } else {
                        Type locationType = ctx.get(name);
                        if (locationType == null) {
                            throw new ParsingException("Unknown location variable '" + name + "'");
                        }
                        location = new NamedLocation(new TypedVariable(locationType, name));
                    }
                } else {
                    throw new ParsingException("Unsupported location: " + jWhere);
                }
            }

            Map<String, Expression> message = new HashMap<>();
            JSONArray jData = jProc.optJSONArray("data");
            if (jData != null) {
                for (int i = 0; i < jData.length(); i++) {
                    JSONObject jAssign = jData.getJSONObject(i);
                    String lhs = jAssign.getJSONObject("left").getString("$refText");
                    Expression rhs = Deserialization.deserializeExpr(jAssign.getJSONObject("right"), ctx);
                    message.put(lhs, rhs);
                }
            }

            BasicProcess action;
            if (type.equals("Send")) {
                Expression sendGuard = Deserialization.deserializeExpr(
                    jProc.getJSONObject("sendGuard"), ctx);
                action = new SendProcess(label, psi, chanExpr, message, update,
                    (Expression<Boolean>) sendGuard);
            } else if (type.equals("Receive")) {
                action = new ReceiveProcess(label, psi, chanExpr, update);
            } else if (type.equals("Get")) {
                action = new GetProcess(label, psi, location, update, message);
            } else {
                action = new SupplyProcess(label, psi, location, message, update);
            }
            return applyFeasibilityConstraints(action, ctx, ctx);
        }
        if (type.equals("Rep")) {
            return new Iterative(deserialize(jProc.getJSONObject("process"), ctx));
        }
        if (type.equals("Choice") || type.equals("Sequence")) {
            Process left = deserialize(jProc.getJSONObject("left"), ctx);
            if (!jProc.has("right")) {
                return left;
            }
            Process right = deserialize(jProc.getJSONObject("right"), ctx);
            switch (type) {
                case "Choice":
                    return new Choice(left, right);
                case "Sequence":
                    return new Sequence(left, right);
                default:
                    break;
            }
        }
        throw new ParsingException("Unexpected type '" + type + "'");
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
                        .map((BasicProcess action) ->
                                applyFeasibilityConstraints(action, localContext, messageContext)))
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
