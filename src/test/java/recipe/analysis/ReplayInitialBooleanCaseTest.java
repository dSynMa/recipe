package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayInitialBooleanCaseTest {
    private void check(String value) throws Exception {
        var model=parse(source("observation-forall-constant"));
        assertCtl(ToNuXmv.transform(model),"EX receiver-received",true);
        JSONObject initial=new JSONObject().put("sender",new JSONObject().put("sent",value))
            .put("receiver",new JSONObject().put("received",value));
        JSONObject next=new JSONObject("{sender:{sent:true},receiver:{received:true}}");
        assertEquals(2,Interpreter.ofJSON(model,new JSONArray().put(initial).put(next)).traceToJSON().size());
    }
    @Test(timeout=20000)
    public void lowerCaseInitialBooleanStringsReplay() throws Exception { check("false"); }
    @Test(timeout=20000)
    public void mixedCaseInitialBooleanStringsReplay() throws Exception { check("FaLsE"); }
    @Test(timeout=20000)
    public void upperCaseInitialBooleanStringsReplay() throws Exception { check("FALSE"); }
    @Test(timeout=20000)
    public void mixedCaseTrueInitialBooleanStringsReplay() throws Exception {
        var model = parse(source("observation-forall-constant").replace("init: !sent", "init: sent"));
        JSONObject initial = new JSONObject("{sender:{sent:'TrUe'},receiver:{received:'false'}}");
        assertEquals(1, Interpreter.ofJSON(model, new JSONArray().put(initial)).traceToJSON().size());
    }

    @Test(timeout=20000)
    public void enumConstantsKeepTheirCase() throws Exception {
        var model = parse(source("observation-forall-constant")
            .replace("channels: c", "channels: c, idle")
            .replace("sent: boolean", "sent: boolean, ch: channel")
            .replace("init: !sent", "init: !sent & ch == idle"));
        JSONObject initial = new JSONObject("{sender:{sent:'false',ch:'idle'},receiver:{received:'FALSE'}}");
        var replay = Interpreter.ofJSON(model, new JSONArray().put(initial));
        assertEquals("idle", replay.getCurrentStep().toJSON().getJSONObject("state")
            .getJSONObject("sender").getString("ch"));
    }
}
