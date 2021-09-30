package recipe.lang.process;

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

public class SendProcessTest {
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

        Parser parser = SendProcess.parser(messageContext, localContext, communicationContext);

        Result r = parser.parse("<v == 5> c!g(m := 1)[v := 6]");
        assert r.isSuccess();
        r = parser.parse("<true> c!g(m := 1)[v := 6]");
        assert r.isSuccess();
        r = parser.parse("<true> c!g(m := 1)[]");
        assert r.isSuccess();
    }
}