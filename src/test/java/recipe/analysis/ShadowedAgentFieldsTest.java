package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ShadowedAgentFieldsTest {
    @Test
    public void shadowedFieldsAndLabelsAreRejected() throws Exception {
        for (String field : new String[] {"sent", "send"}) {
            boolean rejected = false;
            try {
                parse(source("supplier-locality") + "\nSPEC /\\ x: Sender . (\\/ x: Receiver . (x-" + field + " == FALSE));\n");
            } catch (RuntimeException | AssertionError expected) {
                rejected = true;
            }
            assertTrue("Receiver has no " + field, rejected);
        }
    }

    @Test
    public void innerFieldsAndUnshadowedOuterFieldsRemainAvailable() throws Exception {
        for (String spec : new String[] {
            "/\\ x: Sender . (\\/ x: Receiver . (!x-received))",
            "/\\ x: Sender . (\\/ y: Receiver . (!x-sent & !y-received))"
        }) {
            String output = ToNuXmv.transform(parse(source("supplier-locality") + "\nSPEC " + spec + ";\n"));
            assertValid(output);
            assertCtl(output, output.substring(output.indexOf("LTLSPEC ") + 8).trim(), true);
        }
    }
}
