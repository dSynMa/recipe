package recipe.analysis;

import java.io.IOException;
import java.util.function.Predicate;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveCheckErrorTest {
    @Test(timeout = 10000)
    public void divisionDiagnosticIsNotAValidCheck() throws Exception {
        var model = parse(source("arithmetic-multiplication"));
        try (NuXmvInteraction checker = new NuXmvInteraction(model)) {
            // Trigger a checker diagnostic during counterexample construction.
            String smv = java.nio.file.Files.readString(checker.path);
            java.nio.file.Files.writeString(checker.path, smv.replace("DEFINE\n",
                "DEFINE\n broken := (1.0 * sender-count) / (sender-count - 2);\n"));
            var result = checker.modelCheckBmc("sender-sent", true, 3);
            assertTrue(result.getRight(), result.getRight().contains("is false"));
            assertTrue(result.getRight(), result.getRight().contains("There is a division by 0"));
            assertFalse("The failed counterexample must not be reported as a valid verdict", result.getLeft());
        }
    }

    @Test(timeout = 10000)
    public void diagnosticAfterPartialProgressIsAnErrorForBmcAndIc3() throws Exception {
        for (String diagnostic : new String[] {
                "*** internal error ***", "Type System Violation", "syntax error",
                "Parsing error", "cannot be converted into INVARSPEC",
                "Impossible to build a boolean FSM with infinite precision variables"}) {
            try (NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality"))) {
                @Override public String execute(Predicate<String> finished, String command) throws IOException {
                    if (command.startsWith("go")) return super.execute(finished, command);
                    return "-- no counterexample found with bound 0\n" + diagnostic + "\nnuXmv >";
                }
            }) {
                var bmc = checker.modelCheckBmc("!sender-sent", true, 3);
                assertFalse(diagnostic, bmc.getLeft());
                assertTrue(bmc.getRight().contains(diagnostic));
                assertFalse(diagnostic, checker.modelCheckIc3("!sender-sent", true, 3).getLeft());
            }
        }
    }

    @Test(timeout = 10000)
    public void transportRetainsInternalErrorDiagnostics() throws Exception {
        try (NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")))) {
            String output = checker.execute(text -> text.trim().endsWith("nuXmv >"),
                "echo \"*** internal error ***\"");
            assertTrue(output, output.contains("*** internal error ***"));
        }
    }
}
