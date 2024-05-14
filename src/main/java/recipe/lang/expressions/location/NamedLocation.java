package recipe.lang.expressions.location;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsEqualTo;
import recipe.Config;

public class NamedLocation extends Location {
    private String name;

    public NamedLocation(String name) { this.name = name; }

    @Override
    public Expression<Boolean> getPredicate(TypedValue supplier) {
        try {
            Enum locationEnum = Enum.getEnum(Config.locationLabel);
            if (locationEnum.isValidValue(name)) {
                return new IsEqualTo(new TypedValue(locationEnum, name), supplier);
            }
            else {
                return new IsEqualTo(new TypedVariable(locationEnum, name), supplier);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return Condition.getFalse();
        }
    }

    @Override 
    public String toString() { return name; }

}
