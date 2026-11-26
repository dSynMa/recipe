package recipe.analysis;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CheckerExitStatusTest {
    @Test(timeout = 20000)
    public void unexpectedProcessExitIsNotASuccessfulCheck() throws Exception {
        Set<Long> before = ProcessHandle.current().children().map(ProcessHandle::pid).collect(Collectors.toSet());
        try (NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")))) {
            ProcessHandle child = ProcessHandle.current().children()
                .filter(p -> !before.contains(p.pid())).findFirst().orElseThrow();
            child.destroyForcibly();
            child.onExit().get(3, TimeUnit.SECONDS);
            assertThrows(IOException.class, () -> checker.modelCheckBmc("sender-sent", true, 1));
        }
    }
}
