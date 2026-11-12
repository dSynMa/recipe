package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.*;
import recipe.lang.types.Number;
import static org.junit.Assert.*;

public class ComparisonSimplificationTest {
    private Condition[] comparisons(Expression<Number> left, Expression<Number> right) {
        return new Condition[] {new IsLessThan(left, right), new IsGreaterThan(left, right),
            new IsLessOrEqualThan(left, right), new IsGreaterOrEqualThan(left, right)};
    }

    @Test
    public void constantsAgreeWithNumericOrderingAndEvaluation() throws Exception {
        String[] values = {"-2", "-1", "0", "1", "2", "-0.5", "1.5", "1.0"};
        for (String left : values) {
            for (String right : values) {
                TypedValue<Number> lhs = new TypedValue<>(left.contains(".")
                    ? recipe.lang.types.Real.getType() : recipe.lang.types.Integer.getType(), left);
                TypedValue<Number> rhs = new TypedValue<>(right.contains(".")
                    ? recipe.lang.types.Real.getType() : recipe.lang.types.Integer.getType(), right);
                double a = Double.parseDouble(left), b = Double.parseDouble(right);
                boolean[] expected = {a < b, a > b, a <= b, a >= b};
                Condition[] conditions = comparisons(lhs, rhs);
                for (int i = 0; i < conditions.length; i++) {
                    Object truth = expected[i] ? Condition.getTrue() : Condition.getFalse();
                    assertEquals(conditions[i].toString(), truth, conditions[i].simplify());
                    assertEquals(conditions[i].toString(), truth, conditions[i].valueIn(null));
                }
            }
        }
    }

    @Test
    public void symbolicComparisonsPreserveUnknownValuesAndHandleReflexivity() throws Exception {
        TypedVariable<Number> x = new TypedVariable<>(recipe.lang.types.Integer.getType(), "x");
        TypedVariable<Number> y = new TypedVariable<>(recipe.lang.types.Integer.getType(), "y");
        Condition[] reflexive = comparisons(x, x);
        for (int i = 0; i < reflexive.length; i++) {
            assertEquals(i < 2 ? Condition.getFalse() : Condition.getTrue(), reflexive[i].simplify());
        }
        for (Condition condition : comparisons(x, y)) {
            assertEquals(condition.getClass(), condition.simplify().getClass());
            assertEquals(condition.toString(), condition.simplify().toString());
        }
    }
}
