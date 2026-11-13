package recipe.analysis;

import java.io.IOException;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InterpreterInitializationFailureTest {
    @Test(timeout = 20000)
    public void inconsistentConstraintPreservesTheCheckerDiagnostic() throws Exception {
        Interpreter interpreter = new Interpreter(parse(source("supplier-locality")));
        IOException error = assertThrows(IOException.class, () -> interpreter.init("FALSE"));
        assertTrue(error.getMessage(), error.getMessage().contains("No trace"));
        assertNull(interpreter.getCurrentStep());
    }

    @Test(timeout = 20000)
    public void malformedConstraintPreservesTheCheckerDiagnostic() throws Exception {
        Interpreter interpreter = new Interpreter(parse(source("supplier-locality")));
        IOException error = assertThrows(IOException.class, () -> interpreter.init("("));
        assertTrue(error.getMessage(), error.getMessage().contains("syntax error"));
        assertNull(interpreter.getCurrentStep());
    }

    @Test(timeout = 20000)
    public void failedResetDoesNotReplaceAValidCurrentStep() throws Exception {
        Interpreter interpreter = new Interpreter(parse(source("supplier-locality")));
        interpreter.init("TRUE");
        var initial = interpreter.getCurrentStep();
        assertNotNull(initial);
        assertThrows(IOException.class, () -> interpreter.init("FALSE"));
        assertSame(initial, interpreter.getCurrentStep());
        assertFalse(interpreter.isDeadlocked());
    }
}
