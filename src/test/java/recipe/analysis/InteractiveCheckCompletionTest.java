package recipe.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveCheckCompletionTest {
    // Replay production commands in nuXmv; verify the completion predicate against
    // the resulting output and prompt without involving the interactive transport.
    private NuXmvInteraction checker() throws Exception {
        return checker(source("supplier-locality"));
    }

    private NuXmvInteraction checker(String model) throws Exception {
        return new NuXmvInteraction(parse(model)) {
            private final StringBuilder commands = new StringBuilder();
            @Override public Thread startNuXmvThread() { return new Thread(); }
            @Override public String execute(Predicate<String> done, String command) throws IOException {
                commands.append(command).append('\n');
                Path script = Files.createTempFile("completion-", ".cmd");
                Path log = Files.createTempFile("completion-", ".log");
                Process process = null;
                try {
                    Files.writeString(script, commands + "quit\n");
                    process = new ProcessBuilder("nuxmv/bin/nuxmv", "-source", script.toString(), path.toString())
                        .redirectErrorStream(true).redirectOutput(log.toFile()).start();
                    process.getOutputStream().close();
                    assertTrue(process.waitFor(10, TimeUnit.SECONDS));
                    String output = Files.readString(log) + "\nnuXmv >";
                    assertTrue("Completion predicate would hang after command: " + command + "\n" + output, done.test(output));
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
    public void interactiveIc3UsesThePreparedSmtModel() throws Exception {
        NuXmvInteraction checker = checker();
        try {
            var result = checker.modelCheckic3("sender-sent", true, 4);
            assertTrue(result.getRight(), result.getLeft());
            assertTrue(result.getRight(), result.getRight().contains("is false"));
        } finally { Files.deleteIfExists(checker.path); }
    }
    @Test(timeout = 30000)
    public void unboundedSymbolicCheckingDoesNotPassANegativeBmcBound() throws Exception {
        for (String property : new String[] {"sender-sent", "G (sender-count >= 2)"}) {
            NuXmvInteraction checker = checker(source("arithmetic-multiplication"));
            try {
                var result = checker.modelCheck(property, false, -1);
                assertTrue(result.getRight(), result.getLeft());
                assertTrue(result.getRight(), result.getRight().contains(
                    property.equals("sender-sent") ? "is false" : "is true"));
            } finally { Files.deleteIfExists(checker.path); }
        }
    }

    @Test(timeout = 30000)
    public void explicitSymbolicBoundStillUsesBoundedChecking() throws Exception {
        NuXmvInteraction checker = checker(source("arithmetic-multiplication"));
        try {
            var result = checker.modelCheck("G (sender-count >= 2)", false, 2);
            assertTrue(result.getRight(), result.getLeft());
            assertTrue(result.getRight(), result.getRight().contains("no counterexample found with bound 2"));
        } finally { Files.deleteIfExists(checker.path); }
    }
}
