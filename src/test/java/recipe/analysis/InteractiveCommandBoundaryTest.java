package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveCommandBoundaryTest {
    @Test(timeout = 10000)
    public void completingACommandDoesNotQueueAnExtraBlankCommand() throws Exception {
        NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")));
        try {
            for (int i = 0; i < 5; i++) {
                String marker = "response-" + i;
                String output = checker.execute(text -> text.trim().endsWith("nuXmv >"), "echo " + marker);
                assertTrue("Received a stray prompt or another command's response: " + output,
                        output.contains(marker));
            }
        } finally {
            checker.stopNuXmvThread();
        }
    }
}
