package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.utils.TypingContext;
import static org.junit.Assert.*;

public class IntegerLiteralValidationTest {
    @Test
    public void overflowingIntegerLiteralsNeverParseAsNull() {
        for (String literal : new String[] {"2147483648", "9999999999999999999999999999"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> recipe.lang.types.Integer.getType().valueParser().end().parse(literal));
            assertTrue(error.getMessage(), error.getMessage().contains(literal));
        }
    }

    @Test
    public void validIntegerAndLargeRealLiteralsStillParse() throws Exception {
        assertEquals("2147483647", recipe.lang.types.Integer.getType().valueParser().end().parse("2147483647").get().toString());
        assertNotNull(ArithmeticExpression.parser(new TypingContext()).end().parse("2147483648.0").get());
        assertNotNull(recipe.lang.types.Integer.getType().valueParser().end().parse("0").get());
    }
}
