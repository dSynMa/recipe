package recipe.lang.expressions.arithmetic;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.predicate.NumberValue;
import recipe.lang.store.Store;

import java.math.BigDecimal;
import java.util.Set;

public class Multiplication extends ArithmeticExpression{
    ArithmeticExpression lhs;
    ArithmeticExpression rhs;

    public Multiplication(ArithmeticExpression lhs, ArithmeticExpression rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        NumberValue lhsValue = lhs.valueIn(store);
        NumberValue rhsValue = rhs.valueIn(store);

        BigDecimal lhsNo = new BigDecimal(lhsValue.value.toString());
        BigDecimal rhsNo = new BigDecimal(rhsValue.value.toString());
        return new NumberValue(lhsNo.multiply(rhsNo));
    }

    @Override
    public ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException {
        return new Multiplication(lhs.close(store, CV), rhs.close(store, CV));
    }
}
