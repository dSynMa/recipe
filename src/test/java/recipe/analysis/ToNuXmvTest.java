package recipe.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static recipe.analysis.TranslationTestModels.parse;

public class ToNuXmvTest {
    @Test
    public void translatesBiggerExample() throws Exception {
        assertTranslates("bigger-example.txt");
    }

    @Test
    public void translatesExample() throws Exception {
        assertTranslates("example.rcp");
    }

    @Test
    public void translatesExample3() throws Exception {
        assertTranslates("example3.rcp");
    }

    private static void assertTranslates(String file) throws Exception {
        String output = ToNuXmv.transform(parse(Files.readString(Path.of(file))));
        assertTrue("Missing main module for " + file, output.startsWith("MODULE main\n"));
    }
}
