package recipe.lang.expressions.location;

import org.petitparser.parser.primitive.StringParser;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;

public class AnyLocation extends Location {
    public static final String anyToken = "ANY";

    @Override
    public Expression<Boolean> getPredicate(TypedValue supplier) { return Condition.getTrue(); }

    @Override
    public String toString() { return anyToken; }
    
    public static org.petitparser.parser.Parser parser() throws Exception {
        return StringParser.of(anyToken).map((String result) -> {return new AnyLocation();});
    }
}
