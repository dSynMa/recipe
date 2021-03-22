package recipe.lang.expressions.strings;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.store.Store;

import java.util.Set;

public class StringValue extends StringExpression implements TypedValue {
    @Override
    public String getValue() {
        return value;
    }

    String value;

    public StringValue(String value){
        this.value = value;
    }

    @Override
    public StringValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException{
        return this;
    }

    @Override
    public StringExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException{
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
