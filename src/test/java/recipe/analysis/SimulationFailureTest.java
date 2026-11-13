package recipe.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SimulationFailureTest {
    private NuXmvInteraction checker() throws Exception {
        return new NuXmvInteraction(parse(source("supplier-locality"))) {
            private final StringBuilder previous = new StringBuilder();
            @Override public Thread startNuXmvThread() { return new Thread(); }
            @Override public String execute(Predicate<String> done, String command) throws IOException {
                Path script = Files.createTempFile("simulation-result-", ".cmd");
                Path log = Files.createTempFile("simulation-result-", ".log");
                Process process = null;
                try {
                    Files.writeString(script, previous + "echo RESULT_START\n" + command + "\nquit\n");
                    process = new ProcessBuilder("nuxmv/bin/nuxmv", "-source", script.toString(), path.toString())
                        .redirectErrorStream(true).redirectOutput(log.toFile()).start();
                    process.getOutputStream().close();
                    assertTrue(process.waitFor(10, TimeUnit.SECONDS));
                    String output = Files.readString(log);
                    int start = output.indexOf("RESULT_START");
                    assertTrue(output, start >= 0);
                    output = output.substring(output.indexOf('\n', start) + 1) + "\nnuXmv >";
                    assertTrue(output, done.test(output));
                    previous.append(command).append('\n');
                    return output;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                } finally {
                    if (process != null) process.destroyForcibly();
                    Files.deleteIfExists(script);
                    Files.deleteIfExists(log);
                }
            }
        };
    }

    @Test(timeout = 30000)
    public void invalidInitialConstraintDoesNotStartASimulation() throws Exception {
        NuXmvInteraction checker = checker();
        try {
            var result = checker.simulation_pick_init_state("(");
            assertFalse(result.getRight(), result.getLeft());
            assertTrue(result.getRight(), result.getRight().contains("syntax error"));
            assertFalse(checker.simulationStarted);
        } finally { Files.deleteIfExists(checker.path); }
    }

    @Test(timeout = 30000)
    public void invalidStepConstraintDoesNotReturnASuccessfulEmptyState() throws Exception {
        NuXmvInteraction checker = checker();
        try {
            assertTrue(checker.simulation_pick_init_state("TRUE").getLeft());
            var result = checker.simulation_next("(");
            assertFalse(result.getRight(), result.getLeft());
            assertTrue(result.getRight(), result.getRight().contains("syntax error"));
        } finally { Files.deleteIfExists(checker.path); }
    }

    @Test(timeout = 30000)
    public void validSimulationStillReturnsInitialAndSuccessorStates() throws Exception {
        NuXmvInteraction checker = checker();
        try {
            var initial = checker.simulation_pick_init_state("TRUE");
            assertTrue(initial.getRight(), initial.getLeft());
            assertTrue(initial.getRight(), initial.getRight().contains("sender-sent = FALSE"));
            var next = checker.simulation_next("TRUE");
            assertTrue(next.getRight(), next.getLeft());
            assertTrue(next.getRight(), next.getRight().contains("sender-sent = TRUE"));
        } finally { Files.deleteIfExists(checker.path); }
    }
    @Test(timeout = 30000)
    public void inconsistentInitialConstraintStillReportsFailure() throws Exception {
        NuXmvInteraction checker = checker();
        try {
            var result = checker.simulation_pick_init_state("FALSE");
            assertFalse(result.getRight(), result.getLeft());
            assertFalse(checker.simulationStarted);
        } finally { Files.deleteIfExists(checker.path); }
    }

}
