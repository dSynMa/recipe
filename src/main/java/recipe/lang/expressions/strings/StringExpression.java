package recipe.lang.expressions.strings;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.store.Store;

import java.util.Set;

public abstract class StringExpression extends Expression {
    public abstract StringValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    public abstract StringExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;
}
