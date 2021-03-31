package recipe.lang.utils;

import org.junit.Test;
import recipe.lang.expressions.channels.ChannelValue;

import static org.junit.Assert.*;

public class TypingContextTest {

    @Test
    public void setAll() {
        TypingContext context = new TypingContext();
        TypingContext context1 = new TypingContext();
        context.set("A", new ChannelValue("A"));
        context1.setAll(context);

        assert context1.get("A").equals(new ChannelValue("A"));
    }
}