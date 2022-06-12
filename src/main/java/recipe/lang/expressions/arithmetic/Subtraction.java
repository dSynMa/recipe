package recipe.lang.expressions.arithmetic;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Number;
import recipe.lang.types.Real;
import recipe.lang.utils.exceptions.*;

import java.math.BigDecimal;
import java.util.function.Function;

public class Subtraction extends ArithmeticExpression{
    Expression<Number> lhs;
    Expression<Number> rhs;

    public Subtraction(Expression<Number> lhs, Expression<Number> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, NotImplementedYetException {
        TypedValue<Number> lhsValue = lhs.valueIn(store);
        TypedValue<Number> rhsValue = rhs.valueIn(store);

        if(lhsValue.getValue().getClass().equals(Number.class) &&
                rhsValue.getValue().getClass().equals(Number.class)){
            BigDecimal lhsNo = new BigDecimal(lhsValue.getValue().toString());
            BigDecimal rhsNo = new BigDecimal(rhsValue.getValue().toString());
            return new TypedValue<Number>((Number) Real.getType(), lhsNo.subtract(rhsNo).toString());
        }
        throw new AttributeTypeException();
    }

    @Override
    public Expression<Number> close() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
        Expression<Number> lhsValue = lhs.close();
        Expression<Number> rhsValue = rhs.close();

        if (lhsValue.getClass().equals(TypedValue.class)
                && rhsValue.getClass().equals(TypedValue.class)){
            BigDecimal lhsNo = new BigDecimal(((TypedValue) lhsValue).getValue().toString());
            BigDecimal rhsNo = new BigDecimal(((TypedValue) rhsValue).getValue().toString());
            return new TypedValue<Number>((Number) Real.getType(), lhsNo.subtract(rhsNo).toString());
        }
        else {
            return new Subtraction(lhs.close(), rhs.close());
        }
    }

    @Override
    public Expression<Number> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Subtraction(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
    }

    @Override
    public String toString(){
        return "(" + lhs.toString() + " - " + rhs.toString() + ")";
    }
}
