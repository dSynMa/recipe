package recipe.analysis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CliSimulationInputTest {
    private String simulate(String input) throws Exception {
        return simulate(input, false);
    }

    private String simulate(String input, boolean checkFirst) throws Exception {
        Path model = Files.createTempFile("cli-simulation-", ".rcp");
        Path log = Files.createTempFile("cli-simulation-", ".log");
        Process process = null;
        try {
            Files.writeString(model, checkFirst
                ? source("arithmetic-multiplication") + "\nSPEC G (sender-count >= 2);\n"
                : source("supplier-locality"));
            java.util.List<String> command = new java.util.ArrayList<>(java.util.List.of(
                Path.of(java.lang.System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", java.lang.System.getProperty("java.class.path"), "CLIApp", "-i", model.toString(), "-sim"));
            if (checkFirst) command.add("-mc");
            process = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();
            assertTrue("CLI timed out", process.waitFor(20, TimeUnit.SECONDS));
            String output = Files.readString(log);
            assertEquals(output, 0, process.exitValue());
            assertFalse(output, output.contains("syntax error"));
            return output;
        } finally {
            if (process != null) process.destroyForcibly();
            Files.deleteIfExists(model);
            Files.deleteIfExists(log);
        }
    }

    @Test(timeout = 30000)
    public void constraintsWithSpacesAreReadAsWholeLines() throws Exception {
        String output = simulate("!sender-sent & !first-received\nsender-sent & first-received\nFALSE\n");
        assertTrue(output, output.contains("sender-sent = TRUE"));
        assertTrue(output, output.contains("first-received = TRUE"));
        assertTrue(output, output.contains("No reachable states."));
    }

    @Test(timeout = 30000)
    public void endOfInputBeforeInitialSelectionExitsCleanly() throws Exception {
        simulate("");
    }

    @Test(timeout = 30000)
    public void endOfInputAfterInitialSelectionExitsCleanly() throws Exception {
        simulate("TRUE\n");
    }
    @Test(timeout = 30000)
    public void simulationAfterSymbolicCheckingReadsTheNextConstraintLine() throws Exception {
        String output = simulate("2\nsender-count = 2 & !sender-sent\nsender-count = 4 & sender-sent\nFALSE\n", true);
        assertTrue(output, output.contains("no counterexample found with bound 2"));
        assertTrue(output, output.contains("sender-count = 4"));
    }
}
