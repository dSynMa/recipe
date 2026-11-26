package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ChannelLiteralRenderingTest {
    @Test
    public void channelPayloadUpdatesAndSpecificationsPreserveMultiplication() throws Exception {
        String text = "channels: c\n"
            + "message-structure: target: channel\n"
            + "communication-variables: ready: boolean\n"
            + "agent Sender\nlocal: ch: channel, count: 0..8\n"
            + "init: ch == * & count == 2\nrelabel: ready <- TRUE\nreceive-guard: TRUE\n"
            + "repeat: send: <count == 2> ch! (TRUE)(target := *)[ch := *, count := count * 2]\n"
            + "agent Receiver\nlocal: ch: channel\ninit: ch == c\n"
            + "relabel: ready <- TRUE\nreceive-guard: channel == *\n"
            + "repeat: receive: <target == *> *? [ch := target]\n"
            + "system = Sender(sender, TRUE) | Receiver(receiver, TRUE)\n"
            + "SPEC G (sender-ch == *);\n";
        recipe.lang.System model = parse(text);
        String output = ToNuXmv.transform(model);
        assertTrue(output.contains(" * 2"));
        assertTrue(output.substring(output.indexOf("LTLSPEC")).contains("broadcast"));
        assertCtl(output, "EX (sender-count = 4 & receiver-ch = broadcast)", true);
        assertValid(output);
        assertTrue("Rendering must not rename the source channel domain",
            model.getAgentInstances().get(0).getAgent().getStore().getAttributes().get("ch")
                .getType() instanceof recipe.lang.types.Enum);
        assertTrue(((recipe.lang.types.Enum) model.getAgentInstances().get(0).getAgent()
            .getStore().getAttributes().get("ch").getType()).getValues().contains("*"));
    }
    @Test
    public void nestedComparisonAtomsAndGuardsRenameOnlyTheBroadcastValue() throws Exception {
        String text = source("arithmetic-multiplication")
            .replace("channels: c", "channels: c, broadcast1")
            .replace("count: integer", "count: 0..8, ch: channel")
            .replace("init: !sent & count == 2", "init: !sent & count == 2 & ch == *")
            .replace("agent Sender", "guard onStar(ch: channel) := ch == *;\nagent Sender")
            + "\nSPEC G ((sender-ch == *) == (sender-count * 2 >= 0));"
            + "\nSPEC G ((sender-ch != *) != (sender-count * 2 >= 0));"
            + "\nSPEC G onStar(sender-ch);"
            + "\nSPEC G (* == sender-ch);\n";
        recipe.lang.System model = parse(text);
        String before = model.getSpecs().toString();
        String output = ToNuXmv.transform(model);
        assertTrue(output.contains(" * 2"));
        assertTrue(output.contains("broadcast1"));
        for (String line : output.split("\n")) {
            if (line.startsWith("LTLSPEC ")) assertLtl(output, line.substring(8), true);
        }
        assertEquals("Rendering must leave source literals unchanged", before, model.getSpecs().toString());
        assertEquals(output, ToNuXmv.transform(model));
    }

    @Test
    public void explicitRenderingExpandsGuardsWithoutChangingSourceFormulas() throws Exception {
        String text = source("arithmetic-multiplication")
            .replace("channels: c", "channels: c, broadcast1")
            .replace("count: integer", "count: 0..8, ch: channel")
            .replace("init: !sent & count == 2", "init: !sent & count == 2 & ch == *")
            .replace("agent Sender", "guard onStar(ch: channel) := ch == *;\nagent Sender")
            + "\nSPEC G (onStar(sender-ch) & (sender-count * 2 >= 0));\n";
        recipe.lang.System model = parse(text);
        recipe.lang.ltol.LTOL spec = model.getSpecs().get(0);
        boolean previousResolve = recipe.lang.expressions.predicate.GuardReference.resolve;
        try {
            recipe.lang.expressions.predicate.GuardReference.resolve = false;
            String before = spec.toString();
            String rendered = spec.toNuXmv();
            assertTrue(rendered.contains(recipe.Config.nuxmvBroadcast));
            assertTrue(rendered.contains(" * 2"));
            assertFalse(rendered.contains("onStar"));
            assertEquals(rendered, spec.toNuXmv());
            assertEquals(before, spec.toString());
            assertLtl(ToNuXmv.transform(model), rendered, true);
            assertFalse(recipe.lang.expressions.predicate.GuardReference.resolve);
        } finally {
            recipe.lang.expressions.predicate.GuardReference.resolve = previousResolve;
        }
    }
}
