package recipe.lang.expressions.predicate;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;

import static org.junit.Assert.*;

public class ConditionTest {

    @Test
    public void isSatisfiedBy() {
    }

    @Test
    public void getType() {
    }

    @Test
    public void testEquals() {
    }

    @Test
    public void valueIn() {
    }

    @Test
    public void close() {
    }

    @Test
    public void parserSuccess() {
        Parser parser = Condition.parser(ArithmeticExpression.parser()).end();
        Result r = parser.parse("cond");
        assert r.isSuccess();
        r = parser.parse("cond");
        assert r.isSuccess();
        r = parser.parse("!cond");
        assert r.isSuccess();
        r = parser.parse("! cond");
        assert r.isSuccess();
        r = parser.parse("!(cond)");
        assert r.isSuccess();
        r = parser.parse("(!cond)");
        assert r.isSuccess();
        r = parser.parse("!cond & cond");
        assert r.isSuccess();
        r = parser.parse("!(cond & cond)");
        assert r.isSuccess();
        r = parser.parse("!(cond & cond) | cond");
        assert r.isSuccess();
        r = parser.parse("(!(cond & cond)) | cond");
        assert r.isSuccess();
        r = parser.parse("((!(cond & cond)) | cond)");
        assert r.isSuccess();
        r = parser.parse("((!(cond & cond)) | (!(cond & cond)))");
        assert r.isSuccess();
        r = parser.parse("!(cond & cond) | (((!(cond & cond))))");
        assert r.isSuccess();
        r = parser.parse("!(cond & cond) | !(((!(cond & cond))))");
        assert r.isSuccess();
        r = parser.parse("!(cond & !cond) | !(((!(cond & cond))))");
        assert r.isSuccess();
    }

    @Test
    public void parserFailure() {
        Parser parser = Condition.parser(ArithmeticExpression.parser()).end();
        Result r = parser.parse("!cond!");
        assert r.isFailure();
        r = parser.parse("!(cond & !cond");
        assert r.isFailure();
        r = parser.parse("!(cond & !cond))");
        assert r.isFailure();
        r = parser.parse("!cond & !cond | cond");
        assert r.isFailure();
    }
}