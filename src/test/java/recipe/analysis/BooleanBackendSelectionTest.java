package recipe.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class BooleanBackendSelectionTest {
    private static class RecordingChecker extends NuXmvInteraction {
        final List<String> commands = new ArrayList<>();
        RecordingChecker(recipe.lang.System system) throws Exception { super(system); }
        @Override public synchronized String execute(Predicate<String> done, String command) throws IOException {
            commands.add(command);
            return super.execute(done, command);
        }
    }

    private String finiteModel(String update) throws Exception {
        return source("arithmetic-multiplication")
                .replace("count: integer", "count: 0..3")
                .replace("count == 2", "count == 1")
                .replace("count * 2", update);
    }

    @Test(timeout = 30000)
    public void finiteChecksUseBooleanIc3AndCanSwitchToSmtForAProperty() throws Exception {
        try (var checker = new RecordingChecker(parse(finiteModel("count")))) {
            var ordinary = checker.modelCheck("G (sender-count = 1)", false, -1);
            assertTrue(ordinary.getRight(), ordinary.getLeft());
            assertTrue(checker.commands.contains("go"));
            for (boolean bounded : new boolean[] {true, false}) {
                var result = checker.modelCheckIc3("G (sender-count = 1)", bounded, 20);
                assertTrue(result.getRight(), result.getLeft());
                assertTrue(result.getRight(), result.getRight().contains("is true"));
                assertEquals(bounded, checker.commands.get(checker.commands.size() - 1).contains(" -i"));
            }
            var real = checker.modelCheckIc3("G ((1.0 * sender-count) / 2 = 0.5)", false, -1);
            assertTrue(real.getRight(), real.getLeft());
            assertTrue(real.getRight(), real.getRight().contains("is true"));
            assertTrue(checker.commands.get(checker.commands.size() - 1).contains(" -i"));
            var again = checker.modelCheckIc3("G (sender-count = 1)", false, -1);
            assertTrue(again.getRight(), again.getLeft());
            assertEquals(1L, checker.commands.stream().filter("build_boolean_model"::equals).count());
        }
    }

    @Test(timeout = 30000)
    public void generatedRealGuardsFallBackAndInfiniteDomainsSkipBooleanBuilding() throws Exception {
        for (String model : new String[] {finiteModel("count / 2"),
                source("arithmetic-multiplication"),
                source("arithmetic-multiplication").replace("count: integer", "count: real")}) {
            try (var checker = new RecordingChecker(parse(model))) {
                var result = checker.modelCheckIc3("TRUE", false, -1);
                assertTrue(result.getRight(), result.getLeft());
                assertTrue(result.getRight(), result.getRight().contains("is true"));
                assertEquals(!checker.system.isSymbolic(), checker.commands.contains("build_boolean_model"));
                assertTrue(checker.commands.get(checker.commands.size() - 1).contains(" -i"));
            }
        }
    }

    @Test(timeout = 30000)
    public void ordinaryCheckingAlsoRoutesRealPropertiesToSmt() throws Exception {
        try (var checker = new RecordingChecker(parse(finiteModel("count")))) {
            var result = checker.modelCheck("G ((1.0 * sender-count) / 2 = 0.5)", false, -1);
            assertTrue(result.getRight(), result.getLeft());
            assertTrue(result.getRight(), result.getRight().contains("is true"));
            assertTrue(checker.commands.contains("go"));
            assertFalse(checker.commands.contains("build_boolean_model"));
        }
    }

    @Test(timeout = 30000)
    public void bmcAndSimulationDoNotBuildAnUnusedBooleanModel() throws Exception {
        try (var checker = new RecordingChecker(parse(finiteModel("count")))) {
            assertTrue(checker.simulation_pick_init_state("TRUE").getLeft());
            var result = checker.modelCheckBmc("G (sender-count = 1)", true, 3);
            assertTrue(result.getRight(), result.getLeft());
            assertFalse(checker.commands.contains("build_boolean_model"));
        }
    }
    @Test(timeout = 30000)
    public void rejectedModelIsNotBuiltAgain() throws Exception {
        try (var checker = new RecordingChecker(parse(finiteModel("count / 2")))) {
            for (int attempt = 0; attempt < 2; attempt++) {
                var result = checker.modelCheckIc3("G !progress", false, -1);
                assertTrue(result.getRight(), result.getLeft());
                assertTrue(result.getRight(), result.getRight().contains("is true"));
            }
            assertEquals(1L, checker.commands.stream().filter("build_boolean_model"::equals).count());
        }
        try (var checker = new RecordingChecker(parse(finiteModel("count / 2")))) {
            var result = checker.modelCheck("G !progress", false, -1);
            assertTrue(result.getRight(), result.getLeft());
            assertTrue(result.getRight(), result.getRight().contains("is true"));
        }
    }

    @Test(timeout = 30000)
    public void malformedPropertiesRemainErrorsWithoutAnSmtRetry() throws Exception {
        try (var checker = new RecordingChecker(parse(finiteModel("count")))) {
            var result = checker.modelCheckIc3("G (", false, -1);
            assertFalse(result.getRight(), result.getLeft());
            assertFalse(checker.commands.stream().anyMatch(command -> command.startsWith("check_ltlspec_ic3 -i")));
        }
    }
}
