package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.store.Store;

import java.util.List;
import java.util.Set;

public abstract class ArithmeticExpression implements Expression {
    public abstract NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;

    public abstract ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;

    public static org.petitparser.parser.Parser parser() {
        SettableParser parser = SettableParser.undefined();
        SettableParser bracketed = SettableParser.undefined();
        org.petitparser.parser.Parser addition = Addition.parser(bracketed);
        org.petitparser.parser.Parser multiplication = Multiplication.parser(bracketed);
        org.petitparser.parser.Parser subtraction = Subtraction.parser(bracketed);
        org.petitparser.parser.Parser value = NumberValue.parser();
        org.petitparser.parser.Parser variable = NumberVariable.parser();
        org.petitparser.parser.Parser myVariable = MyNumberVariable.parser();

        parser.set(addition
                .or(multiplication)
                .or(subtraction)
                .or(bracketed)
                .or(value)
                .or(variable)
                .or(myVariable));

        bracketed.set((CharacterParser.of('(').trim().seq(parser).seq(CharacterParser.of(')')).map((List<Object> values) -> values.get(1)))
                .or(NumberValue.parser())
                .or(NumberVariable.parser()));


        return parser;
    }
}
