package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class BroadcastSpecificationTest {
    @Test
    public void broadcastLiteralIsRenderedInStateSpecifications() throws Exception {
        String model = source("non-listening-enabled-receive")
            .replace("local: sent: boolean", "local: sent: boolean, ch: channel")
            .replace("init: !sent", "init: !sent & ch == *")
            + "\nSPEC G (sender-ch == *);\n";
        assertValid(ToNuXmv.transform(parse(model.replace("SPEC G (sender-ch == *)", "SPEC G (sender-ch == c)"))));
        assertValid(ToNuXmv.transform(parse(model)));
    }
}
