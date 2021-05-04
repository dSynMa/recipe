package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Number;
import recipe.lang.types.Real;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class Multiplication extends ArithmeticExpression{
    Expression<Number> lhs;
    Expression<Number> rhs;

    public Multiplication(Expression<Number> lhs, Expression<Number> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException {
        TypedValue<Number> lhsValue = lhs.valueIn(store);
        TypedValue<Number> rhsValue = rhs.valueIn(store);

        BigDecimal lhsNo = new BigDecimal(lhsValue.getValue().toString());
        BigDecimal rhsNo = new BigDecimal(rhsValue.getType().toString());
        return new TypedValue<Number>((Number) Real.getType(), lhsNo.multiply(rhsNo).toString());
    }

    @Override
    public Expression<Number> close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException {
        return new Multiplication(lhs.close(store, CV), rhs.close(store, CV));
    }

    @Override
    public ArithmeticExpression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
        return new Multiplication(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
    }

    @Override
    public String toString(){
        return "(" + lhs.toString() + " * " + rhs.toString() + ")";
    }

    public static org.petitparser.parser.Parser parser(Parser basicArithmeticExpression) {
        org.petitparser.parser.Parser parser =
                (basicArithmeticExpression)
                        .seq(CharacterParser.of('*').trim())
                        .seq(basicArithmeticExpression)
                        .map((List<Object> values) -> {
                            return new Multiplication((Expression<Number>) values.get(0), (Expression<Number>) values.get(2));
                        });
        return parser;
    }
}
