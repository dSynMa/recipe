package recipe.lang.expressions.arithmetic;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.predicate.BooleanValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.NumberValue;
import recipe.lang.store.Store;

import java.util.Set;

public class MyNumberVariable extends ArithmeticExpression {
    String name;

    public MyNumberVariable(String name) {
        this.name = name;
    }

    @Override
    public NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        Object o = store.getValue(name);
        if (o == null) {
            throw new AttributeNotInStoreException();
        } else if(!Number.class.isAssignableFrom(o.getClass())){
            throw new AttributeTypeException();
        }

        return new NumberValue((Number) o);
    }

    @Override
    public ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
        if (CV.contains(name)) {
            return this.valueIn(store);
        } else {
            return this;
        }
    }
}
