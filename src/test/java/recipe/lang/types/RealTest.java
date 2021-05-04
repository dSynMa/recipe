package recipe.lang.types;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;

import static org.junit.Assert.*;

public class RealTest {

    @Test
    public void parser() {
        Parser parser = Real.getType().parser();

        Result r;
        r = parser.parse("6");
        assert r.isSuccess();
        r = parser.parse("6.9");
        assert r.isSuccess();
    }
}