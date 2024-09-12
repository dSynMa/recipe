package recipe.lang.expressions.location;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsEqualTo;

import static recipe.Config.getAgent;

import org.petitparser.parser.primitive.FailureParser;
import org.petitparser.parser.primitive.StringParser;

import recipe.Config;

public class NamedLocation extends Location {
    private TypedVariable var;

    public NamedLocation(TypedVariable var) {
        this.var = var;
    }

    @Override
    public Expression<Boolean> getPredicate(TypedValue supplier) {
        try {
            return new IsEqualTo(this.var, supplier);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return Condition.getFalse();
        }
    }

    @Override 
    public String toString() { return this.var.getName(); }


    public static org.petitparser.parser.Parser parser(TypingContext context) throws Exception {
        try {
            return context.variableParser().map((TypedVariable tv) -> {
                // TODO fail if tv is not of Location type
                return new NamedLocation(tv);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            return FailureParser.withMessage(ex.getMessage());
        }
    }
}
