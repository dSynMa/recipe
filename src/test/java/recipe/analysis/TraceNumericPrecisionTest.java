package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class TraceNumericPrecisionTest {
    private Interpreter replay(String nextValue) throws Exception {
        var model = parse(source("arithmetic-multiplication")
            .replace("count: integer", "count: real")
            .replace("count == 2", "count == 9007199254740992.0")
            .replace("count * 2", "count"));
        JSONObject initial = new JSONObject("{sender:{sent:'FALSE',count:'9007199254740992.0'},receiver:{received:'FALSE'}}");
        JSONObject next = new JSONObject("{sender:{sent:'TRUE'},receiver:{received:'TRUE'}}");
        next.getJSONObject("sender").put("count", nextValue);
        return Interpreter.ofJSON(model, new JSONArray().put(initial).put(next));
    }

    @Test(timeout = 20000)
    public void distinctLargeRealValuesCannotMatchTheSameState() throws Exception {
        Exception error = assertThrows(Exception.class, () -> replay("9007199254740993.0"));
        assertTrue(error.getMessage(), error.getMessage().contains("step 1"));
    }

    @Test(timeout = 20000)
    public void equalNumericValuesWithDifferentScalesStillMatch() throws Exception {
        assertEquals(2, replay("9007199254740992.000").traceToJSON().size());
    }
}
