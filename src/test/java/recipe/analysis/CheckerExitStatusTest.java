package recipe.analysis;

import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CheckerExitStatusTest {
    @Test(timeout = 20000)
    public void unexpectedProcessExitIsNotASuccessfulCheck() throws Exception {
        NuXmvBatch checker = new NuXmvBatch(parse(source("supplier-locality")));
        Process process = new ProcessBuilder("sh", "-c", "echo 'solver terminated unexpectedly'; exit 7")
            .redirectErrorStream(true).start();
        try {
            var read = NuXmvBatch.class.getDeclaredMethod("readFrom", Process.class);
            read.setAccessible(true);
            Object result = read.invoke(checker, process);
            var success = result.getClass().getDeclaredMethod("success");
            success.setAccessible(true);
            assertEquals(false, success.invoke(result));
            var output = result.getClass().getDeclaredField("output");
            output.setAccessible(true);
            assertTrue(output.get(result).toString(), output.get(result).toString().contains("7"));
        } finally {
            process.destroyForcibly();
            Files.deleteIfExists(checker.path);
        }
    }
}
