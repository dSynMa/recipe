package recipe.lang.expressions.location;

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
}
