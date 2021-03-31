package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.store.Store;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Set;

public abstract class ArithmeticExpression implements Expression {
    public abstract NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;

    public abstract ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;

    public static Parser typeParser(TypingContext context){
        return ArithmeticExpression.parser(context);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context) {
        SettableParser parser = SettableParser.undefined();
        SettableParser basic = SettableParser.undefined();
        org.petitparser.parser.Parser addition = Addition.parser(basic);
        org.petitparser.parser.Parser multiplication = Multiplication.parser(basic);
        org.petitparser.parser.Parser subtraction = Subtraction.parser(basic);
        org.petitparser.parser.Parser value = NumberValue.parser();
        org.petitparser.parser.Parser variable = NumberVariable.parser(context);

        parser.set(addition
                .or(multiplication)
                .or(subtraction)
                .or(basic)
                .or(value)
                .or(variable));

        basic.set(value
                .or(variable)
                .or((CharacterParser.of('(').trim()
                        .seq(parser)
                        .seq(CharacterParser.of(')'))).map((List<Object> values) -> values.get(1))));

        return parser;
    }
}
