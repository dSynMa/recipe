package recipe.lang.expressions;

import recipe.lang.store.Store;
import recipe.lang.types.Type;
import recipe.lang.utils.exceptions.*;

import java.util.function.Function;

public interface Expression<T extends Type> {
    TypedValue<T> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, NotImplementedYetException;
    Expression<T> simplify() throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, TypeCreationException, RelabellingTypeException;
    Expression<T> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException;

    Boolean isValidAssignmentFor(TypedVariable var);

    Type getType();
}
