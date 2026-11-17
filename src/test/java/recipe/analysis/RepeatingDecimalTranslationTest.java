package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class RepeatingDecimalTranslationTest {
    @Test
    public void repeatingDecimalDoesNotCrashTranslation() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: real")
            .replace("count * 2", "1 / 3");
        assertValid(ToNuXmv.transform(parse(model.replace("1 / 3", "1 / 2"))));
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertSmtInvariant(output, "sender-sent -> sender-count * 3 = 1");
    }

    @Test
    public void nestedQuotientsRemainExact() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: real")
            .replace("count * 2", "(1 / 3) + (1 / 6)");
        assertSmtInvariant(ToNuXmv.transform(parse(model)), "sender-sent -> sender-count * 2 = 1");
    }

    @Test
    public void zeroDivisorDisablesTheTransition() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: real")
            .replace("count * 2", "1 / 0");
        assertSmtInvariant(ToNuXmv.transform(parse(model)), "!progress");
    }
}
