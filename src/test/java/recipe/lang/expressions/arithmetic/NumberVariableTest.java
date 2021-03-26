package recipe.lang.expressions.arithmetic;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.utils.TypingContext;

import static org.junit.Assert.*;

public class NumberVariableTest {

    @Test
    public void valueIn() {
    }

    @Test
    public void close() {
    }

    @Test
    public void isValidValue() {
    }

    @Test
    public void parser() {
        TypingContext context = new TypingContext();
        context.set("v", new NumberVariable("v"));
        context.set("w", new NumberVariable("w"));
        Parser parser = NumberVariable.parser(context).end();
        Result r = parser.parse("v");
        assert r.isSuccess();
        r = parser.parse("w");
        assert r.isSuccess();
        r = parser.parse("w v");
        assert r.isFailure();
    }
}