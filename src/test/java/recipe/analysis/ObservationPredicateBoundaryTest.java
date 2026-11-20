package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class ObservationPredicateBoundaryTest {
    @Test
    public void commonVariableNamesMayStartWithQuantifierNames() throws Exception {
        for (String name : new String[] {"forallReady", "existsReady"}) {
            for (String expression : new String[] {name, "!" + name, "forall(" + name + ")", "exists(" + name + ")"}) {
                String model = source("observation-forall-constant").replace("ready", name)
                    .replace("forall(TRUE)", expression);
                String output = ToNuXmv.transform(parse(model));
                assertValid(output);
                assertCtl(output, "EX obs0", !expression.startsWith("!"));
            }
        }
    }
}
