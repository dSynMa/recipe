package recipe.lang.types;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;

public class RealTest {

    @Test
    public void parser() {
        Parser parser = Real.getType().valueParser();

        Result r;
        r = parser.parse("6.0");
        assert r.isSuccess();
        r = parser.parse("6.9");
        assert r.isSuccess();
    }
}