package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

/** Regression for existential quantifiers nested under another binder. */
public class ExistentialRenamingTest {
    private void assertSpec(String spec, boolean expected) throws Exception {
        String output = ToNuXmv.transform(parse(source("supplier-locality") + "\nSPEC " + spec + ";\n"));
        assertValid(output);
        String proposition = output.substring(output.indexOf("LTLSPEC ") + "LTLSPEC ".length()).trim();
        assertCtl(output, proposition, expected);
    }

    @Test
    public void nestedExistentialRemainsExistential() throws Exception {
        assertSpec("\\/ y: Receiver . (y-name == first)", true);
        assertSpec("/\\ x: Receiver . (\\/ y: Receiver . (y-name == first))", true);
    }

}
