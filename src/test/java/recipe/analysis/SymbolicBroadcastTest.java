package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class SymbolicBroadcastTest {
    @Test
    public void symbolicBroadcastStillReachesFalseGuardReceiver() throws Exception {
        String model = source("non-listening-enabled-receive")
            .replace("local: sent: boolean", "local: sent: boolean, ch: channel")
            .replace("init: !sent", "init: !sent & ch == *")
            .replace("c!", "ch!").replace("c?", "*?");
        assertCtl(ToNuXmv.transform(parse(model.replace("ch!", "*!"))),
            "EX (sender-sent & receiver-received)", true);
        assertCtl(ToNuXmv.transform(parse(model)), "EX (sender-sent & receiver-received)", true);
    }
}
