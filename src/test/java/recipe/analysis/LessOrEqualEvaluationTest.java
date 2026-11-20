package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.IsLessOrEqualThan;
import recipe.lang.types.Number;
import static org.junit.Assert.*;

public class LessOrEqualEvaluationTest {
    @Test
    public void evaluatesIntegerAndRealOperandsInBothOrders() throws Exception {
        TypedValue<Number> one = new TypedValue<>(recipe.lang.types.Integer.getType(), "1");
        TypedValue<Number> two = new TypedValue<>(recipe.lang.types.Integer.getType(), "2");
        TypedValue<Number> negative = new TypedValue<>(recipe.lang.types.Real.getType(), "-0.5");
        assertEquals(Condition.getTrue(), new IsLessOrEqualThan(one, two).valueIn(null));
        assertEquals(Condition.getTrue(), new IsLessOrEqualThan(one, one).valueIn(null));
        assertEquals(Condition.getFalse(), new IsLessOrEqualThan(two, one).valueIn(null));
        assertEquals(Condition.getTrue(), new IsLessOrEqualThan(negative, one).valueIn(null));
        assertEquals(Condition.getFalse(), new IsLessOrEqualThan(one, negative).valueIn(null));
    }
}
