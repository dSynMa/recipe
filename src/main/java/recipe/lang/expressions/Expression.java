package recipe.lang.expressions;

import recipe.lang.exception.*;
import recipe.lang.store.Store;
import recipe.lang.types.Type;

import java.util.Set;
import java.util.function.Function;

public interface Expression<T extends Type> {
    TypedValue<T> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException;
    Expression<T> close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, TypeCreationException;
    Expression<T> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException;
}
