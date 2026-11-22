package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class MessageSnapshotSemanticsTest {
    @Test
    public void sendPayloadAndObservationUseThePreUpdateState() throws Exception {
        String model = source("observation-forall-constant")
            .replace("sent: boolean", "sent: boolean, payload: boolean")
            .replace("init: !sent", "init: !sent & !payload")
            .replace("value := TRUE", "value := payload")
            .replace("sent := TRUE", "sent := TRUE, payload := TRUE")
            .replace("receive: <value>", "receive: <TRUE>")
            .replace("forall(TRUE)", "!value");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertCtl(output, "EX (sender-sent & sender-payload & !receiver-received & obs0)", true);
    }

    @Test
    public void supplyPayloadUsesThePreUpdateState() throws Exception {
        String model = source("supplier-locality")
            .replace("sent: boolean", "sent: boolean, payload: boolean")
            .replace("init: !sent", "init: !sent & !payload")
            .replace("value := TRUE", "value := payload")
            .replace("sent := TRUE", "sent := TRUE, payload := TRUE")
            .replace("receive: <value>", "receive: <TRUE>");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertCtl(output, "EX (sender-sent & sender-payload & !first-received)", true);
    }

    @Test
    public void channelChangeDoesNotRetargetCurrentMessage() throws Exception {
        String model = source("observation-forall-constant")
            .replace("channels: c", "channels: c,d")
            .replace("sent: boolean", "sent: boolean, ch: channel")
            .replace("init: !sent", "init: !sent & ch == c")
            .replace("c!", "ch!")
            .replace("sent := TRUE", "sent := TRUE, ch := d")
            .replace("forall(TRUE)", "channel == c");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertCtl(output, "EX (sender-sent & receiver-received & sender-ch = d & obs0)", true);
    }
}
