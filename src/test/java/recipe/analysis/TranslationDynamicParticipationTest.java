package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationDynamicParticipationTest {
    private String twoSends() throws Exception {
        return source("observation-forall-constant")
            .replace("local: sent: boolean", "local: sent: boolean, count: 0..2")
            .replace("init: !sent", "init: !sent & count == 0")
            .replace("send: <TRUE>", "send: <count < 2>")
            .replace("[sent := TRUE]", "[sent := TRUE, count := count + 1]");
    }

    @Test public void receiverDisconnectsBeforeSecondMulticast() throws Exception {
        String model = twoSends()
            .replace("receive-guard: channel == c", "receive-guard: channel == c & !received")
            .replace("receive: <value>", "receive: <value & !received>");
        String smv = ToNuXmv.transform(parse(model));
        assertCtl(smv, "AX (sender-count = 1 & receiver-received)", true);
        assertCtl(smv, "AX AX (sender-count = 2 & receiver-received)", true);
    }

    @Test public void broadcastSkipsReceiverWhoseUpdateBecomesInfeasible() throws Exception {
        String model = twoSends()
            .replace("c! (@ready)", "*! (@ready)")
            .replace("c?", "*?")
            .replace("local: received: boolean", "local: received: boolean, count: 0..1")
            .replace("ready <- !received", "ready <- TRUE")
            .replace("init: !received", "init: !received & count == 0")
            .replace("[received := value]", "[received := value, count := count + 1]");
        String smv = ToNuXmv.transform(parse(model));
        assertCtl(smv, "AX (sender-count = 1 & receiver-count = 1)", true);
        assertCtl(smv, "AX AX (sender-count = 2 & receiver-count = 1)", true);
    }


    @Test public void competingSendersTakeSeparateJointTransitions() throws Exception {
        String model = source("observation-forall-constant")
            .replace("receive-guard: TRUE", "receive-guard: FALSE")
            .replace("send: <TRUE>", "send: <!sent>")
            .replace("Sender(sender, TRUE) | Receiver(receiver, TRUE)",
                "Sender(first, TRUE) | Sender(second, TRUE)");
        String smv = ToNuXmv.transform(parse(model));
        assertCtl(smv, "AX !(first-sent & second-sent)", true);
        assertCtl(smv, "EX EX (first-sent & second-sent)", true);
    }
}
