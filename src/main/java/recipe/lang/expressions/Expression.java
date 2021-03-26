package recipe.lang.expressions;

import org.petitparser.parser.Parser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.strings.StringExpression;
import recipe.lang.store.Store;
import recipe.lang.utils.TypingContext;

import java.util.Set;

public abstract class Expression {
    public abstract Expression valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    public abstract Expression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;
}
