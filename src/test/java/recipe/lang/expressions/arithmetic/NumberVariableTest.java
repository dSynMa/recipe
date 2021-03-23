package recipe.lang.expressions.arithmetic;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;

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
        Parser parser = NumberVariable.parser().end();
        Result r = parser.parse("v");
        assert r.isSuccess();
        r = parser.parse("w");
        assert r.isSuccess();
        r = parser.parse("w v");
        assert r.isFailure();
    }
}