package recipe.lang.expressions.arithmetic;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.NumberValue;
import recipe.lang.store.Store;

import java.util.Set;

public abstract class ArithmeticExpression extends Expression {
    public abstract NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    public abstract ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;
}
