package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class GenericAgentQuantifierTest {
    @Test
    public void genericAgentWorksAloneAndInEitherUnionPosition() throws Exception {
        for (String type : new String[] {"Agent", "Agent | Receiver", "Sender | Agent", "Sender | Receiver"}) {
            String output = ToNuXmv.transform(parse(source("supplier-locality")
                + "\nSPEC /\\ x: " + type + " . (x-automaton-state == 0);\n"));
            assertValid(output);
            assertTrue(output, output.substring(output.indexOf("LTLSPEC ")).contains("sender-automaton-state"));
            assertTrue(output, output.substring(output.indexOf("LTLSPEC ")).contains("first-automaton-state"));
            assertTrue(output, output.substring(output.indexOf("LTLSPEC ")).contains("second-automaton-state"));
        }
    }

    @Test
    public void genericAgentDoesNotRetainInstancesFromPreviousParses() throws Exception {
        parse(source("supplier-locality").replace("Sender(sender, TRUE)", "Sender(oldSender, TRUE)"));
        String output = ToNuXmv.transform(parse(source("supplier-locality")
            + "\nSPEC /\\ x: Agent . (x-automaton-state == 0);\n"));
        assertValid(output);
        assertFalse(output, output.contains("oldSender"));
        assertEquals(3, recipe.Config.getAgentType().getAllValues().size());
    }
}
