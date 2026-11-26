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
        NuXmvInteraction first = new NuXmvInteraction(parse(source));
        NuXmvInteraction second = null;
        try {
            String before = first.modelCheckBmc("sender-sent", true, 1).getRight();
            assertTrue(before, before.contains("is false"));
            second = new NuXmvInteraction(parse(source.replace("init: !sent", "init: sent")));
            assertNotEquals(first.path, second.path);
            String secondResult = second.modelCheckBmc("sender-sent", true, 1).getRight();
            assertTrue(secondResult, secondResult.contains("no counterexample found with bound 1"));
            String after = first.modelCheckBmc("sender-sent", true, 1).getRight();
            assertTrue(after, after.contains("is false"));
        } finally {
            first.close();
            if (second != null) second.close();
        }
    }

}
