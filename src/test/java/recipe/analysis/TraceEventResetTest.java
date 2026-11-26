package recipe.analysis;

import java.nio.file.Files;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.CompositeJSON;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TraceEventResetTest {
    @Test
    public void explicitFalseEventsOverrideEarlierTrueEvents() throws Exception {
        String model = source("observation-forall-constant");
        var traceModel = parse(model);
        java.util.function.Function<String, org.json.JSONObject> parser =
                output -> NuXmvInteraction.outputToJSON(traceModel, output);
        NuXmvInteraction interactive = new NuXmvInteraction(parse(model)) {
            @Override public Thread startNuXmvThread() { return new Thread(); }
        };
        try {
            for (java.util.function.Function<String, JSONObject> decode :
                    java.util.List.<java.util.function.Function<String, JSONObject>>of(parser, interactive::outputToJSON)) {
                JSONObject before = decode.apply("  sender-send = TRUE\n  receiver-receive = TRUE\n");
                JSONObject after = decode.apply("  sender-send = FALSE\n  receiver-receive = FALSE\n");
                JSONObject reconstructed = new CompositeJSON(before, after);
                assertEquals("FALSE", reconstructed.getJSONObject("sender").getString("send"));
                assertEquals("FALSE", reconstructed.getJSONObject("receiver").getString("receive"));
            }
        } finally {
            Files.deleteIfExists(interactive.path);
        }
    }
}
