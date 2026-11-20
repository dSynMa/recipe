package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.arithmetic.Floor;
import recipe.lang.types.Real;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class IntegerUpdateFeasibilityTest {
    @Test
    public void fractionalConstantUpdatesAreDisabledForIntegerTargets() throws Exception {
        for (String type : new String[] {"0..1", "integer"}) {
            String model = source("arithmetic-multiplication")
                .replace("count: integer", "count: " + type).replace("count == 2", "count == 0")
                .replace("count * 2", "0.5");
            assertSmtInvariant(ToNuXmv.transform(parse(model)), "!progress");
        }
    }

    @Test
    public void symbolicRealUpdatesRequireIntegralValues() throws Exception {
        for (String value : new String[] {"0.5", "1.0"}) {
            String model = source("arithmetic-multiplication")
                .replace("count: integer", "count: 0..1, proposed: real")
                .replace("count == 2", "count == 0 & proposed == " + value)
                .replace("count * 2", "proposed")
                .replace("ready <- !received", "ready <- TRUE");
            String output = ToNuXmv.transform(parse(model));
            assertSmtInvariant(output, value.equals("0.5") ? "!progress" : "progress");
        }
    }

    @Test
    public void floorEvaluationAndSimplificationAgreeForNegativeValues() throws Exception {
        for (String[] example : new String[][] {{"0.5", "0"}, {"1.0", "1"}, {"-0.5", "-1"}, {"-1.5", "-2"}}) {
            Floor floor = new Floor(new TypedValue(Real.getType(), example[0]));
            assertEquals(new TypedValue(Real.getType(), example[1]), floor.valueIn(null));
            assertEquals(floor.valueIn(null), floor.simplify());
        }
    }
}
