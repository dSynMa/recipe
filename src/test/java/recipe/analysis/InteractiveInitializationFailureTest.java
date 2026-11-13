package recipe.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveInitializationFailureTest {
    private NuXmvInteraction checker(String model) throws Exception {
        return new NuXmvInteraction(parse(model)) {
            @Override public Thread startNuXmvThread() { return new Thread(); }
            @Override public String execute(Predicate<String> done, String command) throws IOException {
                Path script = Files.createTempFile("initialization-", ".cmd");
                Path log = Files.createTempFile("initialization-", ".log");
                Process process = null;
                try {
                    Files.writeString(script, command + "\nquit\n");
                    process = new ProcessBuilder("nuxmv/bin/nuxmv", "-source", script.toString(), path.toString())
                        .redirectErrorStream(true).redirectOutput(log.toFile()).start();
                    process.getOutputStream().close();
                    assertTrue(process.waitFor(10, TimeUnit.SECONDS));
                    String output = Files.readString(log);
                    // Interactive mode prints the diagnostic without source-script abortion.
                    int abort = output.indexOf("aborting 'source");
                    if (abort >= 0) output = output.substring(0, abort);
                    output += "\nnuXmv >";
                    assertTrue(output, done.test(output));
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
    public void bddSetupFailureIsNotReportedAsSuccess() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..3")
            .replace("count == 2", "count == 1")
            .replace("count * 2", "count / 2");
        NuXmvInteraction checker = checker(model);
        try {
            var result = checker.initialise(false);
            assertFalse(result.getRight(), result.getLeft());
            assertTrue(result.getRight(), result.getRight().contains("not suitable constants"));
        } finally { Files.deleteIfExists(checker.path); }
    }

    @Test(timeout = 30000)
    public void ordinarySetupWarningsRemainSuccessful() throws Exception {
        NuXmvInteraction checker = checker(source("supplier-locality").replace("Sender(sender,", "Sender(error,"));
        try {
            for (boolean smt : new boolean[] {false, true}) {
                var result = checker.initialise(smt);
                assertTrue(result.getRight(), result.getLeft());
                assertTrue(result.getRight(), result.getRight().contains("WARNING:"));
            }
        } finally { Files.deleteIfExists(checker.path); }
    }
}
