package recipe.lang.expressions.arithmetic;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.types.Real;
import recipe.lang.utils.TypingContext;

import static org.junit.Assert.*;

public class ArithmeticExpressionTest {

    @Test
    public void parser() throws Exception {
        TypingContext context = new TypingContext();
        context.set("v", Real.getType());
        Parser parser = ArithmeticExpression.parser(context).end();
        Result r;
        r = parser.parse("v");
        assert r.isSuccess();
        r = parser.parse("6");
        assert r.isSuccess();
        r = parser.parse("6 + (9*9)");
        assert r.isSuccess();
        r = parser.parse("6 - (9*9)");
        assert r.isSuccess();
        r = parser.parse("6 - (9+9)");
        assert r.isSuccess();
        r = parser.parse("6 * 9");
        assert r.isSuccess();
        r = parser.parse("6 * (9-9)");
        assert r.isSuccess();
        r = parser.parse("6 * (9 - (9 + 7))");
        assert r.isSuccess();
        r = parser.parse("6 * (9 - 9 + 7)");
        assert r.isSuccess();
        r = parser.parse("6 * 9)");
        assert r.isFailure();
        r = parser.parse("6 * 9 + 4");
        assert r.isSuccess();
        r = parser.parse("false > true");
        assert r.isFailure();
    }
}