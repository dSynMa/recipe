package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.arithmetic.Multiplication;
import recipe.lang.expressions.arithmetic.Division;
import recipe.lang.types.Number;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ArithmeticEvaluationTest {
    @Test
    public void repeatingAndNegativeQuotientsEvaluate() throws Exception {
        assertEquals(1.0 / 3.0, ((java.lang.Number) new Division(integer("1"), integer("3"))
            .valueIn(null).getValue()).doubleValue(), 0.0);
        assertEquals(-1.5, ((java.lang.Number) new Division(integer("-3"), integer("2"))
            .valueIn(null).getValue()).doubleValue(), 0.0);
        TypedValue<Number> half = new TypedValue<>(recipe.lang.types.Real.getType(), "0.5");
        assertEquals(1.5, ((java.lang.Number) new Multiplication(half, integer("3"))
            .valueIn(null).getValue()).doubleValue(), 0.0);
    }

    @Test(expected = ArithmeticException.class)
    public void divisionByZeroStillFails() throws Exception {
        new Division(integer("1"), integer("0")).valueIn(null);
    }
    private TypedValue<Number> integer(String value) throws Exception {
        return new TypedValue<Number>(recipe.lang.types.Integer.getType(), value);
    }

    @Test
    public void multiplicationEvaluatesValuesRatherThanTypes() throws Exception {
        assertEquals("6", new Multiplication(integer("2"), integer("3")).valueIn(null).toString());
    }

    @Test
    public void divisionEvaluatesValuesRatherThanTypes() throws Exception {
        assertEquals(1.5, ((java.lang.Number) new Division(integer("3"), integer("2"))
            .valueIn(null).getValue()).doubleValue(), 0.0);
    }

}
