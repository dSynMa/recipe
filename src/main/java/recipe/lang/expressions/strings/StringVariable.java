package recipe.lang.expressions.strings;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;

import java.util.Set;

public class StringVariable extends StringExpression implements TypedVariable {
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
    public StringExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
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
        return val.getClass().equals(StringValue.class);
    }
}
