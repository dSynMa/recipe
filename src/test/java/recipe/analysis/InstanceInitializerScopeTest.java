package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InstanceInitializerScopeTest {
    @Test
    public void initializersCannotReferToPreviousAgentTypesLocals() throws Exception {
        for (String composition : new String[] {
            "Sender(sender, TRUE) | Receiver(first, !sent)",
            "Receiver(first, TRUE) | Sender(sender, !received)"
        }) {
            String model = source("supplier-locality").replace(
                "Sender(sender, TRUE) | Receiver(first, TRUE) | Receiver(second, TRUE)", composition);
            boolean rejected = false;
            try { parse(model); } catch (RuntimeException | AssertionError expected) { rejected = true; }
            assertTrue("Out-of-scope initializer was accepted: " + composition, rejected);
        }
    }

    @Test
    public void validInitializersWorkAcrossAlternatingAgentTypes() throws Exception {
        String model = source("supplier-locality").replace(
            "Sender(sender, TRUE) | Receiver(first, TRUE) | Receiver(second, TRUE)",
            "Receiver(first, !received) | Sender(sender, !sent) | Receiver(second, !received)");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertCtl(output, "!sender-sent & !first-received & !second-received", true);
    }
}
