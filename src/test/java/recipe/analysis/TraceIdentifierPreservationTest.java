package recipe.analysis;

import java.nio.file.Files;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TraceIdentifierPreservationTest {
    private void assertBoth(String model, String trace, java.util.function.Consumer<JSONObject> assertion) throws Exception {
        NuXmvBatch batch = new NuXmvBatch(parse(model));
        NuXmvInteraction interactive = null;
        try {
            interactive = new NuXmvInteraction(parse(model)) {
                @Override public Thread startNuXmvThread() { return new Thread(); }
            };
            assertion.accept(batch.outputToJSON(trace));
            assertion.accept(interactive.outputToJSON(trace));
        } finally {
            Files.deleteIfExists(batch.path);
            if (interactive != null) Files.deleteIfExists(interactive.path);
        }
    }

    @Test
    public void agentNamesAreNotMistakenForGeneratedHelpers() throws Exception {
        for (String name : new String[] {"keeper", "falsifyBot", "obs1bot", "keep"}) {
            String model = source("supplier-locality").replace("Sender(sender,", "Sender(" + name + ",");
            assertBoth(model, "  " + name + "-sent = TRUE\n  " + name + "-name = " + name + "\n  keep-all = FALSE\n", json -> {
                assertTrue(json.toString(), json.has(name));
                assertEquals("TRUE", json.getJSONObject(name).getString("sent"));
                assertEquals(name, json.getJSONObject(name).getString("name"));
                assertFalse(json.toString(), json.getJSONObject(name).has("all"));
            });
        }
    }

    @Test
    public void observationNamesAreRewrittenOnlyForActualObservationAssignments() throws Exception {
        String model = source("supplier-locality")
            .replace("sent: boolean", "sent: boolean, obs0: boolean")
            .replace("init: !sent", "init: !sent & !obs0");
        assertBoth(model, "  sender-obs0 = TRUE\n  obs0 = FALSE\n  no-observations = TRUE\n", json -> {
            assertEquals("TRUE", json.getJSONObject("sender").getString("obs0"));
            assertEquals("FALSE", json.getJSONObject("___LTOL___").getString("obs0"));
            assertEquals("TRUE", json.getJSONObject("___LTOL___").getString("no-observations"));
        });
    }

    @Test
    public void enumValuesContainingObservationPrefixesAreUnchanged() throws Exception {
        String model = source("supplier-locality").replace("channels: c", "channels: c,obs9channel")
            .replace("sent: boolean", "sent: boolean, ch: channel")
            .replace("init: !sent", "init: !sent & ch == obs9channel");
        assertBoth(model, "  sender-ch = obs9channel\n", json ->
            assertEquals("obs9channel", json.getJSONObject("sender").getString("ch")));
    }
}
