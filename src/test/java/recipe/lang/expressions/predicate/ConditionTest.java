package recipe.lang.expressions.predicate;

import org.junit.BeforeClass;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.Config;
import recipe.lang.utils.exceptions.TypeCreationException;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Real;
import recipe.lang.utils.TypingContext;

import java.util.ArrayList;
import java.util.List;

public class ConditionTest {
    static List<String> channels;
    static Enum channelEnum;

    @BeforeClass
    public static void init() throws TypeCreationException {
        Enum.clear();
        channels = new ArrayList<>();
        channels.add("A");
        channels.add("*");

        channelEnum = new Enum(Config.channelLabel, channels);
    }

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
    public void parserSuccess() throws Exception {
        TypingContext context = new TypingContext();

        context.set("cond", Boolean.getType());
        context.set("b", Real.getType());

        context.set("channel", channelEnum);

        Parser parser = Condition.parser(context).end();
        Result r = parser.parse("cond");
        assert r.isSuccess();
        r = parser.parse("false");
        assert r.isSuccess();
        r = parser.parse("cond = false");
        assert r.isSuccess();
        r = IsLessThan.parser(ArithmeticExpression.parser(context)).parse("1 < 2");
        assert r.isSuccess();
        r = parser.parse("1 < 2");
        assert r.isSuccess();
        r = parser.parse("cond");
        assert r.isSuccess();
        r = parser.parse("!cond -> cond");
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
        r = parser.parse("(b > 3 & channel == A & (cond == true)) | (b < 5 & channel == A)");
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
        r = parser.parse("cond & !cond && cond & cond");
        assert r.isSuccess();
        r = parser.parse("cond | !cond || cond | cond");
        assert r.isSuccess();
    }

    @Test
    public void parserFailure() throws Exception {
        TypingContext context = new TypingContext();
        context.set("cond", Boolean.getType());
        context.set("b", Real.getType());
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
        r = parser.parse("cond & !cond && cond & cond || cond");
        assert r.isFailure();
        r = parser.parse("cond | !cond || cond | cond && cond");
        assert r.isFailure();
    }
}