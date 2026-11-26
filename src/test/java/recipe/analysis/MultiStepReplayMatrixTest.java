package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class MultiStepReplayMatrixTest {
    @Test(timeout = 60000)
    public void completeAndDeltaSnapshotsAgreeAcrossCommunicationModes() throws Exception {
        for (String mode : new String[]{"c", "*", "supply"}) {
            boolean supply = mode.equals("supply");
            String send = supply ? "SUPPLY@(TRUE)" : mode + "! (TRUE)";
            String receive = supply ? "GET@TRUE" : mode + "?";
            String text = "channels: c\nmessage-structure: value: 0..3\ncommunication-variables: ready: boolean\n"
                + "agent Sender\nlocal: count: 0..3, unchanged: boolean\ninit: count == 0 & unchanged\n"
                + "relabel: ready <- TRUE\nreceive-guard: FALSE\nrepeat: send: <count < 3> "
                + send + "(value := count + 1)[count := count + 1]\n"
                + "agent Receiver\nlocal: total: 0..3, unchanged: boolean\ninit: total == 0 & !unchanged\n"
                + "relabel: ready <- TRUE\nreceive-guard: channel == c\nrepeat: receive: <TRUE> "
                + receive + " [total := value]\nsystem = Sender(sender, TRUE) | Receiver(receiver, TRUE)";
            var model = parse(text);
            assertCtl(ToNuXmv.transform(model), "EF (sender-count = 3 & receiver-total = 3)", true);
            for (boolean delta : new boolean[]{false, true}) {
                JSONArray states = new JSONArray();
                for (int step = 0; step <= 3; step++) {
                    JSONObject sender = new JSONObject().put("count", step);
                    JSONObject receiver = new JSONObject().put("total", Integer.toString(step));
                    if (!delta || step == 0) {
                        sender.put("unchanged", true);
                        receiver.put("unchanged", false);
                    }
                    states.put(new JSONObject().put("sender", sender).put("receiver", receiver));
                }
                Interpreter replay = Interpreter.ofJSON(model, states);
                assertEquals(4, replay.traceToJSON().size());
                assertTrue(replay.isDeadlocked());
                JSONObject state = replay.getCurrentStep().toJSON().getJSONObject("state");
                assertEquals("3", state.getJSONObject("receiver").getString("total"));
                assertEquals("TRUE", state.getJSONObject("sender").getString("unchanged"));
                assertEquals("FALSE", state.getJSONObject("receiver").getString("unchanged"));
                states.getJSONObject(2).getJSONObject("receiver").put("total", 0);
                assertThrows(Exception.class, () -> Interpreter.ofJSON(model, states));
            }
        }
    }
}
