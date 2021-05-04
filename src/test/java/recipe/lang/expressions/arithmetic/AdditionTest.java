package recipe.lang.expressions.arithmetic;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.utils.TypingContext;


public class AdditionTest {

    @Test
    public void valueIn() {
    }

    @Test
    public void close() {
    }

    @Test
    public void parser() {
        Parser parser = Addition.parser(new TypingContext()).end();
        Result r = parser.parse("6 + 9");
        assert r.isSuccess();
        r = parser.parse("6 + (9 + 8)");
        assert r.isSuccess();
        r = parser.parse("6 + 9 * 8");
        assert r.isFailure();
    }
}