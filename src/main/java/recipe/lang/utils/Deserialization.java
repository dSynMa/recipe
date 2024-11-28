package recipe.lang.utils;


import org.json.JSONObject;

import recipe.Config;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.Addition;
import recipe.lang.expressions.arithmetic.Division;
import recipe.lang.expressions.arithmetic.Multiplication;
import recipe.lang.expressions.arithmetic.Subtraction;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.predicate.Or;
import recipe.lang.expressions.predicate.Implies;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.lang.expressions.predicate.IsGreaterOrEqualThan;
import recipe.lang.expressions.predicate.IsGreaterThan;
import recipe.lang.expressions.predicate.IsLessOrEqualThan;
import recipe.lang.expressions.predicate.IsLessThan;
import recipe.lang.expressions.predicate.IsNotEqualTo;
import recipe.lang.expressions.predicate.Not;
import recipe.lang.types.Boolean;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Enum;
import recipe.lang.types.Integer;
import recipe.lang.types.Type;
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
        }
        else if (jObj.has("rangeType")) {
            JSONObject jRange = jObj.getJSONObject("rangeType");
            return new BoundedInteger(jRange.getInt("lower"), jRange.getInt("upper"));
        }
        else if (jObj.has("builtinType")) {
            String bt = jObj.getString("builtinType");
            switch (bt) {
                case "int":
                    return recipe.lang.types.Integer.getType();
                case "bool":
                    return recipe.lang.types.Boolean.getType();
                case "channel":
                    return Enum.getEnum(Config.channelLabel);
                default:
                    throw new Exception("Unexpected builtinType " + bt);
            }
        }
        else {
            throw new ParsingException("Unexpected type for " + jObj.optString("name"));
        }
    }


    public static Expression deserializeExpr(JSONObject jExpr, TypingContext context) throws MismatchingTypeException, ParsingException, Exception {
        switch (jExpr.getString("$type")) {
            case "NumberLiteral":
                int literal = jExpr.getInt("value");
                return new TypedValue<Integer>(Integer.getType(), String.valueOf(literal));
            case "BoolLiteral":
                return new TypedValue<Boolean>(recipe.lang.types.Boolean.getType(), jExpr.optString("value"));
            case "Broadcast":
                return new TypedVariable<Type>(Enum.getEnum(Config.channelLabel), Config.broadcast);
            case "Ref":
                if (jExpr.has("variable")){
                    String name = jExpr.getJSONObject("variable").getString("$refText");
                    return new TypedVariable<Type>(context.get(name), name);
                }
                else if (jExpr.has("currentChannel")) {
                    return new TypedVariable<Type>(Enum.getEnum(Config.channelLabel), jExpr.getString("currentChannel"));
                }
            case "Neg":
                return new Not(deserializeExpr(jExpr.getJSONObject("expr"), context));
            case "Expr":
                return deserializeExpr(jExpr.getJSONObject("left"), context);
            case "BinExpr":
                String op = jExpr.getString("operator");
                Expression lhs = deserializeExpr(jExpr.getJSONObject("left"), context);
                Expression rhs = deserializeExpr(jExpr.getJSONObject("right"), context);
                switch (op) {
                    case "=": //TODO remove single =?
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
            
            default:
                throw new ParsingException(String.format("Cannot deserialize %s into Expression", jExpr.getString("$type")));
        }
    }
}
