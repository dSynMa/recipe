package recipe.analysis;

import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CheckerModelIsolationTest {
    @Test(timeout = 20000)
    public void creatingAnotherCheckerDoesNotReplaceTheFirstModel() throws Exception {
        org.junit.Assume.assumeTrue(Files.isExecutable(java.nio.file.Path.of("nuxmv/bin/nuxmv")));
        String source = source("supplier-locality");
        NuXmvBatch first = new NuXmvBatch(parse(source));
        NuXmvBatch second = null;
        try {
            String before = first.modelCheckBmc("sender-sent", true, 1).getRight();
            assertTrue(before, before.contains("is false"));
            second = new NuXmvBatch(parse(source.replace("init: !sent", "init: sent")));
            assertNotEquals(first.path, second.path);
            String secondResult = second.modelCheckBmc("sender-sent", true, 1).getRight();
            assertTrue(secondResult, secondResult.contains("no counterexample found with bound 1"));
            String after = first.modelCheckBmc("sender-sent", true, 1).getRight();
            assertTrue(after, after.contains("is false"));
        } finally {
            Files.deleteIfExists(first.path);
            if (second != null) Files.deleteIfExists(second.path);
        }
    }

    @Test
    public void interactiveAndBatchCheckersOwnDifferentFiles() throws Exception {
        NuXmvBatch batch = new NuXmvBatch(parse(source("supplier-locality")));
        NuXmvInteraction interactive = null;
        try {
            String original = Files.readString(batch.path);
            // Test file ownership without starting the interactive transport thread.
            interactive = new NuXmvInteraction(parse(source("supplier-locality").replace("init: !sent", "init: sent"))) {
                @Override public Thread startNuXmvThread() { return new Thread(); }
            };
            assertNotEquals(batch.path, interactive.path);
            assertEquals(original, Files.readString(batch.path));
            assertNotEquals(original, Files.readString(interactive.path));
        } finally {
            Files.deleteIfExists(batch.path);
            if (interactive != null) Files.deleteIfExists(interactive.path);
        }
    }
}
