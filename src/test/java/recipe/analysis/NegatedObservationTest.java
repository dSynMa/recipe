package recipe.analysis;

import java.util.Collections;
import org.junit.Test;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.Not;
import recipe.lang.types.Boolean;

import static org.junit.Assert.*;

public class NegatedObservationTest {
    @Test
    public void translatesNegatedObservationOperand() throws Exception {
        TypedVariable<Boolean> variable = new TypedVariable<>(Boolean.getType(), "ready");
        Expression<Boolean> result = ToNuXmv.handleCVsInObservation(
            Collections.emptyMap(), new Not(variable), Condition.getTrue());

        assertTrue(result instanceof Not);
        assertSame(variable, ((Not) result).getArgument());
    }

    @Test
    public void translatesNestedNegations() throws Exception {
        Expression<Boolean> result = ToNuXmv.handleCVsInObservation(
            Collections.emptyMap(), new Not(new Not(Condition.getTrue())), Condition.getTrue());

        assertEquals(Condition.getTrue(), result.simplify());
    }
}
