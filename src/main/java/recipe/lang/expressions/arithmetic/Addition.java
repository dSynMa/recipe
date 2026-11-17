package recipe.lang.expressions.arithmetic;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.And;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Number;
import recipe.lang.types.Boolean;
import recipe.lang.types.Real;
import recipe.lang.types.Integer;

import recipe.lang.utils.exceptions.*;

import java.math.BigDecimal;
import java.util.function.Function;

public class Addition extends ArithmeticExpression{
    Expression<Number> lhs;
    Expression<Number> rhs;

    public Addition(Expression<Number> lhs, Expression<Number> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public Expression<Boolean> definednessCondition()
            throws RelabellingTypeException, MismatchingTypeException {
        return new And(lhs.definednessCondition(), rhs.definednessCondition());
    }

    @Override
    public TypedValue<Number> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, NotImplementedYetException {
        TypedValue<Number> lhsValue = lhs.valueIn(store);
        TypedValue<Number> rhsValue = rhs.valueIn(store);

        BigDecimal lhsNo = new BigDecimal(lhsValue.getValue().toString());
        BigDecimal rhsNo = new BigDecimal(rhsValue.getValue().toString());
        BigDecimal result = lhsNo.add(rhsNo);
        return numericResult(result);
    }

    @Override
    public Expression<Number> simplify() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
        Expression<Number> lhsValue = lhs.simplify();
        Expression<Number> rhsValue = rhs.simplify();

        if (lhsValue.getClass().equals(TypedValue.class)
                && rhsValue.getClass().equals(TypedValue.class)){
            BigDecimal lhsNo = new BigDecimal(((TypedValue) lhsValue).getValue().toString());
            BigDecimal rhsNo = new BigDecimal(((TypedValue) rhsValue).getValue().toString());
            return numericResult(lhsNo.add(rhsNo));
        }
        else {
            return new Addition(lhs.simplify(), rhs.simplify());
        }
    }

    @Override
    public ArithmeticExpression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Addition(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
    }

    @Override
    public String toNuXmv() {
        return "(" + lhs.toNuXmv() + " + " + rhs.toNuXmv() + ")";
    }

    @Override
    public String toString(){
        return "(" + lhs.toString() + " + " + rhs.toString() + ")";
    }
}
