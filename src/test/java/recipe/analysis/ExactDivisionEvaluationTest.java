package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.types.Real;
import recipe.lang.utils.TypingContext;
import static org.junit.Assert.*;

public class ExactDivisionEvaluationTest {
    @Test
    public void terminatingDivisionRetainsMoreThanDecimal128Precision() throws Exception {
        for (String numerator : new String[] {"12345678901234567890123456789012345.0", "0.12345678901234567890123456789012345"}) {
            for (String denominator : new String[] {"1.0", "2.0", "8.0"}) {
                Expression value = ArithmeticExpression.parser(new TypingContext()).end().parse(numerator + " / " + denominator).get();
                String expected = new java.math.BigDecimal(numerator).divide(new java.math.BigDecimal(denominator)).toPlainString();
                assertEquals(new TypedValue(Real.getType(), expected), value.valueIn(null));
                assertEquals(value.simplify(), value.valueIn(null));
            }
        }
    }

    @Test
    public void generatedTerminatingQuotientsRoundTripWithoutRounding() throws Exception {
        java.util.Random random = new java.util.Random(7193);
        for (int i = 0; i < 100; i++) {
            java.math.BigDecimal numerator = new java.math.BigDecimal(new java.math.BigInteger(200, random), random.nextInt(10));
            if (i % 2 == 0) numerator = numerator.negate();
            java.math.BigDecimal denominator = java.math.BigDecimal.valueOf(2).pow(random.nextInt(20))
                .multiply(java.math.BigDecimal.valueOf(5).pow(random.nextInt(20)));
            recipe.lang.expressions.arithmetic.Division division = new recipe.lang.expressions.arithmetic.Division(
                new TypedValue(Real.getType(), numerator.toPlainString()),
                new TypedValue(Real.getType(), denominator.toPlainString()));
            java.math.BigDecimal quotient = new java.math.BigDecimal(division.valueIn(null).getValue().toString());
            assertEquals(0, numerator.compareTo(quotient.multiply(denominator)));
            assertEquals(division.simplify(), division.valueIn(null));
        }
    }
}
