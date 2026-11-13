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
}
