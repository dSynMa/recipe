package recipe.lang.expressions;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.store.Store;

import java.util.Set;
import java.util.function.Function;

public interface Expression {
    TypedValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    Expression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;
    Expression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException;
}
