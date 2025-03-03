package recipe.lang.utils;

import static recipe.Config.channelLabel;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import recipe.Config;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.Predicate;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.Addition;
import recipe.lang.expressions.arithmetic.Division;
import recipe.lang.expressions.arithmetic.Multiplication;
import recipe.lang.expressions.arithmetic.Subtraction;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.GuardReference;
import recipe.lang.expressions.predicate.Implies;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.expressions.predicate.IsGreaterOrEqualThan;
import recipe.lang.expressions.predicate.IsGreaterThan;
import recipe.lang.expressions.predicate.IsLessOrEqualThan;
import recipe.lang.expressions.predicate.IsLessThan;
import recipe.lang.expressions.predicate.IsNotEqualTo;
import recipe.lang.expressions.predicate.Not;
import recipe.lang.expressions.predicate.Or;
import recipe.lang.ltol.Atom;
import recipe.lang.ltol.BigAnd;
import recipe.lang.ltol.BigOr;
import recipe.lang.ltol.Eventually;
import recipe.lang.ltol.Globally;
import recipe.lang.ltol.LTOL;
import recipe.lang.ltol.Necessary;
import recipe.lang.ltol.Next;
import recipe.lang.ltol.Observation;
import recipe.lang.ltol.Possibly;
import recipe.lang.types.Boolean;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Enum;
import recipe.lang.types.Guard;
import recipe.lang.types.Integer;
import recipe.lang.types.Type;
import recipe.lang.types.UnionType;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.ParsingException;

public class Deserialization {
    public static void checkType(JSONObject jObj, String expected) throws ParsingException {
        String objType = jObj.optString("$type");
        if (!expected.equals(objType)) {
            throw new ParsingException(String.format(
                "Wrong $type (expected %s, got %s)", expected, objType));
        }
    }

    public static Type deserializeType(JSONObject jObj) throws Exception {
        if (jObj.has("customType")) {
            JSONObject customType = jObj.getJSONObject("customType");
            return Enum.getEnum(customType.getString("$refText"));
        } else if (jObj.has("rangeType")) {
            JSONObject jRange = jObj.getJSONObject("rangeType");
            return new BoundedInteger(jRange.getInt("lower"), jRange.getInt("upper"));
        } else if (jObj.has("builtinType")) {
            String bt = jObj.getString("builtinType");
            switch (bt) {
                case "int":
                    return recipe.lang.types.Integer.getType();
                case "bool":
                    return recipe.lang.types.Boolean.getType();
                case "location":
                    return Enum.getEnum(Config.locationLabel);
                default:
                    throw new Exception("Unexpected builtinType " + bt);
            }
        } else {
            throw new ParsingException("Unexpected type for " + jObj.optString("name"));
        }
    }

    public static Expression deserializeRef(String name, TypingContext ctx) throws Exception {
        if (name.equals(Config.broadcast)) {
            return new TypedValue<Type>(Enum.getEnum(Config.channelLabel), name);
        }
        Type type = ctx.get(name);
        if (type == null) {
            throw new Exception(name + " has no type in context " + ctx);
        }
        if (type instanceof Enum) {
            try {
                ((Enum) type).interpret(name);
                return new TypedValue<Type>(type, name);
            }
            catch (MismatchingTypeException e) {
            }
        }
        return new TypedVariable<Type>(type, name);
    }

