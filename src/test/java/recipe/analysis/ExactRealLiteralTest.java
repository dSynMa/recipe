package recipe.analysis;

import org.junit.Test;
import java.math.BigDecimal;
import recipe.lang.expressions.TypedValue;
import recipe.lang.types.Real;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ExactRealLiteralTest {
    @Test
    public void realLiteralsRetainAllDecimalDigitsInTheModel() throws Exception {
        for (String literal : new String[] {"0.10000000000000001", "0.0000001", "9007199254740993.0"}) {
            String model = source("arithmetic-multiplication").replace("count: integer", "count: real")
                .replace("count * 2", literal);
            String output = ToNuXmv.transform(parse(model));
            assertValid(output);
            assertSmtInvariant(output, "sender-sent -> sender-count = " + literal);
        }
    }

    @Test
    public void constantFoldingAndNumericEqualityRemainExact() throws Exception {
        TypedValue<?> exact = new TypedValue<>(Real.getType(), "0.10000000000000001");
        assertNotEquals(new TypedValue<>(Real.getType(), "0.1"), exact);
        assertEquals(new BigDecimal("0.10000000000000001"), exact.getValue());
        TypedValue<?> huge = new TypedValue<>(Real.getType(), "1e400");
        TypedValue<?> hugeScaled = new TypedValue<>(Real.getType(), "10e399");
        assertEquals(huge, hugeScaled);
        assertEquals(huge.hashCode(), hugeScaled.hashCode());
        assertTrue(new TypedValue<>(Real.getType(), "1").toString().contains("."));
        String model = source("arithmetic-multiplication").replace("count: integer", "count: real")
            .replace("count * 2", "0.10000000000000001 - 0.1");
        assertSmtInvariant(ToNuXmv.transform(parse(model)), "sender-sent -> sender-count = 0.00000000000000001");
    }
}
