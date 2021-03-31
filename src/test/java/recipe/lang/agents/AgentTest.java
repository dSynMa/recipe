package recipe.lang.agents;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.utils.TypingContext;

import static org.junit.Assert.*;

public class AgentTest {

    @Test
    public void parser() {
        String agent = "agent B\n" +
                "\tlocal:\n" +
                "\t\tc : int := 0\n" +
                "\t\tb : int := 0\n" +
                "\t\tchVar : channel := A\n" +
                "\trelabel:\n" +
                "\t\tf <- b\n" +
                "\t\tg <- b != 0\n" +
                "\treceive-guard:\n" +
                "\t\t(c < 5 & channel == A) | (c > 3 & channel == *)\n" +
                "\trepeat: (<b == 0> chVar!(b == f) (d1 := b + 1, d2 := false)[b := b + 1])";

        TypingContext messageContext = new TypingContext();
        messageContext.set("d1", new NumberVariable("d1"));
        messageContext.set("d2", new BooleanVariable("d2"));

        TypingContext communicationContext = new TypingContext();
        communicationContext.set("f", new NumberVariable("f"));
        communicationContext.set("g", new BooleanVariable("g"));

        TypingContext channelContext = new TypingContext();
        channelContext.set("A", new ChannelValue("A"));
        channelContext.set("*", new ChannelValue("*"));

        Parser parser = Agent.parser(messageContext, communicationContext, channelContext).end();
        Result r = parser.parse(agent);
        System.out.println(r.getPosition());
        System.out.println(agent.substring(r.getPosition()));
        System.out.println(r.getMessage());
        assert r.isSuccess();
    }
}