    public static Expression deserializeExpr(JSONObject jExpr, TypingContext context)
            throws MismatchingTypeException, ParsingException, Exception {
        Type type;
        switch (jExpr.getString("$type")) {
            case "NumberLiteral":
                int literal = jExpr.getInt("value");
                return new TypedValue<Integer>(Integer.getType(), String.valueOf(literal));
            case "Myself":
                return new TypedValue<Enum>(Enum.getEnum(Config.locationLabel), Config.myselfKeyword);
            case "BoolLiteral":
                return new TypedValue<Boolean>(recipe.lang.types.Boolean.getType(), jExpr.optString("value"));
            case "Broadcast":
                return new TypedValue<Enum>(Enum.getEnum(Config.channelLabel), Config.broadcast);
            case "Ref":
                if (jExpr.has("variable")) {
                    String name = jExpr.getJSONObject("variable").getString("$refText");
                    return deserializeRef(name, context);
                } else if (jExpr.has("currentChannel")) {
                    return new TypedVariable<Type>(Enum.getEnum(Config.channelLabel), Config.channelLabel);
                }
            case "Neg":
                return new Not(deserializeExpr(jExpr.getJSONObject("expr"), context));
            case "Expr":
                return deserializeExpr(jExpr.getJSONObject("left"), context);
            case "GuardCall":
                JSONArray jValues = jExpr.getJSONArray("args");
                Expression[] values = new Expression[jValues.length()];
                for (int i = 0; i < jValues.length(); i++) {
                    values[i] = deserializeExpr(jValues.getJSONObject(i), context);
                }
                Guard guardType = (Guard) context.get(jExpr.getJSONObject("guard").getString("$refText"));
                return new GuardReference(guardType, values);
            case "PropVarRef":
                String name = jExpr.getJSONObject("variable").getString("$refText");
                // Remove @
                name = name.substring(1);
                type = context.get(name);
                return new TypedVariable<Type>(type, name);
            case "AutomatonState":
                return new TypedVariable<Integer>(
                    Integer.getType(), 
                    jExpr.getJSONObject("instance").getString("$refText") + "-automaton-state");
            case "QualifiedRef":
                String instance = jExpr.getJSONObject("instance").getString("$refText");
                String variable = jExpr.getJSONObject("variable").getString("$refText");
                // !!! CAUTION
                // We cannot resolve the type of a qualified reference _here_.
                // We will do that during quantifier elimination!
                return new TypedVariable(null, instance + '-' + variable);
            case "BinExpr":
            case "CompoundExpr":
            case "Comparison":
            case "BinObs":
                Expression lhs = deserializeExpr(jExpr.getJSONObject("left"), context);
                if (!jExpr.has("operator")) {
                    return lhs;
                }
                String op = jExpr.getString("operator");
                Expression rhs = deserializeExpr(jExpr.getJSONObject("right"), context);
                switch (op) {
                    case "=": // TODO remove single =?
                    case "==":
                        return new IsEqualTo(lhs, rhs);
                    case "!=":
                        return new IsNotEqualTo(lhs, rhs);
                    case ">=":
                        return new IsGreaterOrEqualThan(lhs, rhs);
                    case "<=":
                        return new IsLessOrEqualThan(lhs, rhs);
                    case ">":
                        return new IsGreaterThan(lhs, rhs);
                    case "<":
                        return new IsLessThan(lhs, rhs);
                    case "&":
                        return new And(lhs, rhs);
                    case "|":
                        return new Or(lhs, rhs);
                    case "->":
                        return new Implies(lhs, rhs);
                    case "+":
                        return new Addition(lhs, rhs);
                    case "-":
                        return new Subtraction(lhs, rhs);
                    case "*":
                        return new Multiplication(lhs, rhs);
                    case "/":
                        return new Division(lhs, rhs);
                    default:
                        throw new ParsingException("unexpected operator in Expr: " + op);
                }
            case "SenderObs":
                Expression senderExpr = deserializeRef(jExpr.getString("sender"), context);
                return new IsEqualTo<>(new TypedVariable<Type>(Config.getAgentType(), "sender"), senderExpr);
            case "ChannelObs":
                Enum chanEnum = Enum.getEnum(Config.channelLabel);
                Expression chanExpr = deserializeRef(jExpr.getString("chan"), context);
                return new IsEqualTo<>(new TypedVariable<Type>(chanEnum, channelLabel), chanExpr);
            case "ExistsObs":
                    return new Predicate("exists", deserializeExpr(jExpr.getJSONObject("pred"), context));
            case "ForallObs":
                return new Predicate("forall", deserializeExpr(jExpr.getJSONObject("pred"), context));
            default:
                throw new ParsingException(
                        String.format("Cannot deserialize %s into Expression", jExpr.getString("$type")));
        }
    }


    public static Type deserializeQType (JSONArray jQTypes, TypingContext ctx) {
        UnionType t = new UnionType();
        for (int j = 0; j < jQTypes.length(); j++) {
            String kind = jQTypes.getJSONObject(j).getString("$refText");
            Type qType = ctx.get(kind);
            if (jQTypes.length() == 1) return qType;
            else t.addType(qType);
        }
        return t;
    }

