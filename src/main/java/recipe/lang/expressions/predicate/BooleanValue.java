package recipe.lang.expressions.predicate;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Store;

import java.util.Set;

public class BooleanValue extends Condition{

    public BooleanValue(Boolean val){
        super(val ? PredicateType.TRUE : PredicateType.FALSE);
    }

    public BooleanValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException{
        return this;
    }

    public Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException{
        return this;
    }

}
