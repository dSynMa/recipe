package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class CommunicationParticipationTest {
    private String channelModel() throws Exception {
        return source("observation-forall-constant").replace("SPEC G (<forall(TRUE)>true);", "");
    }

    private void assertProgress(String text, boolean expected) throws Exception {
        var model = parse(text);
        assertCtl(ToNuXmv.transform(model), "EX sender-sent", expected);
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        assertEquals(text, expected, !interpreter.isDeadlocked());
    }

    @Test(timeout = 20000)
    public void multicastWithoutListenersIsALocalStep() throws Exception {
        assertProgress(channelModel().replace("receive-guard: TRUE", "receive-guard: FALSE")
            .replace("receive-guard: channel == c", "receive-guard: FALSE"), true);
    }

    @Test(timeout = 20000)
    public void unselectedMulticastListenerBlocksSending() throws Exception {
        assertProgress(channelModel().replace("ready <- !received", "ready <- FALSE"), false);
    }

    @Test(timeout = 20000)
    public void broadcastReceiversParticipateRegardlessOfTheirListeningGuard() throws Exception {
        String text = channelModel().replace("c!", "*!").replace("c?", "*?")
            .replace("receive-guard: channel == c", "receive-guard: FALSE");
        var model = parse(text);
        assertCtl(ToNuXmv.transform(model), "EX receiver-received", true);
        Interpreter interpreter = new Interpreter(model);
        interpreter.init("TRUE");
        assertFalse(interpreter.isDeadlocked());
        var next = interpreter.getCurrentStep().next(0, interpreter).toJSON().getJSONObject("state");
        assertEquals("TRUE", next.getJSONObject("receiver").getString("received"));
    }

}
