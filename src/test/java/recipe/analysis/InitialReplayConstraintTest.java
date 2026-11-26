package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InitialReplayConstraintTest {
    private JSONObject initial() {
        return new JSONObject("{sender:{sent:false},receiver:{received:false}}");
    }

    private void reject(JSONObject state) throws Exception {
        var model = parse(source("observation-forall-constant"));
        Exception error = assertThrows(Exception.class,
            () -> Interpreter.ofJSON(model, new JSONArray().put(state)));
        assertTrue(error.getMessage(), error.getMessage().contains("step 0")
            || error.getMessage().contains("Cannot select an initial state"));
    }

    @Test(timeout = 20000)
    public void impossibleInitialAutomatonStateIsRejected() throws Exception {
        JSONObject state = initial();
        state.getJSONObject("sender").put("automaton-state", 999);
        reject(state);
    }

    @Test(timeout = 20000)
    public void initialReceiveCannotAlreadyHaveOccurred() throws Exception {
        JSONObject state = initial();
        state.getJSONObject("receiver").put("receive", true);
        reject(state);
    }

    @Test(timeout = 20000)
    public void enabledInitialSendCannotBeReportedAsFalse() throws Exception {
        JSONObject state = initial();
        state.getJSONObject("sender").put("send", false);
        reject(state);
    }

    @Test(timeout = 20000)
    public void initialSendObservationCannotBeTrue() throws Exception {
        reject(initial().put("___LTOL___", new JSONObject().put("obs0", true)));
    }

    @Test(timeout = 20000)
    public void enabledInitialSendAndFalseObservationsRemainValid() throws Exception {
        JSONObject state = initial().put("___LTOL___", new JSONObject().put("obs0", false));
        state.getJSONObject("sender").put("send", true);
        assertEquals(1, Interpreter.ofJSON(parse(source("observation-forall-constant")),
            new JSONArray().put(state)).traceToJSON().size());
    }
}
