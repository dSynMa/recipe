package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayInitialOwnerScopeTest {
    private void check(boolean collision, String event) throws Exception {
        String text=source("observation-forall-constant");
        if(collision) text=text.replace("local: sent: boolean", "local: sent: boolean, receive: boolean")
            .replace("init: !sent", "init: !sent & !receive");
        var model=parse(text);
        assertCtl(ToNuXmv.transform(model),"EX receiver-received",true);
        JSONObject initial=new JSONObject("{sender:{sent:false},receiver:{received:false}}");
        initial.getJSONObject("receiver").put("receive",event);
        JSONObject next=new JSONObject("{sender:{sent:true},receiver:{received:true,receive:true}}");
        assertEquals(2,Interpreter.ofJSON(model,new JSONArray().put(initial).put(next)).traceToJSON().size());
    }
    @Test(timeout=20000)
    public void anotherAgentsLocalDoesNotAffectInitialEventParsing() throws Exception {check(true,"false");}
    @Test(timeout=20000)
    public void upperCaseEventWithOtherAgentLocalIsValidControl() throws Exception {check(true,"FALSE");}
    @Test(timeout=20000)
    public void lowerCaseEventWithoutOtherAgentLocalIsValidControl() throws Exception {check(false,"false");}

    @Test(timeout = 20000)
    public void distinctChannelStepsReplayConsistently() throws Exception {
        for (boolean senderChooses : new boolean[]{true, false}) {
            String text = source("observation-forall-constant")
                .replace("channels: c", "channels: c, d")
                .replace("receive-guard: channel == c", "receive-guard: channel == d");
            if (senderChooses) {
                text = text.replace("sent: boolean", "sent: boolean, chosen: channel")
                    .replace("init: !sent", "init: !sent & chosen == d")
                    .replace("c!", "chosen!").replace("c?", "d?");
            } else {
                text = text.replace("received: boolean", "received: boolean, chosen: channel")
                    .replace("init: !received", "init: !received & chosen == d")
                    .replace("c?", "chosen?").replace("c!", "d!");
            }
            JSONObject initial = new JSONObject("{sender:{sent:false},receiver:{received:false}}");
            initial.getJSONObject(senderChooses ? "sender" : "receiver").put("chosen", "d");
            JSONObject next = new JSONObject("{sender:{sent:true},receiver:{received:true}}");
            JSONArray trace = new JSONArray().put(initial).put(next);
            assertEquals(2, Interpreter.ofJSON(parse(text), trace).traceToJSON().size());
        }
    }
}
