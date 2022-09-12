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

public class Division extends ArithmeticExpression{
    Expression<Number> lhs;
    Expression<Number> rhs;

    public Division(Expression<Number> lhs, Expression<Number> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, NotImplementedYetException {
        TypedValue<Number> lhsValue = lhs.valueIn(store);
        TypedValue<Number> rhsValue = rhs.valueIn(store);

        BigDecimal lhsNo = new BigDecimal(lhsValue.getValue().toString());
        BigDecimal rhsNo = new BigDecimal(rhsValue.getType().toString());
        return new TypedValue<Number>((Number) Real.getType(), lhsNo.divide(rhsNo).toString());
    }

    @Override
    public Expression<Number> simplify() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
        Expression<Number> lhsValue = lhs.simplify();
        Expression<Number> rhsValue = rhs.simplify();

        if (lhsValue.getClass().equals(TypedValue.class)
                && rhsValue.getClass().equals(TypedValue.class)){
            BigDecimal lhsNo = new BigDecimal(((TypedValue) lhsValue).getValue().toString());
            BigDecimal rhsNo = new BigDecimal(((TypedValue) rhsValue).getValue().toString());
            return new TypedValue<Number>((Number) Real.getType(), lhsNo.divide(rhsNo).toString());
        }
        else {
            return new Division(lhs.simplify(), rhs.simplify());
        }
    }

    @Override
    public ArithmeticExpression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Division(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
    }

    @Override
    public String toString(){
        return "(" + lhs.toString() + " / " + rhs.toString() + ")";
    }
}
