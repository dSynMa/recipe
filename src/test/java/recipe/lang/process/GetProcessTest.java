package recipe.lang.process;

import org.junit.BeforeClass;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.Config;
import recipe.lang.utils.exceptions.TypeCreationException;
import recipe.lang.expressions.location.NamedLocation;
import recipe.lang.expressions.location.PredicateLocation;
import recipe.lang.expressions.location.SelfLocation;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Real;
import recipe.lang.utils.TypingContext;

import java.util.ArrayList;
import java.util.List;

public class GetProcessTest {
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
    public void parser() throws Exception {
        TypingContext messageContext = new TypingContext();
        messageContext.set("m", Real.getType());

        TypingContext localContext = new TypingContext();
        localContext.set("v", Real.getType());

        TypingContext communicationContext = new TypingContext();
        communicationContext.set("g", Boolean.getType());

        localContext.set("c", channelEnum);

        Parser parser = GetProcess.parser(messageContext, localContext, communicationContext);

        Result r = parser.parse("<v == 5> GET@(TRUE)[v := 6]");
        assert r.isSuccess();
        assert r.get() instanceof GetProcess && (((GetProcess) r.get()).getLocation() instanceof PredicateLocation);
        r = parser.parse("<v == 5> get!@g(m := 1)[v := 6]");
        assert r.isFailure();

        r = parser.parse("<v == 5> GET@SELF[v := 6]");
        assert r.isSuccess();
        assert r.get() instanceof GetProcess && ((GetProcess) r.get()).getLocation() instanceof SelfLocation;
        r = parser.parse("<v == 5> GET@(agentName)[v := 6]");
        assert r.isSuccess();
        assert r.get() instanceof GetProcess && ((GetProcess) r.get()).getLocation() instanceof NamedLocation;
    }
}