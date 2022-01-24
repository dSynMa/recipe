package recipe.lang.expressions.predicate;

import org.junit.BeforeClass;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.types.Enum;
import recipe.lang.utils.TypingContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class IsEqualToTest {
    static List<String> channels;
    static Enum channelEnum;

    @BeforeClass
    public static void init() throws TypeCreationException {
        channels = new ArrayList<>();
        channels.add("A");
        channels.add("*");

        channelEnum = new Enum("channels", channels);
    }

    @Test
    public void parser() throws Exception {
        String script = "channel == A";

        TypingContext context = new TypingContext();

        context.set("channel", channelEnum);

        org.petitparser.parser.Parser variable = context.variableParser();
        org.petitparser.parser.Parser value = context.valueParser();

        SettableParser expression = SettableParser.undefined();
        expression.set((variable).or(value));

        Parser parser = IsEqualTo.parser(expression).end();
        Result r = parser.parse(script);
        assert r.isSuccess();
    }

    @Test
    public void parser1() throws Exception {
        String script = "channel = A";

        TypingContext context = new TypingContext();

        context.set("channel", channelEnum);

        org.petitparser.parser.Parser variable = context.variableParser();
        org.petitparser.parser.Parser value = context.valueParser();

        SettableParser expression = SettableParser.undefined();
        expression.set((variable).or(value));

        Parser parser = IsEqualTo.parser(expression).end();
        Result r = parser.parse(script);
        assert r.isSuccess();
    }
}