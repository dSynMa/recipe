package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class ImplicitObservationQuantifierTest {
    @Test
    public void bareVariablesAndNegationsMatchExplicitQuantifiers() throws Exception {
        for (String selection : new String[] {"@ready", "!@ready", "TRUE", "FALSE"}) {
            for (String observation : new String[] {"ready", "!ready"}) {
                boolean expected = observation.equals("ready")
                    ? selection.equals("@ready") || selection.equals("FALSE")
                    : selection.equals("!@ready") || selection.equals("TRUE");
                String explicit = observation.equals("ready") ? "forall(ready)" : "exists(!ready)";
                for (String expression : new String[] {observation, explicit}) {
                    String model = source("observation-forall-constant")
                        .replace("(@ready)", "(" + selection + ")")
                        .replace("c!", "*!").replace("c?", "*?")
                        .replace("forall(TRUE)", expression);
                    String output = ToNuXmv.transform(parse(model));
                    assertValid(output);
                    assertCtl(output, "EX obs0", expected);
                }
            }
        }
    }

    @Test
    public void explicitQuantifierScopeIsNotChangedByImplicitLiterals() throws Exception {
        String model = source("observation-forall-constant").replace("(@ready)", "(TRUE)");
        for (String[] example : new String[][] {{"exists(ready)", "true"}, {"forall(ready)", "false"},
                {"ready | !ready", "true"}, {"ready & !ready", "false"}, {"!!ready", "false"}}) {
            String output = ToNuXmv.transform(parse(model.replace("forall(TRUE)", example[0])));
            assertCtl(output, "EX obs0", java.lang.Boolean.parseBoolean(example[1]));
        }
    }
}
