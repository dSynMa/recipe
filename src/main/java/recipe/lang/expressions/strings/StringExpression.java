package recipe.lang.expressions.strings;

import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.store.Store;

import java.util.List;
import java.util.Set;

import static org.petitparser.parser.primitive.CharacterParser.word;

public abstract class StringExpression implements Expression {
    public abstract StringValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    public abstract StringExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;


    public static org.petitparser.parser.Parser parser(){
        org.petitparser.parser.Parser parser = MyStringVariable.parser().or(StringVariable.parser()).or(StringValue.parser());

        return parser;
    }
}
