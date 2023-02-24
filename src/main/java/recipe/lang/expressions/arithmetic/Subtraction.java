package recipe.lang.expressions.arithmetic;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Number;
import recipe.lang.types.Integer;
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

        try {
            BigDecimal lhsNo = new BigDecimal(lhsValue.getValue().toString());
            BigDecimal rhsNo = new BigDecimal(rhsValue.getValue().toString());
            BigDecimal result = lhsNo.subtract(rhsNo);
            Number resultType = isInteger(result) ? Integer.getType() : Real.getType();

            return new TypedValue<Number>(resultType, result.toString());
        } catch (NumberFormatException e) {
            System.out.printf("%s %s\n", lhsValue.toString(), lhsValue.getValue().getClass().toString());
            System.out.printf("%s %s\n", rhsValue.toString(), rhsValue.getValue().getClass().toString());
            throw new AttributeTypeException();
        }

        // if(lhsValue.getValue() instanceof Number && rhsValue.getValue() instanceof Number){
        //     BigDecimal lhsNo = new BigDecimal(lhsValue.getValue().toString());
        //     BigDecimal rhsNo = new BigDecimal(rhsValue.getValue().toString());
        //     return new TypedValue<Number>((Number) Real.getType(), lhsNo.subtract(rhsNo).toString());
        // }
        
    }

    @Override
    public Expression<Number> simplify() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
        Expression<Number> lhsValue = lhs.simplify();
        Expression<Number> rhsValue = rhs.simplify();

        if (lhsValue.getClass().equals(TypedValue.class)
                && rhsValue.getClass().equals(TypedValue.class)){
            BigDecimal lhsNo = new BigDecimal(((TypedValue) lhsValue).getValue().toString());
            BigDecimal rhsNo = new BigDecimal(((TypedValue) rhsValue).getValue().toString());
            return new TypedValue<Number>((Number) Real.getType(), lhsNo.subtract(rhsNo).toString());
        }
        else {
            return new Subtraction(lhs.simplify(), rhs.simplify());
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
