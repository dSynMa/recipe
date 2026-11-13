package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class ReleaseTranslationTest {
    @Test
    public void releaseAgreesWithNativeNuXmvForEveryBooleanPair() throws Exception {
        for (String left : new String[] {"false", "true"}) {
            for (String right : new String[] {"false", "true"}) {
                String output = ToNuXmv.transform(parse(source("supplier-locality")
                    + "\nSPEC " + left + " R " + right + ";\n"));
                String actual = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
                boolean expected = right.equals("true");
                assertLtl(output, left.toUpperCase() + " V " + right.toUpperCase(), expected);
                assertLtl(output, actual, expected);
            }
        }
    }

    @Test
    public void releaseAgreesWithNativeNuXmvWhenStateChanges() throws Exception {
        String output = ToNuXmv.transform(parse(source("supplier-locality")
            + "\nSPEC sender-sent R !first-received;\n"));
        String actual = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
        assertLtl(output, "(" + actual + ") <-> (sender-sent V !first-received)", true);
    }
}
