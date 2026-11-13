package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class NestedObservationScopeTest {
    @Test
    public void observationCanReferToAnOuterAgent() throws Exception {
        assertObservation("/\\ x: Sender . (\\/ y: Receiver . (<sender == x>true))", true);
    }

    @Test
    public void observationCanReferToItsInnerAgent() throws Exception {
        assertObservation("/\\ x: Sender . (\\/ y: Receiver . (<sender == y>true))", false);
    }

    @Test
    public void innerBindingShadowsAnOuterBindingWithAnotherType() throws Exception {
        assertObservation("/\\ x: Sender . (\\/ x: Receiver . (<sender == x>true))", false);
        assertObservation("/\\ x: Receiver . (\\/ x: Sender . (<sender == x>true))", true);
    }

    @Test
    public void observationRetainsBindingsAcrossMultipleNestedQuantifiers() throws Exception {
        assertObservation("/\\ x: Sender . (\\/ y: Receiver . (/\\ z: Agent . (<sender == x>true)))", true);
    }

    private void assertObservation(String spec, boolean expected) throws Exception {
        String output = ToNuXmv.transform(parse(source("observation-forall-constant")
            .replace("SPEC G (<forall(TRUE)>true);", "SPEC " + spec + ";")));
        assertValid(output);
        assertCtl(output, "EX obs0", expected);
    }
}
