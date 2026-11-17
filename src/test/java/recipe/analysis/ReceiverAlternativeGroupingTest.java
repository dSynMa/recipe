package recipe.analysis;

import org.junit.Test;

import static recipe.analysis.TranslationTestModels.*;

public class ReceiverAlternativeGroupingTest {
    @Test
    public void everyReceiveAlternativeRequiresItsSourceState() throws Exception {
        String model = "channels: c\n"
            + "message-structure: value: boolean\n"
            + "communication-variables: ready: boolean\n"
            + "agent Sender\n"
            + " local: sent: boolean\n init: !sent\n relabel: ready <- TRUE\n"
            + " receive-guard: TRUE\n"
            + " repeat: send: <TRUE> c! (@ready)(value := TRUE)[sent := TRUE]\n"
            + "agent Receiver\n"
            + " local: received: boolean\n init: !received\n relabel: ready <- TRUE\n"
            + " receive-guard: channel == c\n"
            + " repeat: (enter: <FALSE> c? [received := FALSE];"
            + " (first: <value> c? [received := TRUE] + second: <value> c? [received := TRUE]))\n"
            + "system = Sender(sender, TRUE) | Receiver(receiver, TRUE)\n";
        String output = ToNuXmv.transform(parse(model));
        // Both enabled receives live behind the disabled entry action. Neither may run initially.
        assertCtl(output, "!progress", true);
        assertCtl(output, "AG !receiver-received", true);
        assertCtl(output, "EX receiver-received", false);

        // Positive control: once the entry action is enabled, those receives must remain reachable.
        String reachable = ToNuXmv.transform(parse(model.replace("enter: <FALSE>", "enter: <TRUE>")));
        assertCtl(reachable, "EF receiver-received", true);
    }
}
