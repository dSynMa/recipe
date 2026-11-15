package recipe.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveInitializationReuseTest {
    @Test(timeout = 10000)
    public void repeatedChecksAndSimulationInitializeEachBackendOnlyOnce() throws Exception {
        List<String> commands = new ArrayList<>();
        NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality"))) {
            @Override public String execute(Predicate<String> finished, String command) throws IOException {
                commands.add(command);
                return super.execute(finished, command);
            }
        };
        try {
            assertTrue(checker.modelCheck("!sender-sent", false, 0).getLeft());
            assertTrue(checker.modelCheck("sender-sent", false, 0).getLeft());
            assertTrue(checker.simulation_pick_init_state("TRUE").getLeft());
            assertTrue(checker.initialise(true).getLeft());
            assertTrue(checker.modelCheck("!sender-sent", false, 0).getLeft());
            assertEquals(1, commands.stream().filter("go"::equals).count());
            assertEquals(1, commands.stream().filter("go_msat"::equals).count());
        } finally {
            checker.stopNuXmvThread();
        }
    }
    @Test(timeout = 10000)
    public void failedInitializationCanBeRetried() throws Exception {
        List<String> commands = new ArrayList<>();
        NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality"))) {
            @Override public String execute(Predicate<String> finished, String command) throws IOException {
                commands.add(command);
                if (commands.size() == 1) return "file model.smv: line 1: initialization failed\nnuXmv >";
                return super.execute(finished, command);
            }
        };
        try {
            assertFalse(checker.initialise(false).getLeft());
            assertTrue(checker.initialise(false).getLeft());
            assertTrue(checker.initialise(false).getLeft());
            assertEquals(java.util.List.of("go", "go"), commands);
        } finally {
            checker.stopNuXmvThread();
        }
    }
}
