package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class QuantifierParserIsolationTest {
    @Test
    public void siblingQuantifiersCannotUseEachOthersBindings() throws Exception {
        assertRejected("SPEC (/\\ x: Sender . (x-sent == FALSE)) & (/\\ y: Receiver . (x-sent == FALSE));");
    }

    @Test
    public void separateSpecificationsCannotUseEachOthersBindings() throws Exception {
        assertRejected("SPEC /\\ x: Sender . (x-sent == FALSE);\nSPEC /\\ y: Receiver . (x-sent == FALSE);");
    }

    @Test
    public void independentlyBoundSiblingsRemainValid() throws Exception {
        String output = ToNuXmv.transform(parse(source("supplier-locality")
            + "\nSPEC (/\\ x: Sender . (!x-sent)) & (/\\ y: Receiver . (!y-received));\n"));
        assertValid(output);
        String proposition = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
        assertCtl(output, proposition, true);
    }

    private void assertRejected(String spec) throws Exception {
        boolean rejected = false;
        try {
            parse(source("supplier-locality") + "\n" + spec + "\n");
        } catch (RuntimeException | AssertionError expected) {
            rejected = true;
        }
        assertTrue("Unbound x must be rejected: " + spec, rejected);
    }
}
