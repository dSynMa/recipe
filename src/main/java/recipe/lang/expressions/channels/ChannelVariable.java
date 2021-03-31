package recipe.lang.expressions.channels;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.strings.StringExpression;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.store.Store;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class ChannelVariable extends ChannelExpression implements TypedVariable {
    String name;

    public ChannelVariable(String name) {
        this.name = name;
    }

    @Override
    public ChannelValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        Object o = store.getValue(name);
        if (o == null) {
            throw new AttributeNotInStoreException();
        } else if(!o.getClass().equals(ChannelValue.class)){
            throw new AttributeTypeException();
        }

        return (ChannelValue) o;
    }

    @Override
    public ChannelExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
        if (!CV.contains(name)) {
            return this.valueIn(store);
        } else {
            return this;
        }
    }

    @Override
    public boolean equals(Object o){
        return o.getClass().equals(ChannelVariable.class) && name.equals(o);
    }

    @Override
    public String toString(){
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Boolean isValidValue(TypedValue val) {
        return val.getClass().equals(ChannelValue.class);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context){
        return Parsing.disjunctiveWordParser(context.get(ChannelVariable.class), (String name) -> new ChannelVariable(name));
    }

    @Override
    public ChannelExpression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
        Expression result = relabelling.apply(this);
        if(!ChannelExpression.class.isAssignableFrom(result.getClass())){
            throw new RelabellingTypeException();
        } else{
            return (ChannelExpression) relabelling.apply( this);
        }
    }

}
