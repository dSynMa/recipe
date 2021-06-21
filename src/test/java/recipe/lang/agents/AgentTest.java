package recipe.lang.agents;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.Config;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Real;
import recipe.lang.utils.TypingContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AgentTest {

    @BeforeClass
    public static void init() throws TypeCreationException {
        Enum.clear();
    }

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
                "\t\t(c < 5 & channel == A) | (c > 3 & channel == B)\n" +
                "\trepeat: (<b == 0> chVar!(b == f) (d1 := b + 1, d2 := false)[b := b + 1])";

        TypingContext messageContext = new TypingContext();
        messageContext.set("d1", Real.getType());
        messageContext.set("d2", Boolean.getType());

        TypingContext communicationContext = new TypingContext();
        communicationContext.set("f", Real.getType());
        communicationContext.set("g", Boolean.getType());

        List<String> channels = new ArrayList<>();
        channels.add("A");
        channels.add("B");

        new Enum(Config.channelWithoutBroadcastLabel, channels);
        List<String> channelsWithBroadcast = new ArrayList<>(channels);
        channelsWithBroadcast.add("*");
        new Enum(Config.channelLabel, channelsWithBroadcast);

        Parser parser = Agent.parser(messageContext, communicationContext, new TypingContext()).end();
        Result r = parser.parse(agent);
        System.out.println(r.getPosition());
        System.out.println(agent.substring(r.getPosition()));
        System.out.println(r.getMessage());
        assert r.isSuccess();
    }
}