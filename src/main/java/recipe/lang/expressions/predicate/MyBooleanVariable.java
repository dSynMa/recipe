package recipe.lang.expressions.predicate;

import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.store.Store;

import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class MyBooleanVariable extends Condition{
    String name;

    public MyBooleanVariable(String name) {
        super(PredicateType.VAR);
        this.name = name;
    }

    @Override
    public BooleanValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        Object o = store.getValue(name);
        if (o == null) {
            throw new AttributeNotInStoreException();
        } else if(!o.getClass().equals(Boolean.class)){
            throw new AttributeTypeException();
        }

        return new BooleanValue((Boolean) o);
    }

    @Override
    public Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
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
        org.petitparser.parser.Parser parser = (StringParser.of("my.").seq(word().plus())).flatten().map((String value) -> new MyBooleanVariable(value));

        return parser;
    }
}
