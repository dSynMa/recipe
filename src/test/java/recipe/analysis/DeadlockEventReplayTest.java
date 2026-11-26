package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class DeadlockEventReplayTest {
    private JSONObject initial() {
        return new JSONObject("{sender:{sent:false},receiver:{received:false,receive:false},___LTOL___:{obs0:false}}");
    }
    private JSONObject afterSend() {
        return new JSONObject("{sender:{sent:true,send:false},receiver:{received:true,receive:true},___LTOL___:{obs0:true}}");
    }
    @Test(timeout=20000)
    public void impossibleTrueEventAtInitialDeadlockIsRejected() throws Exception {
        var model=parse(source("observation-forall-constant").replace("send: <TRUE>","send: <FALSE>"));
        assertCtl(ToNuXmv.transform(model),"AG !sender-send",true);
        var next=new JSONObject("{sender:{send:true}}");
        assertThrows(Exception.class,()->Interpreter.ofJSON(model,new JSONArray().put(initial()).put(next)));
    }
    @Test(timeout=20000)
    public void impossibleTrueObservationAtInitialDeadlockIsRejected() throws Exception {
        var model=parse(source("observation-forall-constant").replace("send: <TRUE>","send: <FALSE>"));
        assertCtl(ToNuXmv.transform(model),"AG !obs0",true);
        var next=new JSONObject("{___LTOL___:{obs0:true}}");
        assertThrows(Exception.class,()->Interpreter.ofJSON(model,new JSONArray().put(initial()).put(next)));
    }
    @Test(timeout=20000)
    public void deadlockCannotRepeatPreviousEvent() throws Exception {
        var model=parse(source("observation-forall-constant"));
        assertCtl(ToNuXmv.transform(model),"AG (!progress -> AX !sender-send)",true);
        assertThrows(Exception.class,()->Interpreter.ofJSON(model,new JSONArray().put(initial()).put(afterSend()).put(afterSend())));
    }
    @Test(timeout=20000)
    public void deadlockClearedFlagsRemainAccepted() throws Exception {
        var model=parse(source("observation-forall-constant"));
        var reset = new JSONObject("{sender:{send:false},receiver:{receive:false},___LTOL___:{obs0:false}}");
        assertEquals(2,Interpreter.ofJSON(model,new JSONArray().put(initial()).put(afterSend()).put(reset)).traceToJSON().size());
    }
}
