package recipe.lang.expressions.arithmetic;

import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.expressions.strings.StringVariable;
import recipe.lang.store.Store;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.digit;
import static org.petitparser.parser.primitive.CharacterParser.word;

public class NumberVariable extends ArithmeticExpression implements TypedVariable {
    String name;

    public NumberVariable(String name) {
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
        if (!CV.contains(name)) {
            return this.valueIn(store);
        } else {
            return this;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString(){
        return name;
    }

    @Override
    public boolean equals(Object o){
        return o.getClass().equals(NumberVariable.class) && name.equals(o);
    }

    @Override
    public Boolean isValidValue(TypedValue val) {
        return val.getClass().equals(NumberValue.class);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context){
        return Parsing.disjunctiveWordParser(context.get(NumberVariable.class), (String name) -> new NumberVariable(name));
    }
}
