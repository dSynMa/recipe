package recipe.analysis;

import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class BooleanBackendSelectionTest {
    @Test(timeout = 30000) public void finiteAndRealArithmeticModelsBothSupportIc3() throws Exception {
        String finite = source("arithmetic-multiplication").replace("count: integer", "count: 0..3")
                .replace("count == 2", "count == 1").replace("count * 2", "count");
        for (String model : new String[] {finite, finite.replace("count := count", "count := count / 2")}) {
            NuXmvBatch checker = new NuXmvBatch(parse(model));
            try {
                var result = checker.modelCheckIc3("TRUE", false, -1);
                assertTrue(result.getRight(), result.getLeft());
                assertTrue(result.getRight(), result.getRight().contains("is true"));
            } finally { Files.deleteIfExists(checker.path); }
        }
    }
}
