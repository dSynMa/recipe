package recipe.lang.expressions.predicate;

import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberValue;
import recipe.lang.store.Store;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.petitparser.parser.primitive.CharacterParser.digit;

public class BooleanValue extends Condition implements TypedValue {
    public Boolean getValue() {
        return value;
    }

    Boolean value;

    public BooleanValue(Boolean val){
        super(val ? PredicateType.TRUE : PredicateType.FALSE);
        value = val;
    }

    public BooleanValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException{
        return this;
    }

    public Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException{
        return this;
    }

    @Override
    public boolean equals(Object o){
        if(o.getClass().equals(BooleanValue.class)){
            return this.value.equals(((BooleanValue) o).value);
        }

        return false;
    }

    @Override
    public String toString(){
        return value.toString();
    }

    public static org.petitparser.parser.Parser parser(){
        org.petitparser.parser.Parser parser =
                (StringParser.of("true").map((String value) -> new BooleanValue(true)))
                .or(StringParser.of("false").map((String value) -> new BooleanValue(false)));

        return parser;
    }

    @Override
    public Condition relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
        return this;
    }
}
