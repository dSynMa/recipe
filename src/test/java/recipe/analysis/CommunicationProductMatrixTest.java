package recipe.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CommunicationProductMatrixTest {
    @Test(timeout = 60000)
    public void allSmallReceiverProductsAgreeWithNuXmv() throws Exception {
        for (String channel : new String[]{"c", "*"}) for (int receivers = 1; receivers <= 3; receivers++) {
            for (int choices = 1; choices <= 3; choices++) {
                List<String> actions = new ArrayList<>();
                for (int i = 1; i <= choices; i++) actions.add("r" + i + ": <TRUE> " + channel + "? [tag := " + i + "]");
                List<String> instances = new ArrayList<>(List.of("Sender(sender, TRUE)"));
                for (int i = 0; i < receivers; i++) instances.add("Receiver(receiver" + i + ", TRUE)");
                String text = "channels: c\nmessage-structure: value: boolean\ncommunication-variables: ready: boolean\n"
                    + "agent Sender\nlocal: sent: boolean\ninit: !sent\nrelabel: ready <- TRUE\nreceive-guard: FALSE\n"
                    + "repeat: send: <TRUE> " + channel + "! (TRUE)(value := TRUE)[sent := TRUE]\n"
                    + "agent Receiver\nlocal: tag: 0..3\ninit: tag == 0\nrelabel: ready <- TRUE\nreceive-guard: channel == c\n"
                    + "repeat: " + String.join(" + ", actions) + "\nsystem = " + String.join(" | ", instances);
                var model = parse(text);
                String smv = ToNuXmv.transform(model);
                Interpreter interpreter = new Interpreter(model);
                interpreter.init("TRUE");
                var root = interpreter.getCurrentStep();
                int expected = (int) Math.pow(choices, receivers);
                assertEquals(text, expected, root.toJSON().getJSONArray("transitions").length());
                Set<List<Integer>> tuples = new HashSet<>();
                for (int i = 0; i < expected; i++) {
                    var state = root.next(i, interpreter).toJSON().getJSONObject("state");
                    List<Integer> tuple = new ArrayList<>();
                    List<String> clauses = new ArrayList<>(List.of("sender-sent"));
                    for (int j = 0; j < receivers; j++) {
                        int value = state.getJSONObject("receiver" + j).getInt("tag");
                        assertTrue(text, value >= 1 && value <= choices);
                        tuple.add(value);
                        clauses.add("receiver" + j + "-tag = " + value);
                    }
                    assertTrue("Duplicate successor: " + tuple, tuples.add(tuple));
                    assertCtl(smv, "EX (" + String.join(" & ", clauses) + ")", true);
                }
                List<String> ranges = new ArrayList<>(List.of("sender-sent"));
                for (int j = 0; j < receivers; j++) ranges.add("receiver" + j + "-tag >= 1 & receiver" + j + "-tag <= " + choices);
                assertCtl(smv, "AX (" + String.join(" & ", ranges) + ")", true);
            }
        }
    }
}
