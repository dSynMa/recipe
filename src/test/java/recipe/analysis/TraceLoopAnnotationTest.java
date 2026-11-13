package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TraceLoopAnnotationTest {
    private void assertLoopAt(int loopIndex) throws Exception {
        assertLoopAt(loopIndex, 1, "\n");
    }

    private void assertLoopAt(int loopIndex, int traceNumber, String newline) throws Exception {
        var model = parse(source("supplier-locality")
            .replace("sent := TRUE", "sent := sent")
            .replace("received := value", "received := received"));
        StringBuilder trace = new StringBuilder("Trace Type: Counterexample\n");
        for (int i = 0; i < 3; i++) {
            if (i == loopIndex) trace.append("-- Loop starts here\n");
            trace.append("-> State: ").append(traceNumber).append(".").append(i + 1).append(" <-\n")
                .append("  sender-sent = FALSE\n  first-received = FALSE\n  second-received = FALSE\n");
        }
        var states = Interpreter.ofTrace(model, trace.toString().replace("\n", newline)).traceToJSON();
        assertEquals(3, states.size());
        for (int i = 0; i < states.size(); i++) {
            assertEquals(states.toString(), i == loopIndex, states.get(i).optBoolean("___LOOP___", false));
        }
    }

    @Test(timeout = 20000)
    public void loopStartingAtTheInitialStateIsPreserved() throws Exception {
        assertLoopAt(0);
    }

    @Test(timeout = 20000)
    public void loopStartDoesNotPropagateToLaterStates() throws Exception {
        assertLoopAt(1);
    }
    @Test(timeout = 20000)
    public void laterTraceNumbersPreserveLoopStarts() throws Exception {
        assertLoopAt(1, 2, "\n");
    }
    @Test(timeout = 20000)
    public void windowsLineEndingsPreserveLoopStarts() throws Exception {
        assertLoopAt(1, 1, "\r\n");
    }
}
