package recipe.analysis;

import org.junit.Test;
import org.petitparser.context.Result;

import static org.junit.Assert.assertTrue;
import static recipe.analysis.TranslationTestModels.assertLtl;
import static recipe.analysis.TranslationTestModels.assertValid;

public class TwoWayParserTest {

    @Test
    public void javaParserSupportsTwoWayPayloads() throws Exception {
        String script = String.join("\n",
            "channels: c",
            "message-structure: NAME : bool",
            "",
            "agent Sender",
            "    local: sent: bool",
            "    init: !sent",
            "    receive-guard: true",
            "    repeat: (",
            "        send: <TRUE> SUPPLY@TRUE(NAME := TRUE)[sent := NAME]",
            "    )",
            "",
            "agent Getter",
            "    local: got: bool",
            "    init: !got",
            "    receive-guard: true",
            "    repeat: (",
            "        recv: <NAME> GET@TRUE(NAME := FALSE)[got := NAME]",
            "    )",
            "",
            "system = Sender(s, true) | Getter(g, true)",
            "",
            "SPEC F g-got;");

        Result parsed = recipe.lang.System.parser().end().parse(script);
        assertTrue(parsed.toString(), parsed.isSuccess());
        String output = ToNuXmv.transform(parsed.get());

        assertValid(output);
        assertLtl(output, "F g-got", true);
    }
}
