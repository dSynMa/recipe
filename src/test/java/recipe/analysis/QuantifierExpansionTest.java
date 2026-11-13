package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class QuantifierExpansionTest {
    @Test
    public void multipleBindersExpandIndependently() throws Exception {
        recipe.lang.System first = parse(source("supplier-locality")
            + "\nSPEC \\/ x: Receiver, y: Receiver . ((x-name == first) & (y-name == second));\n");
        String output = ToNuXmv.transform(first);
        assertValid(output);
        String proposition = output.substring(output.indexOf("LTLSPEC ") + "LTLSPEC ".length()).trim();
        assertCtl(output, proposition, true);
    }

}
