package recipe.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveModelCheckingTest {
    @Test(timeout = 20000)
    public void interactiveCheckingReturnsTheActualVerdictAndPreservesExistingTranslation() throws Exception {
        Path legacy = Path.of("translation.smv");
        byte[] before = Files.exists(legacy) ? Files.readAllBytes(legacy) : null;
        try {
            for (String property : new String[] {"sender-sent", "!sender-sent"}) {
                NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")));
                String output;
                try {
                    var result = checker.modelCheck(property, false, 0);
                    assertTrue(result.getRight(), result.getLeft());
                    output = result.getRight();
                } finally {
                    checker.stopNuXmvThread();
                }
                String expected = property.startsWith("!") ? "true" : "false";
                assertTrue(output, output.contains(" is " + expected));
                if (before == null) assertFalse(Files.exists(legacy));
                else assertArrayEquals(before, Files.readAllBytes(legacy));
            }
        } finally {
            // Preserve the caller's file even when testing the buggy implementation.
            if (before == null) Files.deleteIfExists(legacy);
            else if (!java.util.Arrays.equals(before, Files.readAllBytes(legacy))) Files.write(legacy, before);
        }
    }
}
