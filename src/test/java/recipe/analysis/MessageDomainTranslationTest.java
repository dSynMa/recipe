package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class MessageDomainTranslationTest {
    private void check(String mode, String domain, String payload, boolean enabled) throws Exception {
        boolean supply = mode.equals("supply");
        String send = supply ? "SUPPLY@(TRUE)" : mode + "! (TRUE)";
        String receive = supply ? "GET@TRUE" : mode + "?";
        String text = "channels: c\nmessage-structure: value: " + domain + "\ncommunication-variables: ready: boolean\n"
            + "agent Sender\nlocal: sent: boolean, count: 0..2\ninit: !sent & count == 1\n"
            + "relabel: ready <- TRUE\nreceive-guard: FALSE\nrepeat: send: <TRUE> " + send
            + "(value := " + payload + ")[sent := TRUE]\n"
            + "agent Receiver\nlocal: received: boolean\ninit: !received\nrelabel: ready <- TRUE\nreceive-guard: TRUE\n"
            + "repeat: receive: <TRUE> " + receive + "[received := TRUE]\n"
            + "system = Sender(sender, TRUE) | Receiver(receiver, TRUE)";
        var model = parse(text);
        String smv = ToNuXmv.transform(model);
        assertCtl(smv, "EX sender-sent", enabled);
        assertCtl(smv, "progress", enabled);
        assertCtl(smv, "AG EX TRUE", true);
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        assertEquals(enabled, !interpreter.isDeadlocked());
    }

    @Test(timeout=30000)
    public void boundedPayloadCannotExceedEitherEndpoint() throws Exception {
        for (String mode : new String[]{"c", "*", "supply"}) {
            check(mode, "0..1", "count + 1", false);
            check(mode, "0..1", "count - 2", false);
        }
    }

    @Test(timeout=30000)
    public void legalBoundedPayloadRemainsEnabled() throws Exception {
        for (String mode : new String[]{"c", "*", "supply"}) check(mode, "0..1", "count - 1", true);
    }

    @Test(timeout=30000)
    public void integerPayloadMustBeIntegralEvenIfReceiverIgnoresIt() throws Exception {
        for (String mode : new String[]{"c", "*", "supply"}) check(mode, "integer", "0.5", false);
    }
}
