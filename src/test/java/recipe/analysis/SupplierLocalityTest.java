package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class SupplierLocalityTest {
    @Test
    public void namedSupplySelectsOnlyItsTarget() throws Exception {
        String output = ToNuXmv.transform(parse(source("supplier-locality")));
        assertCtl(output, "EX first-received", true);
        assertCtl(output, "EX second-received", false);
    }

    @Test
    public void selfSupplyRequiresGetterToNameSupplier() throws Exception {
        String model = source("supplier-locality").replace("SUPPLY@first", "SUPPLY@SELF");
        assertCtl(ToNuXmv.transform(parse(model)), "EX first-received", false);
        assertCtl(ToNuXmv.transform(parse(model.replace("GET@TRUE ", "GET@sender"))),
            "EX first-received", true);
    }

    @Test
    public void getterMustAlsoSelectSupplier() throws Exception {
        String model = source("supplier-locality").replace("GET@TRUE ", "GET@second");
        assertCtl(ToNuXmv.transform(parse(model)), "EX first-received", false);
    }

    @Test
    public void predicateSupplyStillEvaluatesItsGuard() throws Exception {
        String model = source("supplier-locality");
        assertCtl(ToNuXmv.transform(parse(model.replace("SUPPLY@first", "SUPPLY@TRUE"))),
            "EX second-received", true);
        assertCtl(ToNuXmv.transform(parse(model.replace("SUPPLY@first", "SUPPLY@FALSE"))),
            "EX first-received", false);
    }
}
