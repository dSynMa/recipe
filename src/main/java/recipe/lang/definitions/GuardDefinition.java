package recipe.lang.definitions;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;
import recipe.lang.types.Guard;
import recipe.lang.types.Type;
import recipe.lang.utils.LazyParser;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class GuardDefinition extends Definition<Expression<Boolean>> {
    private Guard type;

    public GuardDefinition(Guard guardType, Expression<Boolean> expression) {
        super(guardType.name(), guardType.getParameters(), expression);
        this.type = guardType;
    }

    public static org.petitparser.parser.Parser parser(TypingContext context){
        AtomicReference<TypingContext> contextHere = new AtomicReference<>(TypingContext.union(context, new TypingContext()));
        AtomicReference<Guard> guarddef = new AtomicReference<>(null);

        Parser parser = (StringParser.of("guard").trim()
                .seq(CharacterParser.word().plus().flatten())
                .seq(CharacterParser.of('(').trim())
                .seq(Parsing.typedVariableList().map((Map<String, Type> typedVars) -> {
                    List<TypedVariable> params = new ArrayList<>();
                    for(Map.Entry<String, Type> typedVariable : typedVars.entrySet()){
                        contextHere.get().set(typedVariable.getKey(), typedVariable.getValue());
                        params.add(new TypedVariable(typedVariable.getValue(), typedVariable.getKey()));
                    }
                    return params.toArray(new TypedVariable[0]);
                }))
                .seq(CharacterParser.of(')').trim()))
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
                .seq(new LazyParser<>((AtomicReference<Guard> guardDef) -> {
                    try {
                        return guardDef.get().valueParser();
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
