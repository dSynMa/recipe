package recipe.analysis;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReceiverChoiceProductTest {
    private void assertIndependentChoices(boolean broadcast) throws Exception {
        String channel = broadcast ? "*" : "c";
        String text = source("supplier-locality")
            .replace("SUPPLY@first", channel + "! (@ready)")
            .replace("repeat: receive: <value> GET@TRUE [received := value]",
                "repeat: one: <TRUE> " + channel + "? [received := TRUE] + two: <TRUE> " + channel + "? [received := FALSE]");
        var model = parse(text);
        String smv = ToNuXmv.transform(model);
        for (boolean first : new boolean[]{false, true}) for (boolean second : new boolean[]{false, true})
            assertCtl(smv, "EX (sender-sent & " + (first ? "" : "!") + "first-received & "
                + (second ? "" : "!") + "second-received)", true);
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        var root = interpreter.getCurrentStep();
        int count = root.toJSON().getJSONArray("transitions").length();
        Set<String> successors = new HashSet<>();
        for (int i = 0; i < count; i++) {
            var state = root.next(i, interpreter).toJSON().getJSONObject("state");
            successors.add(state.getJSONObject("first").getString("received") + ":"
                + state.getJSONObject("second").getString("received"));
        }
        assertEquals(Set.of("TRUE:TRUE", "TRUE:FALSE", "FALSE:TRUE", "FALSE:FALSE"), successors);
        assertEquals(4, count);
    }

    @Test(timeout = 20000)
    public void multicastEnumeratesIndependentReceiverChoices() throws Exception { assertIndependentChoices(false); }

    @Test(timeout = 20000)
    public void broadcastEnumeratesIndependentReceiverChoices() throws Exception { assertIndependentChoices(true); }
}
