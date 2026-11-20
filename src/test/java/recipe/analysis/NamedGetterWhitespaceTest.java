package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class NamedGetterWhitespaceTest {
    @Test
    public void namedGetterIgnoresWhitespaceBeforeUpdate() throws Exception {
        String model = source("supplier-locality");
        assertCtl(ToNuXmv.transform(parse(model.replace("GET@TRUE ", "GET@sender"))),
            "EX first-received", true);
        assertCtl(ToNuXmv.transform(parse(model.replace("GET@TRUE", "GET@sender"))),
            "EX first-received", true);
    }
}
