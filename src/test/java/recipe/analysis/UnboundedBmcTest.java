package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class UnboundedBmcTest {
    @Test(timeout = 20000)
    public void incrementalCheckingFindsCounterexamplesBeyondDefaultBound() throws Exception {
        String text = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..20")
            .replace("count == 2", "count == 0")
            .replace("count * 2", "count + 1")
            .replace("ready <- !received", "ready <- TRUE");
        NuXmvInteraction checker = new NuXmvInteraction(parse(text));
        try {
            var result = checker.modelCheckBmc("G (sender-count < 12)", false, 3);
            assertTrue(result.getRight(), result.getLeft());
            assertTrue(result.getRight(), result.getRight().contains("is false"));
            var proof = checker.modelCheckBmc("G (sender-count <= 20)", false, 3);
            assertTrue(proof.getRight(), proof.getLeft());
            assertTrue(proof.getRight(), proof.getRight().contains("is true"));
        } finally {
            checker.close();
        }
    }
}
