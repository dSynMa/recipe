package recipe.analysis;

import org.junit.Test;
import org.petitparser.context.Result;

import static org.junit.Assert.assertTrue;
import static recipe.analysis.TranslationTestModels.assertLtl;
import static recipe.analysis.TranslationTestModels.assertValid;

public class JavaLocationParserTest {

    @Test
    public void javaParserSupportsLocationTypeAndMyself() throws Exception {
        String script = String.join("\n",
            "channels: c",
            "message-structure: NAME : location",
            "",
            "agent Node",
            "    local: x : location, y : location",
            "    init: x = myself & y = myself",
            "    receive-guard: true",
            "    repeat: (",
            "        s: <TRUE> SUPPLY@(any)(NAME := myself)[y := NAME]",
            "        +",
            "        g: <TRUE> GET@(true)(NAME := myself)[y := NAME]",
            "    )",
            "",
            "system = Node(one, true) | Node(two, true)",
            "",
            "SPEC F (one-y = two & two-y = one);");

        Result parsed = recipe.lang.System.parser().end().parse(script);
        assertTrue(parsed.toString(), parsed.isSuccess());
        String output = ToNuXmv.transform(parsed.get());

        assertValid(output);
        assertLtl(output, "F (one-y = two & two-y = one)", true);
    }
}
