package recipe.lang.expressions.channels;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.store.Store;

import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.word;

public abstract class ChannelExpression implements Expression {
    public abstract ChannelValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    public abstract ChannelExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;

    public static org.petitparser.parser.Parser parser(Set<String> channelVals){
        org.petitparser.parser.Parser parser = ChannelVariable.parser().or(ChannelValue.parser(channelVals));

        return parser;
    }
}
