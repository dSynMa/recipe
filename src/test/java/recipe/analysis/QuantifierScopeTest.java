package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

/** Scope regression tests for nested agent quantifiers. */
public class QuantifierScopeTest {
    @Test
    public void freeOuterReferencesStillExpand() throws Exception {
        assertSpec("\\/ x: Receiver . (/\\ y: Receiver . (x-name == first))", true);
        assertSpec("/\\ x: Receiver . (\\/ y: Receiver . (x-name == first))", false);
    }

    @Test
    public void similarlyNamedBindersDoNotShadowEachOther() throws Exception {
        assertSpec("\\/ x: Receiver . (/\\ xx: Receiver . (x-name == first))", true);
        assertSpec("\\/ x: Receiver . (/\\ xx: Receiver . (xx-name == first))", false);
    }

    @Test
    public void existentialShadowingIsPreserved() throws Exception {
        assertSpec("/\\ x: Receiver . (\\/ x: Receiver . (x-name == first))", true);
    }
    private void assertSpec(String spec, boolean expected) throws Exception {
        String output = ToNuXmv.transform(parse(source("supplier-locality") + "\nSPEC " + spec + ";\n"));
        assertValid(output);
        String proposition = output.substring(output.indexOf("LTLSPEC ") + "LTLSPEC ".length()).trim();
        assertCtl(output, proposition, expected);
    }

    @Test
    public void innerBinderShadowsOuterVariable() throws Exception {
        assertSpec("\\/ x: Receiver . (/\\ y: Receiver . (y-name == first))", false);
        assertSpec("\\/ x: Receiver . (/\\ x: Receiver . (x-name == first))", false);
    }
    @Test
    public void twoAgentVariablesAreFullyBound() throws Exception {
        String model = source("supplier-locality");
        assertValid(ToNuXmv.transform(parse(model + "\nSPEC /\\ x: Receiver . (x-name == x-name);\n")));
        assertValid(ToNuXmv.transform(parse(model + "\nSPEC /\\ x: Receiver, y: Receiver . (x-name == y-name);\n")));
    }

}
