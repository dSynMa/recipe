package recipe.lang.expressions.location;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.Boolean;

/**
 * A compatibility location that names a concrete agent instance (the old
 * {@code GET@foo} / {@code SUPPLY@foo} syntax). New models should use a
 * {@link NamedLocation} holding a location variable instead.
 */
public class NameLocation extends Location {
    private final String name;

    public NameLocation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Expression<Boolean> getPredicate(TypedValue supplier) {
        return Condition.getTrue();
    }

    @Override
    public String toString() {
        return name;
    }
}
