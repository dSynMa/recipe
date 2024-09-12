package recipe.lang.expressions.location;

import org.petitparser.parser.primitive.StringParser;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.types.Boolean;
import recipe.lang.expressions.predicate.Condition;

public class SelfLocation extends Location {
    public static final String selfToken = "SELF";

    @Override
    public Expression<Boolean> getPredicate(TypedValue supplier) { return Condition.getTrue(); }

    @Override
    public String toString() { return selfToken; }

    public static org.petitparser.parser.Parser parser() throws Exception {
        return StringParser.of(selfToken).map((String self) -> {return new SelfLocation();});
    }
}
