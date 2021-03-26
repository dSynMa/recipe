package recipe.lang.expressions.strings;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.store.Store;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class StringVariable extends StringExpression implements TypedVariable {
    String name;

    public StringVariable(String name) {
        this.name = name;
    }

    @Override
    public StringValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
        Object o = store.getValue(name);
        if (o == null) {
            throw new AttributeNotInStoreException();
        } else if(!String.class.isAssignableFrom(o.getClass())){
            throw new AttributeTypeException();
        }

        return new StringValue((String) o);
    }

    @Override
    public StringExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
        if (!CV.contains(name)) {
            return this.valueIn(store);
        } else {
            return this;
        }
    }

    @Override
    public boolean equals(Object o){
        return o.getClass().equals(StringVariable.class) && name.equals(o);
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
        return val.getClass().equals(StringValue.class);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context){
        return Parsing.disjunctiveWordParser(context.get(StringVariable.class), (String name) -> new StringVariable(name));
    }
}
