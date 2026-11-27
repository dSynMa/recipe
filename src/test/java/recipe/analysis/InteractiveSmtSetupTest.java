package recipe.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveSmtSetupTest {
    @Test(timeout = 30000)
    public void simulationSetupSupportsRealUpdateFeasibility() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..3")
            .replace("count == 2", "count == 1")
            .replace("count * 2", "count / 2");
        StringBuilder commands = new StringBuilder();
        NuXmvInteraction checker = new NuXmvInteraction(parse(model)) {
            @Override public Thread startNuXmvThread() { return new Thread(); }
            @Override public String execute(Predicate<String> done, String command) {
                commands.append(command).append('\n');
                return "nuXmv >";
            }
        };
        Path script = Files.createTempFile("interactive-smt-", ".cmd");
        Path log = Files.createTempFile("interactive-smt-", ".log");
        Process process = null;
        try {
            checker.initialise(true);
            commands.append("msat_pick_state -v\nquit\n");
            Files.writeString(script, commands);
            process = new ProcessBuilder(nuxmvExecutable().toString(), "-source", script.toString(), checker.path.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            process.getOutputStream().close();
            assertTrue(process.waitFor(20, TimeUnit.SECONDS));
            String output = Files.readString(log);
            assertEquals(output, 0, process.exitValue());
            assertFalse(output, output.contains("aborting"));
            assertTrue(output, output.contains("sender-count = 1"));
            assertTrue(output, output.contains("progress = FALSE"));
        } finally {
            if (process != null) process.destroyForcibly();
            Files.deleteIfExists(script);
            Files.deleteIfExists(log);
            Files.deleteIfExists(checker.path);
        }
    }
}
