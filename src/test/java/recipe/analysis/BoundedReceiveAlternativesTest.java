package recipe.analysis;
import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

/** Follow-up adversarial cases beyond the individual reproductions. */
public class BoundedReceiveAlternativesTest {
    private String boundedBroadcast() throws Exception {
        return source("observation-forall-constant")
            .replace("received: boolean", "received: boolean, count: 0..1")
            .replace("init: !received", "init: !received & count == 0")
            .replace("received := value", "received := value, count := count + 1")
            .replace("ready <- !received", "ready <- TRUE")
            .replace("c!", "*!").replace("c?", "*?");
    }
    @Test public void disabledBoundedReceiveDoesNotBlockBroadcast() throws Exception {
        String output = ToNuXmv.transform(parse(boundedBroadcast()));
        assertValid(output);
        assertLtl(output, "FALSE", false);
        assertCtl(output, "EF (receiver-count = 1 & progress)", true);
        assertCtl(output, "AG (receiver-count = 1 -> AX (receiver-count = 1 & !receiver-receive))", true);
    }
    @Test public void feasibleReceiveAlternativeRemainsAvailable() throws Exception {
        String model = boundedBroadcast().replace("[received := value, count := count + 1]",
            "[received := value, count := count + 1] + fallback: <value> *? [received := value]");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertLtl(output, "FALSE", false);
        assertCtl(output, "EF (receiver-count = 1 & EX receiver-fallback)", true);
    }
}
