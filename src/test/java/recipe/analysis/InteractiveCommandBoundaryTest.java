package recipe.analysis;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveCommandBoundaryTest {
    @Test(timeout = 10000)
    public void completingACommandDoesNotQueueAnExtraBlankCommand() throws Exception {
        NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality"))) {
            @Override public Thread startNuXmvThread() { return new Thread(); }
        };
        checker.nuxmvTurn.set(false);
        var finished = new java.util.concurrent.CountDownLatch(1);
        Thread response = new Thread(() -> {
            try {
                while (checker.in.read() != '\n') { }
                while (!checker.nuxmvTurn.get()) Thread.onSpinWait();
                checker.byteArrayOutputStream.write("completed\nnuXmv >".getBytes(StandardCharsets.UTF_8));
                checker.nuxmvTurn.set(false);
                finished.await();
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        response.setDaemon(true);
        try {
            response.start();
            String output = checker.execute(text -> text.endsWith("nuXmv >"), "go_msat");
            assertTrue(output, output.contains("completed"));
            assertEquals("A blank command creates a stray prompt for the next request", 0, checker.in.available());
        } finally {
            finished.countDown();
            response.join(1000);
            response.interrupt();
            checker.nuxmvTurn.set(false);
            Files.deleteIfExists(checker.path);
        }
    }
}
