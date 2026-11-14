package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationRelabelCategoryTest {
    @Test public void booleanCommonVariableRejectsNumericAndEnumMappings() throws Exception {
        for (String value : new String[]{"2", "c"}) {
            String text = source("observation-forall-constant").replace("ready <- !received", "ready <- " + value);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse(text));
            assertTrue(error.getMessage(), error.getMessage().contains("Invalid relabelling"));
        }
    }

    private String enumModel(String mapping) throws Exception {
        return source("observation-forall-constant").replace("channels: c", "channels: c, falseValue")
            .replace("ready: boolean", "ready: channel").replace("ready <- TRUE", "ready <- c")
            .replace("ready <- !received", "ready <- " + mapping).replace("(@ready)", "(@ready == falseValue)");
    }

    @Test public void enumCommonVariableRejectsBooleanAndNumericMappings() throws Exception {
        for (String value : new String[]{"TRUE", "2"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse(enumModel(value)));
            assertTrue(error.getMessage(), error.getMessage().contains("Invalid relabelling"));
        }
    }

    @Test public void numericCommonVariableRejectsBooleanMapping() throws Exception {
        String text = source("observation-forall-constant").replace("ready: boolean", "ready: 0..2");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse(text));
        assertTrue(error.getMessage(), error.getMessage().contains("Invalid relabelling"));
    }

    @Test(timeout=30000) public void enumRelabellingUsesItsDeclaredDomain() throws Exception {
        assertCtl(ToNuXmv.transform(parse(enumModel("falseValue"))), "EX receiver-received", true);
        assertCtl(ToNuXmv.transform(parse(enumModel("c"))), "EX receiver-received", false);
    }
}
