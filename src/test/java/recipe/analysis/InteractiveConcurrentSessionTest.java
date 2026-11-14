package recipe.analysis;

import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveConcurrentSessionTest {
    @Test(timeout = 20000)
    public void concurrentCheckersKeepResponsesAndShutdownIndependent() throws Exception {
        var model = parse(source("supplier-locality"));
        NuXmvInteraction first = new NuXmvInteraction(model);
        NuXmvInteraction second = new NuXmvInteraction(model);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            assertNotSame(first.nuxmvTurn, second.nuxmvTurn);
            for (int i = 0; i < 4; i++) {
                CountDownLatch start = new CountDownLatch(1);
                Future<?> left = pool.submit(() -> {
                    start.await();
                    var result = first.modelCheck("sender-sent", false, -1);
                    assertTrue(result.getRight(), result.getLeft());
                    assertTrue(result.getRight(), result.getRight().contains("is false"));
                    return null;
                });
                Future<?> right = pool.submit(() -> {
                    start.await();
                    var result = second.modelCheck("!sender-sent", false, -1);
                    assertTrue(result.getRight(), result.getLeft());
                    assertTrue(result.getRight(), result.getRight().contains("is true"));
                    return null;
                });
                start.countDown();
                left.get(3, TimeUnit.SECONDS);
                right.get(3, TimeUnit.SECONDS);
            }
            first.stopNuXmvThread();
            var remaining = second.modelCheck("sender-sent", false, -1);
            assertTrue(remaining.getRight(), remaining.getLeft());
            assertTrue(remaining.getRight(), remaining.getRight().contains("is false"));
        } finally {
            first.stopNuXmvThread();
            second.stopNuXmvThread();
            pool.shutdownNow();
        }
    }
}
