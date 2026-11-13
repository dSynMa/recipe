package recipe.analysis;

import org.junit.Test;

import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReceiverParticipationTest {
    @Test
    public void disconnectedReceiverDoesNotBlockEitherSenderSelection() throws Exception {
        for (String selection : new String[] {"TRUE", "FALSE"}) {
            String output = translate("c", false, selection, true);
            String send = definition(output, "transition");
            assertTrue(send.contains("keep-all-receiver"));
            assertFalse(send.contains("next(receiver-receive) = TRUE"));
            assertCtl(output, "EX (sender-sent & !receiver-received)", true);
            assertCtl(output, "AG !receiver-received", true);
        }
    }

    @Test
    public void connectedUnselectedMulticastReceiverBlocksSend() throws Exception {
        assertCtl(translate("c", true, "FALSE", true), "EX sender-sent", false);
    }

    @Test
    public void connectedMulticastReceiverWithoutEnabledReceiveBlocksSend() throws Exception {
        assertCtl(translate("c", true, "TRUE", false), "EX sender-sent", false);
    }

    @Test
    public void connectedSelectedMulticastReceiverParticipates() throws Exception {
        assertCtl(translate("c", true, "TRUE", true), "EX (sender-sent & receiver-received)", true);
    }

    @Test
    public void unselectedBroadcastReceiverStaysUnchanged() throws Exception {
        String output = translate("*", true, "FALSE", true);
        assertCtl(output, "EX (sender-sent & !receiver-received)", true);
        assertCtl(output, "AG !receiver-received", true);
    }

    @Test
    public void broadcastWithoutEnabledReceiveDoesNotBlock() throws Exception {
        assertCtl(translate("*", true, "TRUE", false), "EX (sender-sent & !receiver-received)", true);
    }

    @Test
    public void selectedBroadcastReceiverParticipatesEvenWithFalseReceiveGuard() throws Exception {
        // Broadcast connectivity is unconditional, unlike multicast connectivity.
        assertCtl(translate("*", false, "TRUE", true), "EX (sender-sent & receiver-received)", true);
    }

    private String translate(String channel, boolean listening, String selection, boolean enabled) throws Exception {
        String model = source("non-listening-enabled-receive")
            .replace("c! (@ready)", channel + "! (" + selection + ")")
            .replace("c?", channel + "?")
            .replace("receive-guard: FALSE", "receive-guard: " + (listening ? "TRUE" : "FALSE"))
            .replace("receive: <value>", "receive: <" + (enabled ? "value" : "FALSE") + ">");
        return ToNuXmv.transform(parse(model));
    }
}
