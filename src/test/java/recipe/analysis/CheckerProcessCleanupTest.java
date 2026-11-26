package recipe.analysis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CheckerProcessCleanupTest {
    private void check(String property, boolean successful) throws Exception {
        Set<Long> before = ProcessHandle.current().children().map(ProcessHandle::pid).collect(Collectors.toSet());
        List<ProcessHandle> children;
        try (NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")))) {
            children = ProcessHandle.current().children().filter(p -> !before.contains(p.pid())).collect(Collectors.toList());
            assertFalse(children.isEmpty());
            assertEquals(successful, checker.modelCheckBmc(property, true, 1).getLeft());
        }
        assertTrue("Closing the checker must reap its process", children.stream().noneMatch(ProcessHandle::isAlive));
    }

    @Test(timeout = 20000)
    public void completedChecksLeaveNoProcessAfterClosing() throws Exception {
        check("sender-sent", true);
    }

    @Test(timeout = 20000)
    public void failedChecksLeaveNoProcessAfterClosing() throws Exception {
        check("(", false);
    }
}
