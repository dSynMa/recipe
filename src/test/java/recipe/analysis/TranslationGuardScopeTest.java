package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationGuardScopeTest {
    private String model() throws Exception {
        return source("observation-forall-constant")
            .replace("agent Sender", "guard targetReady() := @ready;\nagent Sender");
    }

    @Test public void entryGuardsCannotHideReceiverVariables() throws Exception {
        for (boolean supply : new boolean[]{false, true}) {
            String text = model().replace("send: <TRUE>", "send: <targetReady()>");
            if (supply) text = text.replace("c! (@ready)", "SUPPLY@(@ready)").replace("c?", "GET@TRUE");
            final String input = text;
            assertThrows(IllegalArgumentException.class, () -> parse(input));
        }
    }

    @Test public void receiveAndGetEntryGuardsCannotHideReceiverVariables() throws Exception {
        for (String text : new String[]{model().replace("receive: <value>", "receive: <targetReady()>"),
                model().replace("receive: <value> c?", "receive: <targetReady()> GET@TRUE")}) {
            assertThrows(IllegalArgumentException.class, () -> parse(text));
        }
    }

    @Test(timeout=30000) public void selectionGuardsCanUseReceiverVariables() throws Exception {
        String text = model().replace("c! (@ready)", "c! (targetReady())");
        assertCtl(ToNuXmv.transform(parse(text)), "EX receiver-received", true);
    }
}
