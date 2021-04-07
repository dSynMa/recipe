package recipe.lang.expressions.strings;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.strings.StringValue;
import recipe.lang.store.Store;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static org.petitparser.parser.primitive.CharacterParser.word;

public abstract class StringExpression implements Expression {
    public abstract StringValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
    public abstract StringExpression close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;

    public static Parser typeParser(TypingContext context){
        return StringExpression.parser(context);
    }

    public static Parser parser(TypingContext context){
        Parser parser = StringVariable.parser(context).or(StringValue.parser());

        return parser;
    }

    public abstract StringExpression relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException;

    @Override
    public int hashCode(){
        return Objects.hash(this.toString());
    }
}
