package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class JsonAutomatonStateReplayTest {
    private JSONArray trace() throws Exception {
        var model = parse(source("supplier-locality"));
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        var root = interpreter.getCurrentStep();
        JSONArray states = new JSONArray().put(root.toJSON().getJSONObject("state"))
            .put(root.next(0, interpreter).toJSON().getJSONObject("state"));
        for (int i = 0; i < states.length(); i++) {
            JSONObject state = states.getJSONObject(i);
            for (String agent : state.keySet()) {
                JSONObject values = state.getJSONObject(agent);
                values.put("automaton-state", values.getString("**state**"));
            }
        }
        return states;
    }

    private void numericStates(JSONObject state) {
        for (String agent : state.keySet()) {
            JSONObject values = state.optJSONObject(agent);
            if (values != null && values.has("automaton-state"))
                values.put("automaton-state", Integer.valueOf(values.get("automaton-state").toString()));
        }
    }

    @Test(timeout = 20000)
    public void nativeNumericSuccessorAutomatonStatesReplay() throws Exception {
        JSONArray states = trace();
        numericStates(states.getJSONObject(1));
        assertEquals(2, Interpreter.ofJSON(parse(source("supplier-locality")), states).traceToJSON().size());
    }

    @Test(timeout = 20000)
    public void nativeNumericInitialAndSuccessorAutomatonStatesReplay() throws Exception {
        JSONArray states = trace();
        numericStates(states.getJSONObject(0));
        numericStates(states.getJSONObject(1));
        assertEquals(2, Interpreter.ofJSON(parse(source("supplier-locality")), states).traceToJSON().size());
    }

    @Test(timeout = 20000)
    public void incorrectAutomatonStateStillFailsReplay() throws Exception {
        JSONArray states = trace();
        states.getJSONObject(1).getJSONObject("sender").put("automaton-state", 999);
        Exception error = assertThrows(Exception.class, () -> Interpreter.ofJSON(parse(source("supplier-locality")), states));
        assertTrue(error.getMessage(), error.getMessage().contains("step 1"));
    }
}
