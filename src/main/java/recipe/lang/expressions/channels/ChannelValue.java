package recipe.lang.expressions.channels;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.Store;

import java.util.Set;

public class ChannelValue extends ChannelExpression implements TypedValue {
    public String value;

    public ChannelValue(String value){
        this.value = value;
    }

    public ChannelValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException{
        return this;
    }

    public ChannelExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException{
        return this;
    }

    @Override
    public String getValue() {
        return value;
    }
}
