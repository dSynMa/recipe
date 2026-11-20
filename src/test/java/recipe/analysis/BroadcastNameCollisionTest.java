package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;
import static org.junit.Assert.*;

public class BroadcastNameCollisionTest {
    private void assertReserved(String model) {
        try {
            assertFalse(recipe.lang.System.parser().end().parse(model).isSuccess());
        } catch (RuntimeException error) {
            assertTrue(error.toString(), error.toString().contains("'broadcast' is reserved"));
        }
    }

    @Test
    public void rejectsBroadcastAsAChannelName() throws Exception {
        assertReserved(source("non-listening-enabled-receive").replace("channels: c", "channels: broadcast"));
    }

    @Test
    public void rejectsBroadcastAsAnEnumValue() throws Exception {
        assertReserved(source("non-listening-enabled-receive")
            .replace("message-structure:", "enum choice {ordinary, broadcast}\nmessage-structure:"));
    }

    @Test
    public void rejectsBroadcastAsAnAgentInstanceName() throws Exception {
        assertReserved(source("non-listening-enabled-receive").replace("Sender(sender,", "Sender(broadcast,"));
    }

    @Test
    public void similarlyNamedChannelDoesNotAliasStar() throws Exception {
        String model = source("non-listening-enabled-receive")
            .replace("channels: c", "channels: broadcast1")
            .replace("local: sent: boolean", "local: sent: boolean, ch: channel")
            .replace("init: !sent", "init: !sent & ch == broadcast1")
            .replace("receive-guard: FALSE", "receive-guard: TRUE")
            .replace("c!", "ch!").replace("c?", "*?");
        assertCtl(ToNuXmv.transform(parse(model)), "AG !receiver-received", true);
        assertCtl(ToNuXmv.transform(parse(model.replace("ch == broadcast1", "ch == *"))),
            "EX receiver-received", true);
    }
}
