package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TraceActionSelectionTest {
    private void check(boolean supply) throws Exception {
        for (String chosen : new String[]{"one", "two"}) {
            String text = source("observation-forall-constant").replace("SPEC G (<forall(TRUE)>true);", "");
            if (supply) text = text.replace("c! (@ready)", "SUPPLY@(@ready)");
            String action = supply ? "GET@TRUE" : "c?";
            text = text.replace("repeat: receive: <value> c? [received := value]",
                "repeat: one: <value> " + action + " [received := value] + two: <value> " + action + " [received := value]");
            var model = parse(text);
            assertCtl(ToNuXmv.transform(model), "EX receiver-" + chosen, true);
            JSONObject initial = new JSONObject("{sender:{sent:'FALSE'},receiver:{received:'FALSE',one:'FALSE',two:'FALSE'}}");
            JSONObject next = new JSONObject("{sender:{sent:'TRUE'},receiver:{received:'TRUE'}}");
            next.getJSONObject("receiver").put("one", chosen.equals("one")).put("two", chosen.equals("two"));
            var replay = Interpreter.ofJSON(model, new JSONArray().put(initial).put(next));
            assertEquals(chosen + (supply ? "" : "?"), replay.getCurrentStep().toJSON().getJSONObject("state")
                .getJSONObject("receiver").getString("**last_label**"));
            next.getJSONObject("receiver").put("one", true).put("two", true);
            assertThrows(Exception.class, () -> Interpreter.ofJSON(model, new JSONArray().put(initial).put(next)));
        }
    }

    @Test(timeout = 20000)
    public void receiveEventFlagsSelectTheMatchingAction() throws Exception { check(false); }

    @Test(timeout = 20000)
    public void getEventFlagsSelectTheMatchingAction() throws Exception { check(true); }
    @Test(timeout = 20000)
    public void deadlockStutterCanClearThePreviousActionFlags() throws Exception {
        var model = parse(source("observation-forall-constant").replace("SPEC G (<forall(TRUE)>true);", ""));
        JSONObject initial = new JSONObject("{sender:{sent:'FALSE',send:'TRUE'},receiver:{received:'FALSE',receive:'FALSE'}}");
        JSONObject next = new JSONObject("{sender:{sent:'TRUE',send:'FALSE'},receiver:{received:'TRUE',receive:'TRUE'}}");
        JSONObject stutter = new JSONObject("{sender:{send:'FALSE'},receiver:{receive:'FALSE'}}");
        var replay = Interpreter.ofJSON(model, new JSONArray().put(initial).put(next).put(stutter));
        assertTrue(replay.isDeadlocked());
        assertEquals(2, replay.traceToJSON().size());
    }
}
