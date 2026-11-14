package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationCrossDomainObservationTest {
    private String model(String selection, String observation) {
        return "channels: c\nmessage-structure: value: 0..2\n"
            + "communication-variables: rank: 0..2, tier: 1..3\n"
            + "agent Sender\nlocal: pivot: 0..2, sent: boolean\ninit: !sent\n"
            + "relabel: rank <- pivot tier <- pivot + 1\nreceive-guard: FALSE\n"
            + "repeat: send: <!sent> *! (" + selection + ")(value := pivot)[sent := TRUE]\n"
            + "agent Receiver\nlocal: received: boolean\ninit: !received\n"
            + "relabel: rank <- 0 tier <- 1\nreceive-guard: FALSE\n"
            + "repeat: receive: <TRUE> *? [received := TRUE]\n"
            + "system = Sender(sender, TRUE) | Receiver(receiver, TRUE)\n"
            + "SPEC G (<" + observation + ">true);\n";
    }

    @Test(timeout=60000)
    public void correlatedDomainsRetainSymbolicSenderAndMessageValues() throws Exception {
        String selection = "@rank == pivot & @tier == pivot + 1";
        String[] observations = {"rank + tier == 2 * value + 1", "forall(rank + tier == 2 * value + 1)",
            "exists(rank + tier == 2 * value + 1)", "rank + tier != 2 * value + 1",
            "forall(rank + tier != 2 * value + 1)", "exists(rank + tier != 2 * value + 1)"};
        for (int i = 0; i < observations.length; i++) {
            String smv = ToNuXmv.transform(parse(model(selection, observations[i])));
            assertCtl(smv, "AX " + (i < 3 ? "obs0" : "!obs0"), true);
            assertCtl(smv, "AX sender-sent", true);
        }
    }

    @Test(timeout=60000)
    public void emptyCrossDomainSelectionPreservesQuantifierVacuity() throws Exception {
        String[] observations = {"rank + tier == value", "forall(rank + tier == value)",
            "exists(rank + tier == value)", "rank + tier != value"};
        for (int i = 0; i < observations.length; i++) {
            String smv = ToNuXmv.transform(parse(model("@rank == pivot & @tier < 0", observations[i])));
            assertCtl(smv, "AX " + (i < 2 ? "obs0" : "!obs0"), true);
            assertCtl(smv, "AX (sender-sent & !receiver-received)", true);
        }
    }
    @Test(timeout=60000)
    public void integralConstantFoldsRemainNumericInFiniteDomainChecking() throws Exception {
        String[] observations = {"rank + 1 == value + 1", "rank - 1 == value - 1",
            "rank * 2 == value * 2", "rank / 1 == value"};
        for (String observation : observations) {
            String smv = ToNuXmv.transform(parse(model("@rank == pivot", observation)));
            assertCtl(smv, "AX obs0", true);
        }
    }

}
