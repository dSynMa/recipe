package recipe.lang.expressions.location;

import org.petitparser.parser.Parser;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.TypingContext;

public class PredicateLocation extends Location {
    Expression<recipe.lang.types.Boolean> predicate;

    public PredicateLocation(Expression<recipe.lang.types.Boolean> c) { this.predicate = c; }
    
    @Override
    public Expression<recipe.lang.types.Boolean> getPredicate(TypedValue supplier) { return predicate; }

    public static org.petitparser.parser.Parser parser(TypingContext context) throws Exception {
        Parser messageGuard = Condition.typeParser(context);
        return messageGuard.map((Condition x) -> new PredicateLocation(x));
    }

    @Override
    public String toString() { return predicate.toString(); }

}
