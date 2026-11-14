package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ObservationReplayDifferentialTest {
    private void check(String selection, String observation, boolean expected) throws Exception {
        String text = source("observation-forall-constant")
            .replace("(@ready)", "(" + selection + ")")
            .replace("c!", "*!").replace("c?", "*?")
            .replace("forall(TRUE)", observation);
        var model = parse(text);
        assertCtl(ToNuXmv.transform(model), "EX obs0", expected);
        JSONObject initial = new JSONObject("{sender:{sent:'FALSE'},receiver:{received:'FALSE'}}");
        JSONObject next = new JSONObject("{sender:{sent:'TRUE'}}");
        next.put("receiver", new JSONObject().put("received", selection.equals("TRUE") || selection.equals("@ready")));
        next.put("___LTOL___", new JSONObject().put("obs0", expected));
        assertEquals(selection + ": " + observation, 2,
            Interpreter.ofJSON(model, new JSONArray().put(initial).put(next)).traceToJSON().size());
        next.getJSONObject("___LTOL___").put("obs0", !expected);
        assertThrows(selection + ": " + observation, Exception.class,
            () -> Interpreter.ofJSON(model, new JSONArray().put(initial).put(next)));
    }

    @Test(timeout = 60000)
    public void quantifierReplayMatchesNuXmvAndThePaperSemantics() throws Exception {
        for (String selection : new String[]{"@ready", "!@ready", "TRUE", "FALSE"}) {
            check(selection, "ready", selection.equals("@ready") || selection.equals("FALSE"));
            check(selection, "!ready", selection.equals("!@ready") || selection.equals("TRUE"));
            check(selection, "forall(ready)", selection.equals("@ready") || selection.equals("FALSE"));
            check(selection, "exists(ready)", selection.equals("@ready") || selection.equals("TRUE"));
            check(selection, "forall(TRUE)", true);
            check(selection, "exists(FALSE)", false);
        }
    }
}
