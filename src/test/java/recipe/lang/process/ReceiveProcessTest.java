package recipe.lang.process;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.utils.TypingContext;

import static org.junit.Assert.*;

public class ReceiveProcessTest {

    @Test
    public void parser() {
        TypingContext messageContext = new TypingContext();
        messageContext.set("m", new NumberVariable("v"));

        TypingContext localContext = new TypingContext();
        localContext.set("v", new NumberVariable("v"));

        TypingContext channelContext = new TypingContext();
        channelContext.set("c", new ChannelVariable("c"));

        Parser parser = ReceiveProcess.parser(messageContext, localContext, channelContext);

        Result r = parser.parse("<v == 5> c? [v := 6]");
        assert r.isSuccess();
    }
}