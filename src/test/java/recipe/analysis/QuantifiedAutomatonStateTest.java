package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class QuantifiedAutomatonStateTest {
    @Test
    public void stateReferencesExpandForBothQuantifiers() throws Exception {
        for (String quantifier : new String[] {"/\\", "\\/"}) {
            String output = ToNuXmv.transform(parse(source("supplier-locality")
                + "\nSPEC " + quantifier + " x: Receiver . (x-automaton-state == 0);\n"));
            assertValid(output);
            String proposition = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
            assertCtl(output, proposition, true);
        }
    }

    @Test
    public void stateReferencesTrackTransitions() throws Exception {
        String model = source("supplier-locality").replace(
            "repeat: receive: <value> GET@TRUE [received := value]",
            "repeat: receive: <value> GET@TRUE [received := value]; <TRUE> c? []");
        String output = ToNuXmv.transform(parse(model
            + "\nSPEC \\/ x: Receiver . (x-automaton-state == 1);\n"));
        String proposition = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
        assertCtl(output, proposition, false);
        assertCtl(output, "EX (" + proposition + ")", true);
    }
}
