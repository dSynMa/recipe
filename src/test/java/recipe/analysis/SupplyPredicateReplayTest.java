package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SupplyPredicateReplayTest {
    private void check(String supply, String get, boolean expected) throws Exception {
        String text = source("send-observation-on-get")
            .replace("SUPPLY@(@ready)", "SUPPLY@(" + supply + ")")
            .replace("GET@TRUE", "GET@(" + get + ")");
        var model = parse(text);
        assertCtl(ToNuXmv.transform(model), "EX receiver-received", expected);
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        assertEquals(text, expected, !interpreter.isDeadlocked());
    }

    @Test(timeout = 20000)
    public void supplySelectionCombinesSupplierLocalsAndGetterCommonVariables() throws Exception {
        check("!sent & @ready", "TRUE", true);
        check("sent & @ready", "TRUE", false);
    }

    @Test(timeout = 20000)
    public void getSelectionCombinesGetterLocalsAndSupplierCommonVariables() throws Exception {
        check("@ready", "!received & @ready", true);
        check("@ready", "received & @ready", false);
    }
    @Test(timeout = 20000)
    public void receiverLocalsDoNotShadowSenderSelectionLocals() throws Exception {
        String text = source("observation-forall-constant")
            .replace("received: boolean", "received: boolean, sent: boolean")
            .replace("init: !received", "init: !received & sent")
            .replace("(@ready)", "(!sent & @ready)");
        var model = parse(text);
        assertCtl(ToNuXmv.transform(model), "EX receiver-received", true);
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        assertFalse(interpreter.isDeadlocked());
        var next = interpreter.getCurrentStep().next(0, interpreter).toJSON().getJSONObject("state");
        assertEquals("TRUE", next.getJSONObject("receiver").getString("received"));
    }
}
