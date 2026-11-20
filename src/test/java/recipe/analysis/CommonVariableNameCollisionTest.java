package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CommonVariableNameCollisionTest {
    private void assertRejected(String text) throws Exception {
        RuntimeException error = assertThrows(RuntimeException.class, () -> parse(text));
        assertTrue(error.toString(), error.toString().contains("disjoint"));
    }

    @Test(timeout = 20000)
    public void senderLocalCommonNameCollisionIsRejected() throws Exception {
        String text = source("observation-forall-constant").replace("sent", "ready");
        assertRejected(text.replace("(@ready)", "(ready)"));
        assertRejected(text.replace("(@ready)", "(!ready & @ready)"));
    }

    @Test(timeout = 20000)
    public void supplierLocalCommonNameCollisionIsRejected() throws Exception {
        String text = source("send-observation-on-get").replace("sent", "ready")
            .replace("SUPPLY@(@ready)", "SUPPLY@(!ready & @ready)");
        assertRejected(text);
    }

    @Test(timeout = 20000)
    public void getterLocalCommonNameCollisionIsRejected() throws Exception {
        String text = source("send-observation-on-get").replace("received", "ready")
            .replace("GET@TRUE", "GET@(!ready & @ready)");
        assertRejected(text);
    }
}
