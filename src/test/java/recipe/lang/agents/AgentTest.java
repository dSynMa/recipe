package recipe.lang.agents;

import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Real;
import recipe.lang.utils.TypingContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AgentTest {

    @Test
    public void parser() throws Exception {
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
        messageContext.set("d1", Real.getType());
        messageContext.set("d2", Boolean.getType());

        TypingContext communicationContext = new TypingContext();
        communicationContext.set("f", Real.getType());
        communicationContext.set("g", Boolean.getType());

        TypingContext channelContext = new TypingContext();
        List<String> channels = new ArrayList<>();
        channels.add("A");
        channels.add("*");

        Enum channelEnum = new Enum("channels", channels);

        Parser parser = Agent.parser(messageContext, communicationContext, channelContext).end();
        Result r = parser.parse(agent);
        System.out.println(r.getPosition());
        System.out.println(agent.substring(r.getPosition()));
        System.out.println(r.getMessage());
        assert r.isSuccess();
    }
}