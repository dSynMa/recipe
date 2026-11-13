package recipe.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CheckerProcessCleanupTest {
    @Test(timeout = 20000)
    public void completedChecksLeaveNoRegisteredProcess() throws Exception {
        org.junit.Assume.assumeTrue(Files.isExecutable(Path.of("nuxmv/bin/nuxmv")));
        NuXmvBatch checker = new NuXmvBatch(parse(source("supplier-locality")));
        int before = NuXmvBatch.processes.size();
        try {
            assertTrue(checker.modelCheckBmc("sender-sent", true, 1).getLeft());
            assertEquals(before, NuXmvBatch.processes.size());
        } finally {
            Files.deleteIfExists(checker.path);
        }
    }

    @Test(timeout = 20000)
    public void failedChecksLeaveNoRegisteredProcess() throws Exception {
        org.junit.Assume.assumeTrue(Files.isExecutable(Path.of("nuxmv/bin/nuxmv")));
        NuXmvBatch checker = new NuXmvBatch(parse(source("supplier-locality")));
        int before = NuXmvBatch.processes.size();
        try {
            assertFalse(checker.modelCheckBmc("(", true, 1).getLeft());
            assertEquals(before, NuXmvBatch.processes.size());
        } finally {
            Files.deleteIfExists(checker.path);
        }
    }
}
