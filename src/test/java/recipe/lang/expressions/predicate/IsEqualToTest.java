package recipe.lang.expressions.predicate;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.strings.StringExpression;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import static org.junit.Assert.*;

public class IsEqualToTest {

    @Test
    public void parser() {
        String script = "channel == A";

        TypingContext context = new TypingContext();

        context.set("channel", new ChannelVariable("channel"));
        context.set("A", new ChannelValue("A"));

        org.petitparser.parser.Parser arithmeticExpression = ArithmeticExpression.typeParser(context);
        org.petitparser.parser.Parser channelExpression = ChannelExpression.typeParser(context);
        org.petitparser.parser.Parser stringExpression = StringExpression.typeParser(context);

        SettableParser expression = SettableParser.undefined();
        expression.set((arithmeticExpression).or(channelExpression).or(stringExpression));


        Parser parser = IsEqualTo.parser(expression).end();
        Result r = parser.parse(script);
        assert r.isSuccess();
    }
}