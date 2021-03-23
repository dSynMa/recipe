package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.predicate.BooleanValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.arithmetic.NumberValue;
import recipe.lang.store.Store;

import java.util.List;
import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class MyNumberVariable extends ArithmeticExpression {
    String name;

    public MyNumberVariable(String name) {
        this.name = name;
    }

    @Override
    public NumberValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        Object o = store.getValue(name);
        if (o == null) {
            throw new AttributeNotInStoreException();
        } else if(!Number.class.isAssignableFrom(o.getClass())){
            throw new AttributeTypeException();
        }

        return new NumberValue((Number) o);
    }

    @Override
    public ArithmeticExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
        if (CV.contains(name)) {
            return this.valueIn(store);
        } else {
            return this;
        }
    }

    @Override
    public String toString(){
        return "my." + name;
    }

    public static org.petitparser.parser.Parser parser(){
        org.petitparser.parser.Parser parser = StringParser.of("my.").seq(word().plus().flatten()).map((String value) -> new MyNumberVariable(value));

        return parser;
    }
}
