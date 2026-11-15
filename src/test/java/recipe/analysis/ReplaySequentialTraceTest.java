package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplaySequentialTraceTest {
    private String source(boolean mixed) {
        String send1=mixed ? "SUPPLY@receiver" : "c! (TRUE)";
        String receive1=mixed ? "GET@sender" : "c?";
        return "channels: c,d,broadcast1\nmessage-structure: value: 0..3\ncommunication-variables: ready: boolean\n"
            +"agent Sender\nlocal: count: 0..3, ch: channel\ninit: count == 0 & ch == c\n"
            +"relabel: ready <- TRUE\nreceive-guard: FALSE\nrepeat: first: <count < 3> "+send1+"(value := 1)[count := 1, ch := d]; "
            +"second: <TRUE> ch! (TRUE)(value := 2)[count := 2, ch := *]; third: <TRUE> ch! (TRUE)(value := 3)[count := 3]\n"
            +"agent Receiver\nlocal: total: 0..3\ninit: total == 0\nrelabel: ready <- TRUE\nreceive-guard: channel == c | channel == d\n"
            +"repeat: first: <TRUE> "+receive1+"[total := value]; second: <TRUE> d?[total := value]; third: <TRUE> *?[total := value]\n"
            +"system = Sender(sender, TRUE) | Receiver(receiver, TRUE)\nSPEC G (<channel == *>true);";
    }
    private void check(boolean mixed) throws Exception {
        var model=parse(source(mixed));
        NuXmvInteraction checker=new NuXmvInteraction(model);
        try {
            var trace=checker.modelCheckBmc("G (sender-count < 3)",true,4);
            assertTrue(trace.getRight(),trace.getLeft());
            assertTrue(trace.getRight(),trace.getRight().contains("is false"));
            var replay=Interpreter.ofTrace(model,trace.getRight());
            assertEquals(4,replay.traceToJSON().size());
            assertTrue(replay.isDeadlocked());
            var state=replay.getCurrentStep().toJSON().getJSONObject("state");
            assertEquals("3",state.getJSONObject("sender").getString("count"));
            assertEquals("*",state.getJSONObject("sender").getString("ch"));
            assertEquals("3",state.getJSONObject("receiver").getString("total"));
            assertTrue(replay.getCurrentStep().toJSON().getJSONObject("___LTOL___").getBoolean("obs0"));
        } finally { checker.close(); }
    }
    @Test(timeout=30000)
    public void changingChannelAndSequentialStatesReplayFromRealBmcTrace() throws Exception {check(false);}
    @Test(timeout=30000)
    public void mixedSupplyChannelAndBroadcastSequenceReplays() throws Exception {check(true);}
}
