package recipe.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TraceDeadlockMarkerTest {
    @Test(timeout = 20000)
    public void enabledUnchangedBroadcastIsNotMarkedDeadlocked() throws Exception {
        org.junit.Assume.assumeTrue(Files.isExecutable(Path.of("nuxmv/bin/nuxmv")));
        String model = source("observation-forall-constant")
            .replace("send: ", "").replace("receive: ", "")
            .replace("c! (@ready)", "*! (FALSE)").replace("sent := TRUE", "sent := sent");
        String output = ToNuXmv.transform(parse(model));
        assertCtl(output, "AG progress", true);
        NuXmvBatch batch = new NuXmvBatch(parse(model));
        try {
            String trace = batch.modelCheckBmc("G !obs0", true, 2).getRight();
            assertTrue(trace, trace.contains("is false"));
            assertTrue(trace, trace.contains("keep-all = TRUE"));
            assertFalse(batch.outputToJSON(trace).toString(), batch.outputToJSON(trace).has("___STUCK___"));
        } finally {
            Files.deleteIfExists(batch.path);
        }
    }

    @Test
    public void deadlockMarkerUsesProgressRatherThanStateEquality() throws Exception {
        NuXmvBatch batch = new NuXmvBatch(parse(source("supplier-locality")));
        NuXmvInteraction interactive = new NuXmvInteraction(parse(source("supplier-locality"))) {
            @Override public Thread startNuXmvThread() { return new Thread(); }
        };
        try {
            for (java.util.function.Function<String, JSONObject> decode :
                    java.util.List.<java.util.function.Function<String, JSONObject>>of(batch::outputToJSON, interactive::outputToJSON)) {
                assertFalse(decode.apply("  keep-all = TRUE\n  progress = TRUE\n").has("___STUCK___"));
                assertTrue(decode.apply("  progress = FALSE\n").getBoolean("___STUCK___"));
            }
        } finally {
            Files.deleteIfExists(batch.path);
            Files.deleteIfExists(interactive.path);
        }
    }
}
