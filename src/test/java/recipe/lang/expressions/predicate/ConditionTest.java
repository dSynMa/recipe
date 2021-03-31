package recipe.lang.expressions.predicate;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.utils.TypingContext;

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
        TypingContext context = new TypingContext();

        context.set("cond", new BooleanVariable("cond"));
        context.set("b", new NumberVariable("b"));
        context.set("channel", new ChannelVariable("channel"));
        context.set("A", new ChannelValue("A"));

        Parser parser = Condition.parser(context).end();
        Result r = parser.parse("cond");
        assert r.isSuccess();
        r = parser.parse("cond");
        assert r.isSuccess();
        r = parser.parse("(cond)");
        assert r.isSuccess();
        r = parser.parse("!cond");
        assert r.isSuccess();
        r = parser.parse("! cond");
        assert r.isSuccess();
        r = parser.parse("b == 3");
        assert r.isSuccess();
        r = parser.parse("(b == 3)");
        assert r.isSuccess();
        r = parser.parse("(channel == A)");
        assert r.isSuccess();
        r = parser.parse("(channel == A) | (A == channel)");
        assert r.isSuccess();
        r = parser.parse("(b > 3 & channel == A) | (b < 5 & channel == A)");
        assert r.isSuccess();
        r = parser.parse("(channel == A) | false");
        assert r.isSuccess();
        r = parser.parse("channel == A");
        assert r.isSuccess();
        r = parser.parse("!(b > 3)");
        assert r.isSuccess();
        r = parser.parse("!(b > 3) & cond");
        assert r.isSuccess();
        r = parser.parse("b > 3 & cond");
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
        TypingContext context = new TypingContext();
        context.set("cond", new BooleanVariable("cond"));
        context.set("b", new NumberVariable("b"));
        Parser parser = Condition.parser(new TypingContext()).end();
        Result r = parser.parse("!condd");
        assert r.isFailure();
        r = parser.parse("!b > 3");
        assert r.isFailure();
        r = parser.parse("!cond!");
        assert r.isFailure();
        r = parser.parse("!(cond & !cond");
        assert r.isFailure();
        r = parser.parse("!(cond & !cond))");
        assert r.isFailure();
        r = parser.parse("!cond & !cond | cond");
        assert r.isFailure();
    }
}