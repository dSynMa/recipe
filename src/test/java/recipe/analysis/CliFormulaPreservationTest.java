package recipe.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CliFormulaPreservationTest {
    private void assertCli(String property, String expected) throws Exception {
        Path model = Files.createTempFile("cli-formula-", ".rcp");
        Path log = Files.createTempFile("cli-formula-", ".log");
        Process process = null;
        try {
            Files.writeString(model, source("supplier-locality") + "\nSPEC " + property + ";\n");
            process = new ProcessBuilder(Path.of(java.lang.System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", java.lang.System.getProperty("java.class.path"), "CLIApp", "-i", model.toString(), "-bmc", "2")
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            process.getOutputStream().close();
            assertTrue("CLI timed out", process.waitFor(20, TimeUnit.SECONDS));
            String output = Files.readString(log);
            assertEquals(output, 0, process.exitValue());
            assertFalse(output, output.contains("syntax error"));
            assertTrue(output, output.contains(expected));
        } finally {
            if (process != null) process.destroyForcibly();
            Files.deleteIfExists(model);
            Files.deleteIfExists(log);
        }
    }
    @Test(timeout = 30000)
    public void boundedCliCheckingPreservesTheEntireFormula() throws Exception {
        assertCli("sender-sent | !sender-sent", "no counterexample found with bound 2");
    }

    @Test(timeout = 30000)
    public void boundedCliCheckingPreservesTemporalOperators() throws Exception {
        assertCli("G !sender-sent", "is false");
    }
}
