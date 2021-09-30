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

import static org.junit.Assert.*;

public class ProcessTest {
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
        messageContext.set("m1", Real.getType());
        messageContext.set("m2", Real.getType());

        TypingContext localContext = new TypingContext();
        localContext.set("v", Real.getType());
        localContext.set("c", channelEnum);

        TypingContext communicationContext = new TypingContext();
        communicationContext.set("g", Boolean.getType());

        Parser parser = Process.parser(messageContext, localContext, communicationContext);

        Result r = parser.parse("<v == 5> c!g (m1 := 1, m2 := 2)[v := 6]");
        assert r.isSuccess();
        r = parser.parse("<v == 5> c!g (m1 := 1)[v := 6]");
        assert r.isSuccess();
        r = parser.parse("(<v == 5> c!g (m1 := 1)[v := 6])");
        assert r.isSuccess();
        r = parser.parse("(<v == 5> c!g (m1 := 1)[v := 6]) ; (<v == 5> c!g (m1 := 1)[v := 6])");
        assert r.isSuccess();
        r = parser.parse("((<v == 5> c!g (m1 := 1)[v := 6]) + (<v == 5> c!g (m1 := 1)[v := 6])) ; (<v == 5> c!g (m1 := 1)[v := 6])");
        assert r.isSuccess();
        r = parser.parse("(<v == 5> c!g (m1 := 1)[v := 6] + <v == 5> c!g (m1 := 1)[v := 6]) ; (<v == 5> c!g (m1 := 1)[v := 6] + <v == 5> c!g (m1 := 1)[v := 6])");
        assert r.isSuccess();
    }
}