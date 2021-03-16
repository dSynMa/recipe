package recipe.lang.expressions.predicate;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.Store;

import java.util.Set;

public class NumberValue extends ArithmeticExpression {
    public Number value;

    public NumberValue(Number value){
        this.value = value;
    }

    public NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException{
        return this;
    }

    public ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException{
        return this;
    }

}