package recipe.lang.expressions;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.predicate.NumberValue;
import recipe.lang.store.Store;

import java.util.Set;

public class StringVariable extends Expression {
    String name;

    public StringVariable(String name) {
        this.name = name;
    }

    @Override
    public StringValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        Object o = store.getValue(name);
        if (o == null) {
            throw new AttributeNotInStoreException();
        } else if(!String.class.isAssignableFrom(o.getClass())){
            throw new AttributeTypeException();
        }

        return new StringValue((String) o);
    }

    @Override
    public Expression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
        if (!CV.contains(name)) {
            return this.valueIn(store);
        } else {
            return this;
        }
    }
}
