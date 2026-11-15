package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SmtArithmeticBackendTest {
    private String model(String update) throws Exception {
        return source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..3")
            .replace("count == 2", "count == 1")
            .replace("count * 2", update);
    }

    @Test(timeout = 20000)
    public void finiteVariablesDoNotForceRealPropertyArithmeticThroughAiger() throws Exception {
        var system = parse(model("count") + "\nSPEC (sender-count / 2) * 2 == sender-count;\n");
        String output = ToNuXmv.transform(system);
        String property = output.substring(output.indexOf("LTLSPEC ") + 8).trim();
        NuXmvInteraction checker = new NuXmvInteraction(system);
        try {
            for (boolean bounded : new boolean[] {false, true}) {
                var result = checker.modelCheckIc3(property, bounded, 4);
                assertTrue(result.getRight(), result.getLeft());
                assertTrue(result.getRight(), result.getRight().contains("is true"));
            }
        } finally {
            checker.close();
        }
    }

    @Test(timeout = 20000)
    public void realUpdateFeasibilityDoesNotRequireABooleanModel() throws Exception {
        NuXmvInteraction checker = new NuXmvInteraction(parse(model("count / 2")));
        try {
            var bmc = checker.modelCheckBmc("G !progress", true, 3);
            assertTrue(bmc.getRight(), bmc.getLeft());
            assertTrue(bmc.getRight(), bmc.getRight().contains("no counterexample found with bound 3"));
            var ic3 = checker.modelCheckIc3("G !progress", false, 4);
            assertTrue(ic3.getRight(), ic3.getLeft());
            assertTrue(ic3.getRight(), ic3.getRight().contains("is true"));
        } finally {
            checker.close();
        }
    }
}
