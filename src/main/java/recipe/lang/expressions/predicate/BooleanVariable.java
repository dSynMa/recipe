package recipe.lang.expressions.predicate;

import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.expressions.strings.StringVariable;
import recipe.lang.store.Store;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class BooleanVariable extends Condition implements TypedVariable {
    String name;

    public BooleanVariable(String name) {
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
    public Boolean isValidValue(TypedValue val) {
        return val.getClass().equals(BooleanValue.class);
    }


    @Override
    public String toString(){
        return name;
    }

    @Override
    public boolean equals(Object o){
        return o.getClass().equals(BooleanVariable.class) && name.equals(o);
    }

    public static org.petitparser.parser.Parser parser(TypingContext context){
        return Parsing.disjunctiveWordParser(context.get(BooleanVariable.class), (String name) -> new BooleanVariable(name));
    }
}
