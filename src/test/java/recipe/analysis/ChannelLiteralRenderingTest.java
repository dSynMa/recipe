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
}
