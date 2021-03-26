package recipe.lang.process;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.utils.TypingContext;

import static org.junit.Assert.*;

public class ProcessTest {

    @Test
    public void parser() {
        TypingContext messageContext = new TypingContext();
        messageContext.set("m", new NumberVariable("v"));

        TypingContext localContext = new TypingContext();
        localContext.set("v", new NumberVariable("v"));

        TypingContext communicationContext = new TypingContext();
        communicationContext.set("g", new BooleanVariable("g"));

        TypingContext channelContext = new TypingContext();
        channelContext.set("c", new ChannelVariable("c"));

        Parser parser = Process.parser(messageContext, localContext, communicationContext, channelContext);

        Result r = parser.parse("<v == 5> c!g(m := 1)[v := 6]");
        assert r.isSuccess();
    }
}