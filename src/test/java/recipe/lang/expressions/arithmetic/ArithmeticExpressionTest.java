package recipe.lang.expressions.arithmetic;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;

import static org.junit.Assert.*;

public class ArithmeticExpressionTest {

    @Test
    public void parser() {
        Parser parser = ArithmeticExpression.parser().end();
        Result r = parser.parse("6 + (9*9)");
        assert r.isSuccess();
        r = parser.parse("6 - (9*9)");
        assert r.isSuccess();
        r = parser.parse("6 - (9+9)");
        assert r.isSuccess();
        r = parser.parse("6 * (9-9)");
        assert r.isSuccess();
        r = parser.parse("6 * (9 - (9 + 7))");
        assert r.isSuccess();
        r = parser.parse("6 * (9 - 9 + 7)");
        assert r.isFailure();
    }
}