package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Boolean;
import recipe.lang.types.Number;
import recipe.lang.store.Store;
import recipe.lang.types.Real;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public abstract class ArithmeticExpression implements Expression<Number> {
    public abstract TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException;

    public abstract Expression<Number> close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException;

    public static Parser typeParser(TypingContext context){
        return ArithmeticExpression.parser(context);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context) {
        SettableParser parser = SettableParser.undefined();
        SettableParser basic = SettableParser.undefined();
        org.petitparser.parser.Parser addition = Addition.parser(basic);
        org.petitparser.parser.Parser multiplication = Multiplication.parser(basic);
        org.petitparser.parser.Parser subtraction = Subtraction.parser(basic);
        org.petitparser.parser.Parser value = Real.getType().parser().map((String val) -> {
            try {
                return new TypedValue(Real.getType(), val);
            } catch (MismatchingTypeException e) {
                e.printStackTrace();
            }
            return null;
        });
        org.petitparser.parser.Parser variable = context.getSubContext(Real.getType()).variableParser();

        parser.set(addition
                .or(multiplication)
                .or(subtraction)
                .or(basic));

        basic.set(value
                .or(variable)
                .or((CharacterParser.of('(').trim()
                        .seq(parser)
                        .seq(CharacterParser.of(')')))
                        .map((List<Object> values) -> values.get(1))));

        return parser;
    }

    @Override
    public abstract Expression<Number> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException;

    @Override
    public int hashCode(){
        return Objects.hash(this.toString());
    }
}
