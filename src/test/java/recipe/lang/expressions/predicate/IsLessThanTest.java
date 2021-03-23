package recipe.lang.expressions.predicate;

import org.junit.Test;
import org.petitparser.context.Result;
import recipe.lang.Parser;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;

import static org.junit.Assert.*;

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
    public void parser() {
        org.petitparser.parser.Parser parser = IsLessThan.parser(ArithmeticExpression.parser());
        Result r = parser.parse("6 < 7");
        assert r.isSuccess();
    }
}