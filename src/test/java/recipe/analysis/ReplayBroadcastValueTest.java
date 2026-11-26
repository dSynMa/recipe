package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayBroadcastValueTest {
    private recipe.lang.System model(boolean similarName) throws Exception {
        String text=source("observation-forall-constant")
            .replace("local: sent: boolean", "local: sent: boolean, ch: channel")
            .replace("init: !sent", "init: !sent & ch == *")
            .replace("c!", "ch!").replace("c?", "*?");
        if(similarName) text=text.replace("channels: c", "channels: c, broadcast1");
        return parse(text);
    }
    private void check(boolean similarName) throws Exception {
        var model=model(similarName);
        assertCtl(ToNuXmv.transform(model),"EX receiver-received",true);
        Interpreter interpreter=new Interpreter(model);
        interpreter.init("TRUE");
        assertFalse("A selected broadcast receiver must be reachable",interpreter.isDeadlocked());
        assertEquals("*",interpreter.getCurrentStep().toJSON().getJSONObject("state").getJSONObject("sender").getString("ch"));
        interpreter.next(0);
        assertEquals("TRUE",interpreter.getCurrentStep().toJSON().getJSONObject("state").getJSONObject("receiver").getString("received"));
    }
    @Test(timeout=20000)
    public void symbolicBroadcastChannelSurvivesInitialSelection() throws Exception {check(false);}
    @Test(timeout=20000)
    public void similarlyNamedChannelDoesNotAffectBroadcastSelection() throws Exception {check(true);}
    @Test(timeout=30000)
    public void actualBroadcastCounterexamplesReplayWithSimilarNames() throws Exception {
        for (boolean similarName : new boolean[]{false, true}) {
            var model = model(similarName);
            try (NuXmvInteraction checker = new NuXmvInteraction(model)) {
                var raw = checker.modelCheckBmc("G !receiver-received", true, 3);
                assertTrue(raw.getRight(), raw.getLeft());
                assertTrue(raw.getRight(), raw.getRight().contains("is false"));
                var replay = Interpreter.ofTrace(model, raw.getRight());
                assertEquals(2, replay.traceToJSON().size());
                assertEquals("*", replay.getCurrentStep().toJSON().getJSONObject("state")
                    .getJSONObject("sender").getString("ch"));
            }
        }
    }

    @Test(timeout=20000)
    public void similarlyNamedChannelRemainsAnOrdinaryChannel() throws Exception {
        var model = parse(source("observation-forall-constant")
            .replace("channels: c", "channels: c, broadcast1")
            .replace("local: sent: boolean", "local: sent: boolean, ch: channel")
            .replace("init: !sent", "init: !sent & ch == broadcast1")
            .replace("c!", "ch!").replace("c?", "broadcast1?")
            .replace("channel == c", "channel == broadcast1"));
        assertCtl(ToNuXmv.transform(model), "EX receiver-received", true);
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        assertEquals("broadcast1", interpreter.getCurrentStep().toJSON().getJSONObject("state")
            .getJSONObject("sender").getString("ch"));
        interpreter.next(0);
        assertEquals("TRUE", interpreter.getCurrentStep().toJSON().getJSONObject("state")
            .getJSONObject("receiver").getString("received"));
    }
    @Test(timeout=20000)
    public void traceAndInteractiveDecodersDistinguishBroadcastFromSimilarNames() throws Exception {
        var first = model(true);
        var traceModel = first;
        java.util.function.Function<String, org.json.JSONObject> parser =
                output -> NuXmvInteraction.outputToJSON(traceModel, output);
        NuXmvInteraction interactive = new NuXmvInteraction(first) {
            @Override public Thread startNuXmvThread() { return new Thread(); }
        };
        model(false); // A later parse must not change the broadcast decoding.
        try {
            for (var decode : java.util.List.<java.util.function.Function<String, org.json.JSONObject>>of(
                    parser, interactive::outputToJSON)) {
                assertEquals("*", decode.apply("  sender-ch = broadcast\n")
                    .getJSONObject("sender").getString("ch"));
                assertEquals("broadcast1", decode.apply("  sender-ch = broadcast1\n")
                    .getJSONObject("sender").getString("ch"));
            }
        } finally {
            java.nio.file.Files.deleteIfExists(interactive.path);
        }
    }
}
