package recipe.lang.utils;

import org.junit.Test;
import recipe.lang.types.Boolean;

public class TypingContextTest {

    @Test
    public void setAll() {
        TypingContext context = new TypingContext();
        TypingContext context1 = new TypingContext();
        context.set("A", Boolean.getType());
        context1.setAll(context);

        assert context1.get("A").equals(Boolean.getType());
    }
}