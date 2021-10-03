package recipe.lang.expressions;

import recipe.lang.exception.*;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.store.Store;
import recipe.lang.types.Type;

import java.lang.reflect.ParameterizedType;
import java.util.Set;
import java.util.function.Function;

public interface Expression<T extends Type> {
    TypedValue<T> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException;
    Expression<T> close() throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, TypeCreationException, RelabellingTypeException;
    Expression<T> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException;

    Boolean isValidAssignmentFor(TypedVariable var);

    Type getType();
}
