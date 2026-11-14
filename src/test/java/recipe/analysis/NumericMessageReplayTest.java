package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class NumericMessageReplayTest {
    private void check(String type, String value, boolean supply) throws Exception {
        String send = supply ? "SUPPLY@(TRUE)" : "c! (TRUE)";
        String receive = supply ? "GET@TRUE" : "c?";
        String text = "channels: c\nmessage-structure: value: " + type + "\ncommunication-variables: ready: boolean\n"
            + "agent Sender\nlocal: sent: boolean, count: 0..2\ninit: !sent & count == 1\nrelabel: ready <- TRUE\nreceive-guard: FALSE\n"
            + "repeat: send: <TRUE> " + send + "(value := " + value + ")[sent := TRUE]\n"
            + "agent Receiver\nlocal: received: boolean\ninit: !received\nrelabel: ready <- TRUE\nreceive-guard: channel == c\n"
            + "repeat: receive: <value == 2> " + receive + " [received := TRUE]\n"
            + "system = Sender(sender, TRUE) | Receiver(receiver, TRUE)";
        var model = parse(text);
        String smv = ToNuXmv.transform(model);
        if (value.contains(".")) {
            assertSmtInvariant(smv, "progress & (sender-sent -> receiver-received)");
        } else {
            assertCtl(smv, "EX receiver-received", true);
        }
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        assertFalse(text, interpreter.isDeadlocked());
        assertEquals("TRUE", interpreter.getCurrentStep().next(0, interpreter).toJSON()
            .getJSONObject("state").getJSONObject("receiver").getString("received"));
    }

    @Test(timeout = 30000)
    public void numericSendPayloadsRetainTheirDeclaredType() throws Exception {
        for (String type : new String[]{"0..3", "integer", "real"})
            for (String value : new String[]{"2", "2.0", "count + 1", "count * 2"}) check(type, value, false);
    }

    @Test(timeout = 30000)
    public void numericSupplyPayloadsRetainTheirDeclaredType() throws Exception {
        for (String type : new String[]{"0..3", "integer", "real"})
            for (String value : new String[]{"2", "2.0", "count + 1", "count * 2"}) check(type, value, true);
    }
}
