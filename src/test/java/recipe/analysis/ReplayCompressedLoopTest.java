package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayCompressedLoopTest {
    private JSONObject initial() {
        return new JSONObject("{sender:{sent:false},receiver:{received:false,receive:false},___LTOL___:{obs0:false}}");
    }
    @Test(timeout=20000)
    public void loopAtCompressedInitialDeadlockIsRetained() throws Exception {
        var model=parse(source("observation-forall-constant").replace("send: <TRUE>","send: <FALSE>"));
        assertCtl(ToNuXmv.transform(model),"!progress",true);
        var states=Interpreter.ofJSON(model,new JSONArray().put(initial())
            .put(new JSONObject().put("___LOOP___",true))).traceToJSON();
        assertEquals(1,states.size());
        assertTrue(states.toString(),states.get(0).optBoolean("___LOOP___",false));
    }
    @Test(timeout=20000)
    public void loopAtCompressedPostSendDeadlockIsRetained() throws Exception {
        var model=parse(source("observation-forall-constant"));
        JSONObject sent=new JSONObject("{sender:{sent:true,send:false},receiver:{received:true,receive:true},___LTOL___:{obs0:true}}");
        JSONObject stutter=new JSONObject("{sender:{send:false},receiver:{receive:false},___LTOL___:{obs0:false},___LOOP___:true}");
        var states=Interpreter.ofJSON(model,new JSONArray().put(initial()).put(sent).put(stutter).put(new JSONObject().put("___LOOP___", false))).traceToJSON();
        assertEquals(2,states.size());
        assertFalse(states.get(0).optBoolean("___LOOP___",false));
        assertTrue(states.toString(),states.get(1).optBoolean("___LOOP___",false));
        assertTrue(states.get(1).getJSONObject("___LTOL___").getBoolean("obs0"));
    }
    @Test(timeout=20000)
    public void initialLoopAnnotationRemainsValidControl() throws Exception {
        var model=parse(source("observation-forall-constant").replace("send: <TRUE>","send: <FALSE>"));
        var states=Interpreter.ofJSON(model,new JSONArray().put(initial().put("___LOOP___",true))
            .put(new JSONObject())).traceToJSON();
        assertTrue(states.get(0).optBoolean("___LOOP___",false));
    }
}
