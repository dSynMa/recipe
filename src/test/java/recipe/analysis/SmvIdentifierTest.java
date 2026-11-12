package recipe.analysis;

import org.junit.Test;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.arithmetic.Multiplication;
import recipe.lang.expressions.arithmetic.Division;
import recipe.lang.types.Number;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SmvIdentifierTest {
    @Test
    public void trueSuffixInInstanceNameIsPreserved() throws Exception {
        String model = source("supplier-locality");
        assertValid(ToNuXmv.transform(parse(model)));
        String output = ToNuXmv.transform(parse(model.replace("Sender(sender, TRUE)", "Sender(senderTRUE, TRUE)")));
        assertValid(output);
        assertCtl(output, "EX (senderTRUE-sent & first-received)", true);
    }
}
