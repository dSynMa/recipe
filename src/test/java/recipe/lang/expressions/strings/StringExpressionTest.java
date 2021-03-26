package recipe.lang.expressions.strings;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.utils.TypingContext;

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
        TypingContext context = new TypingContext();
        context.set("v", new StringVariable("v"));
        Parser parser = StringExpression.parser(context);
        Result r = parser.parse("v");
        assert r.isSuccess();
//        r = parser.parse("my.v");
//        assert r.isSuccess();
        r = parser.parse("\"xxxxx\"");
        assert r.isSuccess();
    }
}