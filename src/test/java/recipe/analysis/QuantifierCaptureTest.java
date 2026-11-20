package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class QuantifierCaptureTest {
    @Test
    public void instanceNamesDoNotCaptureOuterReferencesInExistentials() throws Exception {
        for (String inner : new String[] {"y", "first", "second"}) {
            assertSpec("/\\ x: Receiver . (\\/ " + inner + ": Receiver . (x-name != " + inner + "-name))", true);
        }
    }

    @Test
    public void instanceNamesDoNotCaptureOuterReferencesInUniversals() throws Exception {
        for (String inner : new String[] {"y", "first", "second"}) {
            assertSpec("\\/ x: Receiver . (/\\ " + inner + ": Receiver . (x-name == " + inner + "-name))", false);
        }
    }

    @Test
    public void simultaneousBindersAreSubstitutedWithoutCapture() throws Exception {
        assertSpec("/\\ x: Receiver, first: Receiver . (x-name == first-name)", false);
        assertSpec("\\/ x: Receiver, first: Receiver . (x-name != first-name)", true);
    }

    @Test
    public void observationAgentReferencesAreAlsoCaptureFree() throws Exception {
        String model = source("observation-forall-constant")
            .replace("SPEC G (<forall(TRUE)>true);",
                "SPEC /\\ x: Receiver . (\\/ receiver: Sender . (<sender == x>true));");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertCtl(output, "EX obs0", false);
    }

    private void assertSpec(String spec, boolean expected) throws Exception {
        String output = ToNuXmv.transform(parse(source("supplier-locality") + "\nSPEC " + spec + ";\n"));
        assertValid(output);
        assertCtl(output, output.substring(output.indexOf("LTLSPEC ") + 8).trim(), expected);
    }
}
