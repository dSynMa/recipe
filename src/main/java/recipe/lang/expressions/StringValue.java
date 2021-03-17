package recipe.lang.expressions;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.Store;

import java.util.Set;

public class StringValue extends Expression {
    public String value;

    public StringValue(String value){
        this.value = value;
    }

    @Override
    public StringValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException{
        return this;
    }

    @Override
    public Expression close(Store store, Set<String> CV) throws AttributeNotInStoreException{
        return this;
    }

    @Override
    public boolean equals(Object o){
        if(o.getClass().equals(StringValue.class)){
            return value.equals(((StringValue) o).value);
        }

        return false;
    }

    @Override
    public String toString(){
        return value;
    }
}
