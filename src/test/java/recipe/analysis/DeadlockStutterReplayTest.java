package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class DeadlockStutterReplayTest {
    private JSONObject initial() {
        return new JSONObject("{sender:{sent:'FALSE'},first:{received:'FALSE'},second:{received:'FALSE'}}");
    }

    private Interpreter replay(JSONObject next) throws Exception {
        var model = parse(source("supplier-locality").replace("<TRUE> SUPPLY", "<FALSE> SUPPLY"));
        return Interpreter.ofJSON(model, new JSONArray().put(initial()).put(next));
    }

    @Test(timeout = 20000)
    public void repeatedCompleteDeadlockedStateIsAccepted() throws Exception {
        var result = replay(initial());
        assertTrue(result.isDeadlocked());
        assertEquals(1, result.traceToJSON().size());
    }

    @Test(timeout = 20000)
    public void unchangedDeltaAtDeadlockIsAccepted() throws Exception {
        assertTrue(replay(new JSONObject()).isDeadlocked());
    }

    @Test(timeout = 20000)
    public void changedStateAtDeadlockIsRejected() throws Exception {
        JSONObject next = initial();
        next.getJSONObject("sender").put("sent", "TRUE");
        Exception error = assertThrows(Exception.class, () -> replay(next));
        assertTrue(error.getMessage(), error.getMessage().contains("step 1"));
    }
    @Test(timeout = 20000)
    public void changedStateAfterAnUnchangedDeadlockSuffixIsRejected() throws Exception {
        var model = parse(source("supplier-locality").replace("<TRUE> SUPPLY", "<FALSE> SUPPLY"));
        JSONObject changed = initial();
        changed.getJSONObject("sender").put("sent", "TRUE");
        Exception error = assertThrows(Exception.class, () -> Interpreter.ofJSON(model,
            new JSONArray().put(initial()).put(new JSONObject()).put(changed)));
        assertTrue(error.getMessage(), error.getMessage().contains("step 2"));
    }

    @Test(timeout = 20000)
    public void multipleUnchangedDeadlockSnapshotsRemainCompressed() throws Exception {
        var model = parse(source("supplier-locality").replace("<TRUE> SUPPLY", "<FALSE> SUPPLY"));
        var replay = Interpreter.ofJSON(model, new JSONArray().put(initial())
            .put(new JSONObject()).put(initial()).put(new JSONObject()));
        assertTrue(replay.isDeadlocked());
        assertEquals(1, replay.traceToJSON().size());
    }
}
