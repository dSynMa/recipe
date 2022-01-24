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

public class Addition extends ArithmeticExpression{
    Expression<Number> lhs;
    Expression<Number> rhs;

    public Addition(Expression<Number> lhs, Expression<Number> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException {
        TypedValue<Number> lhsValue = lhs.valueIn(store);
        TypedValue<Number> rhsValue = rhs.valueIn(store);

        BigDecimal lhsNo = new BigDecimal(lhsValue.getValue().toString());
        BigDecimal rhsNo = new BigDecimal(rhsValue.getValue().toString());
        return new TypedValue<Number>((Number) Real.getType(), lhsNo.add(rhsNo).toString());
    }

    @Override
    public Expression<Number> close() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
        Expression<Number> lhsValue = lhs.close();
        Expression<Number> rhsValue = rhs.close();

        if (lhsValue.getClass().equals(TypedValue.class)
                && rhsValue.getClass().equals(TypedValue.class)){
            BigDecimal lhsNo = new BigDecimal(((TypedValue) lhsValue).getValue().toString());
            BigDecimal rhsNo = new BigDecimal(((TypedValue) rhsValue).getValue().toString());
            return new TypedValue<Number>((Number) Real.getType(), lhsNo.add(rhsNo).toString());
        }
        else {
            return new Addition(lhs.close(), rhs.close());
        }
    }

    @Override
    public ArithmeticExpression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Addition(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
    }

    @Override
    public String toString(){
        return "(" + lhs.toString() + " + " + rhs.toString() + ")";
    }
}
