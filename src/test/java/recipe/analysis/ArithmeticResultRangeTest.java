package recipe.analysis;

import org.junit.Test;
import java.math.BigDecimal;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.utils.TypingContext;
import static org.junit.Assert.*;

public class ArithmeticResultRangeTest {
    @Test
    public void largeIntegralRealResultsAgreeWithSymbolicSimplification() throws Exception {
        String[][] examples = {
            {"2147483647.0 + 1.0", "2147483648"},
            {"0.0 - 2147483647.0 - 2.0", "-2147483649"},
            {"2147483647.0 * 2.0", "4294967294"},
            {"100000000000000000000.0 * 100000000000000000000.0", "10000000000000000000000000000000000000000"}
        };
        for (String[] example : examples) {
            Expression expression = ArithmeticExpression.parser(new TypingContext()).end().parse(example[0]).get();
            TypedValue<?> evaluated = expression.valueIn(null);
            assertEquals(0, new BigDecimal(example[1]).compareTo(new BigDecimal(evaluated.getValue().toString())));
            assertEquals(recipe.lang.types.Real.getType(), evaluated.getType());
            assertEquals(expression.simplify(), evaluated);
        }
    }

    @Test
    public void inRangeIntegersAndFractionsRetainTheirValues() throws Exception {
        for (String[] example : new String[][] {{"2147483646.0 + 1.0", "2147483647"},
                {"0.0 - 2147483647.0 - 1.0", "-2147483648"}, {"0.1 + 0.2", "0.3"}, {"0.0 * 1.0", "0"}}) {
            Expression expression = ArithmeticExpression.parser(new TypingContext()).end().parse(example[0]).get();
            assertEquals(example[1], expression.valueIn(null).toString());
        }
    }
}
