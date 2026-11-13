package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class GuardObservationNormalizationTest {
    private String model() throws Exception {
        return source("observation-forall-constant")
            .replace("agent Sender", "guard identity(a: boolean) := a;\nguard negate(a: boolean) := !a;\nagent Sender")
            .replace("(@ready)", "(TRUE)");
    }

    @Test
    public void outerQuantifierControlsNestedPredicatesInsideGuardArguments() throws Exception {
        assertObservation("forall(identity(exists(ready)))", false, false);
        assertObservation("exists(identity(forall(ready)))", true, false);
        assertObservation("forall(identity(exists(ready)))", false, true);
        assertObservation("exists(identity(forall(ready)))", true, true);
    }

    @Test
    public void guardWrappingTheWholeObservationDoesNotHideItsQuantifiers() throws Exception {
        assertObservation("identity(forall(exists(ready)))", false, false);
        assertObservation("identity(exists(forall(ready)))", true, false);
    }

    @Test
    public void nestedGuardExpansionPreservesNegationAndIndependentQuantifiers() throws Exception {
        assertObservation("exists(identity(negate(forall(ready))))", true, false);
        assertObservation("forall(identity(negate(exists(ready))))", false, false);
        assertObservation("identity(exists(ready)) & negate(forall(ready))", true, false);
    }

    @Test
    public void normalizationPreservesEqualityAndInequalityOnSelectedReceivers() throws Exception {
        for (String[] example : new String[][] {
            {"forall(identity(exists(ready)))", "true"},
            {"identity(forall(exists(ready))) == TRUE", "true"},
            {"identity(forall(exists(ready))) != TRUE", "false"},
            {"forall(identity(exists(ready)) == TRUE)", "true"}
        }) {
            String source = model().replace("c! (TRUE)", "c! (@ready)")
                .replace("forall(TRUE)", example[0]);
            String output = ToNuXmv.transform(parse(source));
            assertValid(output);
            assertCtl(output, "EX obs0", java.lang.Boolean.parseBoolean(example[1]));
        }
    }

    private void assertObservation(String observation, boolean expected, boolean box) throws Exception {
        String formula = box ? "[" + observation + "]true" : "<" + observation + ">true";
        String output = ToNuXmv.transform(parse(model().replace("SPEC G (<forall(TRUE)>true);", "SPEC G (" + formula + ");")));
        assertValid(output);
        assertCtl(output, "EX obs0", expected);
    }
}
