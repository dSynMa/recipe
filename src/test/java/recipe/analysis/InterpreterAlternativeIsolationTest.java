package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InterpreterAlternativeIsolationTest {
    private void check(boolean supply) throws Exception {
        for (boolean missingUpdate : new boolean[]{false, true}) for (int i = 0; i < 6; i++) {
            String text = source("observation-forall-constant").replace("SPEC G (<forall(TRUE)>true);", "")
                .replace("value: boolean", "value: boolean, absent: boolean");
            if (supply) text = text.replace("c! (@ready)", "SUPPLY@(@ready)");
            String action = supply ? "GET@TRUE" : "c?";
            text = text.replace("repeat: receive: <value> c? [received := value]",
                "repeat: bad" + i + ": <" + (missingUpdate ? "value" : "absent") + "> " + action
                    + " [received := " + (missingUpdate ? "absent" : "FALSE") + "] + good" + i
                    + ": <value> " + action + " [received := value]");
            var model = parse(text);
            assertCtl(ToNuXmv.transform(model), "EX receiver-received", true);
            Interpreter interpreter = new Interpreter(model);
            interpreter.init("TRUE");
            var root = interpreter.getCurrentStep();
            assertEquals(text, 1, root.toJSON().getJSONArray("transitions").length());
            assertEquals("TRUE", root.next(0, interpreter).toJSON().getJSONObject("state")
                .getJSONObject("receiver").getString("received"));
        }
    }

    @Test(timeout = 60000)
    public void invalidReceiveDoesNotSuppressValidAlternative() throws Exception { check(false); }

    @Test(timeout = 60000)
    public void invalidGetDoesNotSuppressValidAlternative() throws Exception { check(true); }
}
