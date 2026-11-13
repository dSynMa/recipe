package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class BoundedUpdateProgressTest {
    private void assertReachesDeadlockAndStutters(String model, String counter) throws Exception {
        assertReachesDeadlockAndStutters(model, counter, 1);
    }

    private void assertReachesDeadlockAndStutters(String model, String counter, int endpoint) throws Exception {
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertLtl(output, "FALSE", false); // Reject vacuous success from an empty set of infinite runs.
        assertCtl(output, "EF (" + counter + " = " + endpoint + " & !progress)", true);
        assertCtl(output, "AG EX TRUE", true);
    }

    @Test
    public void sendUpdateMustFitItsTargetDomain() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..1").replace("count == 2", "count == 0")
            .replace("count * 2", "count + 1").replace("ready <- !received", "ready <- TRUE");
        assertReachesDeadlockAndStutters(model, "sender-count");
    }

    @Test
    public void updatesCannotCrossTheLowerBoundEither() throws Exception {
        String model = source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..1").replace("count == 2", "count == 1")
            .replace("count * 2", "count - 1").replace("ready <- !received", "ready <- TRUE");
        assertReachesDeadlockAndStutters(model, "sender-count", 0);
    }

    @Test
    public void receiveUpdateMustFitItsTargetDomain() throws Exception {
        String model = source("observation-forall-constant")
            .replace("received: boolean", "received: boolean, count: 0..1")
            .replace("init: !received", "init: !received & count == 0")
            .replace("received := value", "received := value, count := count + 1")
            .replace("ready <- !received", "ready <- TRUE");
        assertReachesDeadlockAndStutters(model, "receiver-count");
    }

    @Test
    public void supplyUpdateMustFitItsTargetDomain() throws Exception {
        String model = source("supplier-locality")
            .replace("sent: boolean", "sent: boolean, count: 0..1")
            .replace("init: !sent", "init: !sent & count == 0")
            .replace("sent := TRUE", "sent := TRUE, count := count + 1");
        assertReachesDeadlockAndStutters(model, "sender-count");
    }

    @Test
    public void getUpdateMustFitItsTargetDomain() throws Exception {
        String model = source("supplier-locality")
            .replace("received: boolean", "received: boolean, count: 0..1")
            .replace("init: !received", "init: !received & count == 0")
            .replace("received := value", "received := value, count := count + 1");
        assertReachesDeadlockAndStutters(model, "first-count");
    }
}
