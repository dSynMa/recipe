package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayLongDeltaTest {
    @Test(timeout=60000)
    public void longFiniteDeltaTraceKeepsUnchangedValues() throws Exception {
        int steps=2000;
        String text="channels: c\nmessage-structure: value: boolean\ncommunication-variables: ready: boolean\n"
            +"agent Sender\nlocal: count: 0.."+steps+", stable: boolean\ninit: count == 0 & stable\n"
            +"relabel: ready <- TRUE\nreceive-guard: FALSE\nrepeat: send: <count < "+steps+"> *! (TRUE)(value := TRUE)[count := count + 1]\n"
            +"system = Sender(sender, TRUE)";
        var model=parse(text);
        assertCtl(ToNuXmv.transform(model),"EF sender-count = " + steps,true);
        JSONArray states=new JSONArray().put(new JSONObject("{sender:{count:0,stable:true}}"));
        for(int step=1;step<=steps;step++) states.put(new JSONObject().put("sender",new JSONObject().put("count",step)));
        Interpreter interpreter=Interpreter.ofJSON(model,states);
        assertTrue(interpreter.isDeadlocked());
        assertEquals(steps+1,interpreter.traceToJSON().size());
        assertEquals("TRUE",interpreter.getCurrentStep().toJSON().getJSONObject("state").getJSONObject("sender").getString("stable"));
    }
}
