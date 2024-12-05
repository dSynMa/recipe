package recipe.lang.definitions;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONObject;
import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.FailureParser;
import org.petitparser.parser.primitive.StringParser;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Boolean;
import recipe.lang.types.Guard;
import recipe.lang.types.Type;
import recipe.lang.utils.Deserialization;
import recipe.lang.utils.LazyParser;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.ParsingException;
import recipe.lang.utils.exceptions.TypeCreationException;

public class GuardDefinition extends Definition<Expression<Boolean>> {
    private Guard type;

    public GuardDefinition(Guard guardType, Expression<Boolean> expression) {
        super(guardType.name(), guardType.getParameters(), expression);
        this.type = guardType;
    }

    public static GuardDefinition deserialize(JSONObject jGuard, TypingContext parentContext) throws ParsingException,Exception {
        Deserialization.checkType(jGuard, "Guard");
        String name = jGuard.getString("name");
        TypedVariable[] params;
        if (jGuard.has("params")) {
            JSONArray jParams = jGuard.getJSONArray("params");
            params = new TypedVariable[jParams.length()];
            for (int i=0; i<jParams.length(); i++) {
                JSONObject jParam = jParams.getJSONObject(i);
                Type t = Deserialization.deserializeType(jParam);
                params[i] = new TypedVariable(t, jParam.getString("name"));
            }
        }
        else {
            params = new TypedVariable[0];
        }
        Guard guard = new Guard(name, params);
        TypingContext ctx = new TypingContext();
        if (parentContext != null) {
            ctx.setAll(parentContext);
        }
        for (TypedVariable tv: params) {
            ctx.set(tv.getName(), tv.getType());
        }
        Expression body = Deserialization.deserializeExpr(jGuard.getJSONObject("body"), ctx);
        return new GuardDefinition(guard, body);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context){
        // AtomicReference<TypingContext> contextHere = new AtomicReference<>(TypingContext.union(context, new TypingContext()));
        AtomicReference<Guard> guarddef = new AtomicReference<>(null);

        Parser parser = (StringParser.of("guard").trim()
                .seq(CharacterParser.noneOf("(").plus().flatten().or(FailureParser.withMessage("Error in guard definition name. Missing name?")))
                .seq(CharacterParser.of('(').trim().or(FailureParser.withMessage("Error in guard definition name. Must have a (possibly empty) parameter list enclosed in round brackets.")))
                .seq(Parsing.typedVariableList().map((List<TypedVariable> typedVars) -> {
                    return typedVars.toArray(new TypedVariable[0]);
                }).trim().optional(new TypedVariable[0]))
                .seq(CharacterParser.of(')').trim().or(FailureParser.withMessage("Error in guard definition name. Must have a (possibly empty) parameter list enclosed in round brackets."))))
                .mapWithSideEffects((List<Object> values) -> {
                    Guard guardType =
                            null;
                    try {
                        guardType = new Guard((String) values.get(1), (TypedVariable[]) values.get(3));
                    } catch (TypeCreationException e) {
                        e.printStackTrace();
                    }
                    guarddef.set(guardType);

                    return guardType;
                })
                .seq(StringParser.of(":=").trim())
                .seq(new LazyParser<AtomicReference<Guard>>((AtomicReference<Guard> guardDef) -> {
                    try {
                        return guardDef.get().valueParser(context);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }, guarddef))
                .seq(CharacterParser.of(';').trim())
                .map((List<Object> values) -> {
                    return new GuardDefinition((Guard) values.get(0), (Expression<Boolean>) values.get(2));
                });

        return parser;
    }

    public Guard getType() {
        return type;
    }
}
