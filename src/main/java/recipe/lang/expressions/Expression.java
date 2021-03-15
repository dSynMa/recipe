package recipe.lang.expressions;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Store;

import java.util.Set;

public abstract class Expression {
    public abstract Expression valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    public abstract Expression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;
}
