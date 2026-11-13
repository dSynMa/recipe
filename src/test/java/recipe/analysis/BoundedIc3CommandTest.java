package recipe.analysis;

import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class BoundedIc3CommandTest {
    @Test(timeout = 20000)
    public void boundedIc3ChecksPropertiesWithASupportedBoundOption() throws Exception {
        NuXmvBatch checker = new NuXmvBatch(parse(source("supplier-locality")));
        try {
            var falseResult = checker.modelCheckIc3("sender-sent", true, 4);
            assertTrue(falseResult.getRight(), falseResult.getLeft());
            assertTrue(falseResult.getRight(), falseResult.getRight().contains("is false"));
            var trueResult = checker.modelCheckIc3("G (sender-sent -> first-received)", true, 20);
            assertTrue(trueResult.getRight(), trueResult.getLeft());
            assertTrue(trueResult.getRight(), trueResult.getRight().contains("is true"));
        } finally {
            Files.deleteIfExists(checker.path);
        }
    }
}
