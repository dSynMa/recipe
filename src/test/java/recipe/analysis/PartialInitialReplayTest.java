package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class PartialInitialReplayTest {
    @Test(timeout=20000)
    public void omittedSupplierInPartialInitialStateStillReplays() throws Exception {
        var model=parse(source("supplier-locality"));
        assertCtl(ToNuXmv.transform(model),"EX first-received",true);
        JSONObject initial=new JSONObject("{first:{received:false},second:{received:false}}");
        JSONObject next=new JSONObject("{sender:{sent:true},first:{received:true},second:{received:false}}");
        assertEquals(2,Interpreter.ofJSON(model,new JSONArray().put(initial).put(next)).traceToJSON().size());
    }
    @Test(timeout=20000)
    public void emptyInitialStateCanReplaySupply() throws Exception {
        var model=parse(source("supplier-locality"));
        JSONObject next=new JSONObject("{sender:{sent:true},first:{received:true},second:{received:false}}");
        assertEquals(2,Interpreter.ofJSON(model,new JSONArray().put(new JSONObject()).put(next)).traceToJSON().size());
    }
    @Test(timeout=20000)
    public void omittedSenderWorksForChannelSend() throws Exception {
        var model=parse(source("observation-forall-constant"));
        JSONObject initial=new JSONObject("{receiver:{received:false}}");
        JSONObject next=new JSONObject("{sender:{sent:true},receiver:{received:true}}");
        assertEquals(2,Interpreter.ofJSON(model,new JSONArray().put(initial).put(next)).traceToJSON().size());
    }
    @Test(timeout=20000)
    public void emptyInitialStateCanReplayChannelSend() throws Exception {
        var model=parse(source("observation-forall-constant"));
        JSONObject next=new JSONObject("{sender:{sent:true},receiver:{received:true}}");
        assertEquals(2,Interpreter.ofJSON(model,new JSONArray().put(new JSONObject()).put(next)).traceToJSON().size());
    }
    @Test(timeout=20000)
    public void modelWithoutLocalsCanReplayItsInitialSnapshot() throws Exception {
        String text=source("observation-forall-constant")
            .replace("local: sent: boolean", "local:")
            .replace("local: received: boolean", "local:")
            .replace("init: !sent", "init: TRUE").replace("init: !received", "init: TRUE")
            .replace("ready <- !received", "ready <- TRUE")
            .replace("sent := TRUE", "").replace("received := value", "");
        var model=parse(text);
        assertCtl(ToNuXmv.transform(model),"progress",true);
        JSONObject initial=new JSONObject("{sender:{send:true,'automaton-state':0},receiver:{receive:false,'automaton-state':0},___LTOL___:{obs0:false}}");
        assertEquals(1,Interpreter.ofJSON(model,new JSONArray().put(initial)).traceToJSON().size());
    }
}
