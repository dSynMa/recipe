package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.Store;

import java.util.List;
import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.digit;

public class NumberValue extends ArithmeticExpression implements TypedValue {
    public Number value;

    public NumberValue(Number value){
        this.value = value;
    }

    public NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException{
        return this;
    }

    public ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException{
        return this;
    }

    @Override
    public Number getValue() {
        return value;
    }

    @Override
    public String toString(){
        return value.toString();
    }

    public static org.petitparser.parser.Parser parser(){
        org.petitparser.parser.Parser decimalParser = (digit().plus().flatten().seq(CharacterParser.of('.')).seq(digit().plus().flatten()))
                .map((List<Object> values) -> {
                    return new NumberValue(Double.parseDouble(values.get(0).toString() + '.' + values.get(2)));
                });

        org.petitparser.parser.Parser integerParser = (digit().plus().flatten())
                .map((String value) -> {
                    return new NumberValue(Integer.parseInt(value));
                });

        org.petitparser.parser.Parser parser = (decimalParser
                    .or(integerParser));

        return parser;
    }
}
