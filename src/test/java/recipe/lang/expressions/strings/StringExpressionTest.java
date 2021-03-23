package recipe.lang.expressions.strings;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;

import static org.junit.Assert.*;

public class StringExpressionTest {

    @Test
    public void valueIn() {
    }

    @Test
    public void close() {
    }

    @Test
    public void parser() {
        Parser parser = StringExpression.parser();
        Result r = parser.parse("v");
        assert r.isSuccess();
        r = parser.parse("my.v");
        assert r.isSuccess();
        r = parser.parse("\"xxxxx\"");
        assert r.isSuccess();
    }
}