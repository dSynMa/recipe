package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationAssignmentCategoryTest {
    private String model(boolean supply, String domain, String payload) throws Exception {
        String text = source("observation-forall-constant").replace("value: boolean", "value: " + domain)
            .replace("value := TRUE", "value := " + payload)
            .replace("receive: <value>", "receive: <TRUE>").replace("received := value", "received := TRUE")
            .replace("SPEC G (<forall(TRUE)>true);", "");
        return supply ? text.replace("c! (@ready)", "SUPPLY@(@ready)").replace("c?", "GET@TRUE") : text;
    }

    private void reject(String text) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse(text));
        assertTrue(error.getMessage(), error.getMessage().contains("Invalid assignment"));
    }

    @Test public void booleanMessagesRejectNumericAndEnumValues() throws Exception {
        for (boolean supply : new boolean[]{false, true}) for (String value : new String[]{"2", "c"})
            reject(model(supply, "boolean", value));
    }

    @Test public void enumMessagesRejectBooleanAndNumericValues() throws Exception {
        for (boolean supply : new boolean[]{false, true}) for (String value : new String[]{"TRUE", "2"})
            reject(model(supply, "channel", value));
    }

    @Test public void localUpdatesRejectCategoricalMismatches() throws Exception {
        for (boolean supply : new boolean[]{false, true}) {
            reject(model(supply, "boolean", "TRUE").replace("sent := TRUE", "sent := 2"));
            reject(model(supply, "boolean", "TRUE").replace("received := TRUE", "received := c"));
        }
    }

    @Test(timeout=30000) public void validCategoricalMessagesRemainEnabled() throws Exception {
        for (boolean supply : new boolean[]{false, true}) {
            assertCtl(ToNuXmv.transform(parse(model(supply, "boolean", "TRUE"))), "EX receiver-received", true);
            assertCtl(ToNuXmv.transform(parse(model(supply, "channel", "c"))), "EX receiver-received", true);
        }
    }
}
