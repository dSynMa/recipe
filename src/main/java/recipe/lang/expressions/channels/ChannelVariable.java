package recipe.lang.expressions.channels;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.strings.StringExpression;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.store.Store;

import java.util.Set;

public class ChannelVariable extends ChannelExpression implements TypedVariable {
    String name;

    public ChannelVariable(String name) {
        this.name = name;
    }

    @Override
    public ChannelValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        Object o = store.getValue(name);
        if (o == null) {
            throw new AttributeNotInStoreException();
        } else if(!o.getClass().equals(ChannelValue.class)){
            throw new AttributeTypeException();
        }

        return (ChannelValue) o;
    }

    @Override
    public ChannelExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
        if (!CV.contains(name)) {
            return this.valueIn(store);
        } else {
            return this;
        }
    }

    @Override
    public boolean equals(Object o){
        return name.equals(o);
    }

    @Override
    public String toString(){
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Boolean isValidValue(TypedValue val) {
        return val.getClass().equals(ChannelValue.class);
    }
}