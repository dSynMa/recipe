package recipe.lang.expressions.channels;

import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.Store;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChannelValue extends ChannelExpression implements TypedValue {
    public String value;

    public ChannelValue(String value){
        this.value = value;
    }

    public ChannelValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException{
        return this;
    }

    public ChannelExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException{
        return this;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static org.petitparser.parser.Parser parser(List<String> channelsVals){
        if(channelsVals.size() == 0){
            return StringParser.of("").not();
        }

        org.petitparser.parser.Parser parser = StringParser.of(channelsVals.get(0)).map((String value) -> new ChannelValue(value));
        for(int i = 1; i < channelsVals.size(); i++){
            parser = parser.or(StringParser.of(channelsVals.get(i)).map((String value) -> new ChannelValue(value)));
        }

        return parser;
    }
}
