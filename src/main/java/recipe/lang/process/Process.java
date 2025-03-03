package recipe.lang.process;

import static org.petitparser.parser.primitive.CharacterParser.of;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.tools.ExpressionBuilder;

import recipe.Config;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.location.AnyLocation;
import recipe.lang.expressions.location.Location;
import recipe.lang.expressions.location.NamedLocation;
import recipe.lang.expressions.location.SelfLocation;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Type;
import recipe.lang.utils.Deserialization;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.ParsingException;

public abstract class Process {

    public static int stateSeed = 0;

    public int getSeed(){
        stateSeed++;
        return stateSeed;
    }

    public abstract Set<Transition> asTransitionSystem(State start, State end);

    public abstract Expression<Boolean> entryCondition();

    public abstract void addEntryCondition(Expression<Boolean> condition);

    public static Parser parser(TypingContext messageContext,
                                TypingContext localContext,
                                TypingContext communicationContext) throws Exception {
        Parser receiveProcess = ReceiveProcess.parser(messageContext, localContext);
        Parser sendProcess = SendProcess.parser(messageContext, localContext, communicationContext);
        Parser getProcess = GetProcess.parser(messageContext, localContext, communicationContext);
        Parser supplyProcess = SupplyProcess.parser(messageContext, localContext, communicationContext);

        ExpressionBuilder builder = new ExpressionBuilder();
        builder.group()
                .primitive(sendProcess.or(receiveProcess).or(getProcess).or(supplyProcess).trim())
                .wrapper(of('(').trim(), of(')').trim(),
                        (List<Process> values) -> values.get(1));
        
        // repetition is a prefix operator
        builder.group()
                .prefix(StringParser.of("rep").trim(), (List<Process> values) -> new Iterative(values.get(1)));

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

    public static Process deserialize(JSONObject jProc, TypingContext ctx) throws Exception {
        String type = jProc.getString("$type");
        String label = jProc.optString("name");
        if (type.equals("Send") || type.equals("Receive") || type.equals("Get") || type.equals("Supply")) {
            // Common stuff: psi & update
            Expression psi = Deserialization.deserializeExpr(jProc.getJSONObject("psi"), ctx);
            Map<String, Expression> update = new HashMap<>();
            JSONArray jUpdate = jProc.getJSONArray("update");
            for (int i=0; i<jUpdate.length(); i++) {
                JSONObject jAssign = jUpdate.getJSONObject(i);
                String lhs = jAssign.getJSONObject("left").getString("$refText");
                Expression rhs = Deserialization.deserializeExpr(jAssign.getJSONObject("right"), ctx);
                update.put(lhs, rhs);
            }
            
            // Channel expression (send/receive)
            JSONObject jChanExpr = jProc.optJSONObject("chanExpr");
            Expression chanExpr = null;
            if (jChanExpr != null) {
                if (jChanExpr.has("bcast")) {
                    chanExpr = new TypedValue<Enum>(Enum.getEnum(Config.channelLabel), Config.broadcast);
                }
                else {
                    chanExpr = Deserialization.deserializeRef(jChanExpr.getJSONObject("channel").getString("$refText"), ctx);
                    //chanExpr = new TypedVariable<Type>(Enum.getEnum(Config.channelLabel), jChanExpr.getJSONObject("channel").getString("$refText"));
                }
            }

            // Location (get/supply)
            JSONObject jWhere = jProc.optJSONObject("where");
            Location location = null;
            if (jWhere != null) {
                if (jWhere.has(Config.myselfKeyword)) {
                    location = new SelfLocation();
                }
                else if (jWhere.has("any")) {
                    location = new AnyLocation();
                }
                else {
                    Type locationType = Enum.getEnum(Config.locationLabel);
                    location = new NamedLocation(new TypedVariable<Type>(locationType, jWhere.getJSONObject("location").getString("$refText")));
                }
            }
            // Message (send/supply)
            Map<String, Expression> message = new HashMap<>();
            JSONArray jData = jProc.optJSONArray("data");
            if (jData != null) {
                for (int i=0; i<jData.length(); i++) {
                    JSONObject jAssign = jData.getJSONObject(i);
                    String lhs = jAssign.getJSONObject("left").getString("$refText");
                    Expression rhs = Deserialization.deserializeExpr(jAssign.getJSONObject("right"), ctx);
                    message.put(lhs, rhs);
                }
            }
            
            if (type.equals("Send")) {
                Expression sendGuard = Deserialization.deserializeExpr(jProc.getJSONObject("sendGuard"), ctx);
                return new SendProcess(label, psi, chanExpr, message, update, sendGuard);
            }
            if (type.equals("Receive")) {
                return new ReceiveProcess(label, psi, chanExpr, update);
            }
            if (type.equals("Get")) {
                return new GetProcess(label, psi, location, update);
            }
            if (type.equals("Supply")) {
                return new SupplyProcess(label, psi, location, message, update);
            }
        }
        if (type.equals("Rep")) {
            Process p = deserialize(jProc.getJSONObject("process"), ctx);
            return new Iterative(p);
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
}
