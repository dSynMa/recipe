package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class FalseDeadlockAnnotationTest {
    private JSONObject initial() {
        return new JSONObject("{sender:{sent:false,send:true},receiver:{received:false,receive:false},___LTOL___:{obs0:false}}");
    }
    private JSONObject afterSend() {
        return new JSONObject("{sender:{sent:true,send:false},receiver:{received:true,receive:true},___LTOL___:{obs0:true}}");
    }
    @Test(timeout=20000)
    public void liveStateCannotClaimToBeStuck() throws Exception {
        var model=parse(source("observation-forall-constant"));
        assertCtl(ToNuXmv.transform(model),"progress",true);
        assertThrows(Exception.class,()->Interpreter.ofJSON(model,new JSONArray().put(initial().put("___STUCK___",true)).put(afterSend())));
    }
    @Test(timeout=20000)
    public void liveSuccessorCannotClaimToBeStuck() throws Exception {
        var model = parse(source("observation-forall-constant")
            .replace("sent := TRUE", "sent := sent")
            .replace("received := value", "received := received"));
        assertCtl(ToNuXmv.transform(model), "AG progress", true);
        JSONObject next = initial().put("___STUCK___", true);
        next.getJSONObject("sender").put("send", true);
        next.getJSONObject("receiver").put("receive", true);
        next.getJSONObject("___LTOL___").put("obs0", true);
        Exception error = assertThrows(Exception.class, () -> Interpreter.ofJSON(model,
            new JSONArray().put(initial()).put(next)));
        assertTrue(error.getMessage(), error.getMessage().contains("deadlock annotation at step 1"));
    }
}
