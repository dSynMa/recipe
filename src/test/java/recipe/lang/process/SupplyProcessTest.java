package recipe.lang.process;

import org.junit.BeforeClass;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.Config;
import recipe.lang.utils.exceptions.TypeCreationException;
import recipe.lang.expressions.predicate.NamedLocation;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Real;
import recipe.lang.utils.TypingContext;

import java.util.ArrayList;
import java.util.List;

public class SupplyProcessTest {
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

        Parser parser = SupplyProcess.parser(messageContext, localContext, communicationContext);

        Result r = parser.parse("<v == 5> SUPPLY@TRUE(m := 1)[v := 6]");
        assert r.isSuccess();
        assert r.get() instanceof SupplyProcess && !(((SupplyProcess) r.get()).getMessageGuard() instanceof NamedLocation);
        Result r2 = parser.parse("<v == 5> SUPPLY@SELF(m := 1)[v := 6]");
        assert r2.isSuccess();
        assert r2.get() instanceof SupplyProcess && ((SupplyProcess) r2.get()).getMessageGuard() instanceof NamedLocation;
        Result r3 = parser.parse("<v == 5> SUPPLY@(agent)(m := 1)[v := 6]");
        assert r3.isSuccess();
        assert r3.get() instanceof SupplyProcess && ((SupplyProcess) r3.get()).getMessageGuard() instanceof NamedLocation;
    }
}