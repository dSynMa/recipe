package recipe.lang.expressions.channels;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
    public String toString(){
        return value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o){
        return o.getClass().equals(ChannelValue.class) && ((ChannelValue) o).value.equals(value);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context){
        return Parsing.disjunctiveWordParser(context.get(ChannelValue.class), (String name) -> new ChannelValue(name));
    }

    @Override
    public ChannelExpression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
        return this;
    }
}
