package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationNumericObservationTest {
    private String model(String descriptor) throws Exception {
        return source("observation-forall-constant")
            .replace("ready: boolean","rank: 0..2")
            .replace("ready <- TRUE","rank <- 1")
            .replace("ready <- !received","rank <- 1")
            .replace("(@ready)","(@rank == 1)")
            .replace("forall(TRUE)",descriptor);
    }
    @Test(timeout=20000)
    public void bareFiniteNumericCommonComparisonIsSpecialised() throws Exception {
        assertCtl(ToNuXmv.transform(parse(model("rank == 1"))),"EX obs0",true);
    }
    @Test(timeout=20000)
    public void negatedFiniteNumericCommonComparisonIsSpecialised() throws Exception {
        assertCtl(ToNuXmv.transform(parse(model("!(rank == 1)"))),"EX obs0",false);
    }
    @Test(timeout=20000)
    public void explicitlyQuantifiedFiniteNumericComparisonControl() throws Exception {
        assertCtl(ToNuXmv.transform(parse(model("forall(rank == 1)"))),"EX obs0",true);
    }
}
