package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class SendLabelInitialReplayTest {
    @Test(timeout = 20000)
    public void sendLabelSelectsOmittedInitialLocals() throws Exception {
        String text = source("observation-forall-constant")
            .replace("init: !sent", "init: TRUE")
            .replace("send: <TRUE>", "send: <!sent>");
        for (Object enabled : new Object[] {true, false, "TRUE", "false"}) {
            JSONObject state = new JSONObject().put("sender", new JSONObject().put("send", enabled));
            var replay = Interpreter.ofJSON(parse(text), new JSONArray().put(state));
            String sent = replay.getCurrentStep().toJSON().getJSONObject("state")
                .getJSONObject("sender").getString("sent");
            assertEquals(java.lang.Boolean.parseBoolean(enabled.toString()) ? "FALSE" : "TRUE", sent);
        }
    }

    @Test(timeout = 20000)
    public void sendLabelSelectsOmittedReceiverState() throws Exception {
        String text = source("observation-forall-constant").replace("init: !received", "init: TRUE");
        JSONObject state = new JSONObject("{sender:{send:false}}");
        var replay = Interpreter.ofJSON(parse(text), new JSONArray().put(state));
        assertEquals("TRUE", replay.getCurrentStep().toJSON().getJSONObject("state")
            .getJSONObject("receiver").getString("received"));
    }

    @Test(timeout = 20000)
    public void invalidSendLabelValueIsRejected() throws Exception {
        JSONObject state = new JSONObject("{sender:{sent:false},receiver:{received:false}}");
        state.getJSONObject("sender").put("send", "FALSE | TRUE");
        Exception error = assertThrows(Exception.class, () -> Interpreter.ofJSON(
            parse(source("observation-forall-constant")), new JSONArray().put(state)));
        assertTrue(error.getMessage(), error.getMessage().contains("invalid send label value"));
    }

}
