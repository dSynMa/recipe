package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InterpreterSystemReplacementTest {
    private void check(boolean initializePreviousSystem) throws Exception {
        String text = source("observation-forall-constant");
        var first = parse(text);
        Interpreter interpreter = new Interpreter(first);
        if (initializePreviousSystem) interpreter.init("TRUE");
        var current = parse(text.replace("channels: c", "channels: d").replace("c!", "d!")
            .replace("c?", "d?").replace("channel == c", "channel == d"));
        interpreter = new Interpreter(current);
        interpreter.init("TRUE");
        assertFalse(interpreter.isDeadlocked());
        interpreter.next(0);
        assertEquals("TRUE", interpreter.getCurrentStep().toJSON().getJSONObject("state")
            .getJSONObject("receiver").getString("received"));
        assertTrue("The connected but unselected receiver blocks another multicast", interpreter.isDeadlocked());
    }

    @Test(timeout = 20000)
    public void replacementBeforeInitializationUsesCurrentChannels() throws Exception { check(false); }

    @Test(timeout = 20000)
    public void replacementAfterInitializationUsesCurrentChannels() throws Exception { check(true); }
}
