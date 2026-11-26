package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class JsonBooleanReplayTest {
    private JSONObject initial(boolean nativeValues) {
        JSONObject state = new JSONObject("{sender:{sent:'FALSE'},first:{received:'FALSE'},second:{received:'FALSE'}}");
        if (nativeValues) for (String agent : state.keySet()) {
            JSONObject values = state.getJSONObject(agent);
            for (String key : values.keySet()) values.put(key, false);
        }
        return state;
    }

    private void assertNativeSuccessor(boolean nativeInitial) throws Exception {
        JSONObject next = new JSONObject().put("sender", new JSONObject().put("sent", true))
            .put("first", new JSONObject().put("received", true));
        var replay = Interpreter.ofJSON(parse(source("supplier-locality")), new JSONArray().put(initial(nativeInitial)).put(next));
        assertEquals(2, replay.traceToJSON().size());
    }

    @Test(timeout = 20000)
    public void nativeBooleanSuccessorsMatchActualBooleanValues() throws Exception {
        assertNativeSuccessor(false);
    }

    @Test(timeout = 20000)
    public void nativeBooleanInitialStatesAreRenderedAsSmvBooleans() throws Exception {
        assertNativeSuccessor(true);
    }

    @Test(timeout = 20000)
    public void invalidBooleanTextCannotMatchFalse() throws Exception {
        var model = parse(source("supplier-locality").replace("sent := TRUE", "sent := sent")
            .replace("received := value", "received := received"));
        JSONObject next = new JSONObject().put("sender", new JSONObject().put("sent", "garbage"));
        assertThrows(Exception.class, () -> Interpreter.ofJSON(model, new JSONArray().put(initial(false)).put(next)));
    }
    @Test(timeout = 20000)
    public void nativeBooleanObservationFlagsMatchObservedMessages() throws Exception {
        for (boolean negated : new boolean[] {false, true}) {
            var model = parse(source("observation-forall-constant")
                .replace("SPEC G (<forall(TRUE)>true);", "SPEC G (<" + (negated ? "!" : "") + "value>true);"));
            JSONObject initial = new JSONObject("{sender:{sent:'FALSE'},receiver:{received:'FALSE'}}");
            JSONObject next = new JSONObject("{sender:{sent:'TRUE'},receiver:{received:'TRUE'}}")
                .put("___LTOL___", new JSONObject().put("obs0", !negated));
            assertEquals(2, Interpreter.ofJSON(model, new JSONArray().put(initial).put(next)).traceToJSON().size());
        }
    }
}
