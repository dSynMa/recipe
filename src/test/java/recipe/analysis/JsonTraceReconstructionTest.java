package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class JsonTraceReconstructionTest {
    private JSONObject initial() {
        return new JSONObject("{sender:{sent:'FALSE'},first:{received:'FALSE'},second:{received:'FALSE'}}");
    }

    @Test(timeout = 20000)
    public void omittedSenderStateCannotChangeDuringDeltaReplay() throws Exception {
        JSONArray states = new JSONArray().put(initial())
            .put(new JSONObject("{first:{received:'TRUE'}}"));
        Exception error = assertThrows(Exception.class, () -> Interpreter.ofJSON(parse(source("supplier-locality")), states));
        assertTrue(error.getMessage(), error.getMessage().contains("step 1"));
    }

    @Test(timeout = 20000)
    public void validDeltaRetainsUnchangedAgentState() throws Exception {
        JSONArray states = new JSONArray().put(initial())
            .put(new JSONObject("{sender:{sent:'TRUE'},first:{received:'TRUE'}}"));
        var replay = Interpreter.ofJSON(parse(source("supplier-locality")), states).traceToJSON();
        assertEquals(2, replay.size());
        assertEquals("FALSE", replay.get(1).getJSONObject("state").getJSONObject("second").getString("received"));
    }

    @Test(timeout = 20000)
    public void completeSnapshotsRemainSupported() throws Exception {
        JSONObject next = initial();
        next.getJSONObject("sender").put("sent", "TRUE");
        next.getJSONObject("first").put("received", "TRUE");
        var replay = Interpreter.ofJSON(parse(source("supplier-locality")), new JSONArray().put(initial()).put(next));
        assertEquals(2, replay.traceToJSON().size());
    }
}
