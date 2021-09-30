package recipe.lang.expressions.predicate;

import org.junit.Test;
import org.petitparser.context.Result;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.utils.TypingContext;

public class IsLessThanTest {

    @Test
    public void testEquals() {
    }

    @Test
    public void testHashCode() {
    }

    @Test
    public void testToString() {
    }

    @Test
    public void valueIn() {
    }

    @Test
    public void close() {
    }

    @Test
    public void parser() throws Exception {
        org.petitparser.parser.Parser parser = IsLessThan.parser(ArithmeticExpression.parser(new TypingContext()));
        Result r = parser.parse("6 < 7");
        assert r.isSuccess();
    }
}