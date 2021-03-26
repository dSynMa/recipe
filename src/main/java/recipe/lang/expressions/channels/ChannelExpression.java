package recipe.lang.expressions.channels;

import org.petitparser.parser.Parser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.store.Store;
import recipe.lang.utils.TypingContext;

import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.word;

public abstract class ChannelExpression implements Expression {
    public abstract ChannelValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    public abstract ChannelExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;

    public static Parser typeParser(TypingContext context){
        return ChannelExpression.parser(context);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context){
        org.petitparser.parser.Parser parser = ChannelVariable.parser(context).or(ChannelValue.parser(context));

        return parser;
    }
}
