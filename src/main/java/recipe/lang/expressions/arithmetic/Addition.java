package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberValue;
import recipe.lang.store.Store;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class Addition extends ArithmeticExpression{
    ArithmeticExpression lhs;
    ArithmeticExpression rhs;

    public Addition(ArithmeticExpression lhs, ArithmeticExpression rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        NumberValue lhsValue = lhs.valueIn(store);
        NumberValue rhsValue = rhs.valueIn(store);

        BigDecimal lhsNo = new BigDecimal(lhsValue.value.toString());
        BigDecimal rhsNo = new BigDecimal(rhsValue.value.toString());
        return new NumberValue(lhsNo.add(rhsNo));
    }

    @Override
    public ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
        return new Addition(lhs.close(store, CV), rhs.close(store, CV));
    }

    @Override
    public ArithmeticExpression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
        return new Addition(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
    }

    @Override
    public String toString(){
        return "(" + lhs.toString() + "+" + rhs.toString() + ")";
    }

    public static org.petitparser.parser.Parser parser(Parser basicArithmeticExpression) {
        org.petitparser.parser.Parser parser =
                (basicArithmeticExpression)
                .seq(CharacterParser.of('+').trim())
                .seq(basicArithmeticExpression)
                .map((List<Object> values) -> {
                    return new Addition((ArithmeticExpression) values.get(0), (ArithmeticExpression) values.get(2));
                });

        return parser;
    }
}
