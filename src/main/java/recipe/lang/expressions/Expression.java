package recipe.lang.expressions;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Store;

import java.util.Set;

public interface Expression {
    Expression valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    Expression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;
}