    public static LTOL deserializeLTOL(JSONObject jPhi, TypingContext parentContext) throws Exception {
        String type = jPhi.getString("$type");
        LTOL phi;
        Expression obs;
        TypingContext ctx = new TypingContext();
        ctx.setAll(parentContext);
        ctx.set(Config.chanLabel, Enum.getEnum(Config.channelLabel));

        JSONArray jQuants = jPhi.optJSONArray("quants");
        List<TypedVariable> quantVars = new ArrayList<>();
        if (jQuants != null && jQuants.length() > 0) {
            for (int i = 0; i < jQuants.length(); i++) {
                JSONObject jq = jQuants.getJSONObject(i);
                String qName = jq.getString("name");
                Type qType = deserializeQType(jq.getJSONArray("kinds"), ctx);
                quantVars.add(new TypedVariable<Type>(qType, qName));
                ctx.set(qName, qType);
            }
            LTOL result = deserializeLTOLorExpr(jPhi.getJSONObject("expr"), ctx);
            for (int i = 0; i < jQuants.length(); i++) {
                JSONObject jq = jQuants.getJSONObject(i);
                switch (jq.getString("op")) {
                    case "forall":
                        result = new BigAnd(quantVars.get(i), result);
                        break;
                    case "exists": 
                        result = new BigOr(quantVars.get(i), result);
                    default:
                        break;
                }
            }
            return result;
        }


        switch (type) {
            case "Ltol":
                return deserializeLTOLorExpr(jPhi.getJSONObject("expr"), ctx);
            case "Globally":
                return new Globally(deserializeLTOLorExpr(jPhi.getJSONObject("expr"), ctx));
            case "Finally":
                return new Eventually(deserializeLTOLorExpr(jPhi.getJSONObject("expr"), ctx));
            case "Next":
                return new Next(deserializeLTOLorExpr(jPhi.getJSONObject("expr"), ctx));
            case "Neg":
                return new recipe.lang.ltol.Not(deserializeLTOLorExpr(jPhi.getJSONObject("expr"), ctx));
            case "CompoundExpr":
            case "Comparison":
                LTOL lhs = deserializeLTOLorExpr(jPhi.getJSONObject("left"), ctx);
                if (!jPhi.has(("operator"))) {
                    return lhs;

                }
                LTOL rhs = deserializeLTOLorExpr(jPhi.getJSONObject("right"), ctx);
                String op = jPhi.getString("operator");
                switch (op) {
                    case "&":
                        return new recipe.lang.ltol.And(lhs, rhs);
                    case "|":
                        return new recipe.lang.ltol.Or(lhs, rhs);
                    case "->":
                        return new recipe.lang.ltol.Or(new recipe.lang.ltol.Not(lhs), rhs);
                    case "<->":
                        return new recipe.lang.ltol.Iff(lhs, rhs);
                    default:
                        throw new Exception("Unexpected operator " + op);
                }
            case "Diamond":
                obs = deserializeExpr(jPhi.getJSONObject("obs"), ctx);
                phi = deserializeLTOLorExpr(jPhi.getJSONObject("expr"), ctx);
                return new Necessary(new Observation(obs), phi);
            case "Box":
                obs = deserializeExpr(jPhi.getJSONObject("obs"), ctx);
                phi = deserializeLTOLorExpr(jPhi.getJSONObject("expr"), ctx);
                return new Possibly(new Observation(obs), phi);
            case "Expr":
                try {
                    return deserializeLTOL(jPhi.getJSONObject("left"), ctx);
                }
                catch (ParsingException e) {
                    return new Atom(deserializeExpr(jPhi.getJSONObject("left"), ctx));
                }
            default:
                throw new ParsingException(String.format("Cannot deserialize %s into LTOL", type));
        }
    }

    public static LTOL deserializeLTOLorExpr(JSONObject jPhi, TypingContext ctx) throws Exception {
        try {
            Expression expr = deserializeExpr(jPhi, ctx);
            return new Atom(expr);
        } catch (Exception e) {
            return deserializeLTOL(jPhi, ctx);
        }
    }
}
