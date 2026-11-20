package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class BooleanObservationInequalityTest {
    @Test
    public void inequalitySpecializesItsQuantifiedOperands() throws Exception {
        String[] observations = {"exists(ready) != FALSE", "exists(ready) != TRUE",
            "forall(TRUE) != exists(ready)", "!(exists(ready) != TRUE)"};
        boolean[] expected = {true, false, false, true};
        for (int i = 0; i < observations.length; i++) {
            String model = source("observation-forall-constant")
                .replace("forall(TRUE)", "(" + observations[i] + ")");
            String output = ToNuXmv.transform(parse(model));
            assertValid(output);
            assertCtl(output, "EX obs0", expected[i]);
        }
    }
}
