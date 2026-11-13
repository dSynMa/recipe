package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class ActionLabelEventTest {
    @Test
    public void sendLabelsDescribeEnablednessAndReceiveLabelsDescribeEvents() throws Exception {
        String output = ToNuXmv.transform(parse(source("observation-forall-constant")));
        assertValid(output);
        assertCtl(output, "sender-send & !receiver-receive", true);
        assertCtl(output, "EX (!sender-send & receiver-receive)", true);
        assertCtl(output, "AG (receiver-receive -> AX (!sender-send & !receiver-receive))", true);
    }

    @Test
    public void supplyAndGetEventsIdentifyOnlyTheParticipants() throws Exception {
        String model = source("supplier-locality").replace("send: <TRUE>", "send: <!sent>");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertCtl(output, "!sender-send & !first-receive & !second-receive", true);
        assertCtl(output, "EX (sender-send & first-receive & !second-receive)", true);
        assertCtl(output, "AG (sender-send -> AX (!sender-send & !first-receive & !second-receive))", true);
    }

    @Test
    public void sendLabelsFollowTheCurrentControlLocation() throws Exception {
        String model = source("observation-forall-constant")
            .replace("ready <- !received", "ready <- TRUE")
            .replace("[sent := TRUE]", "[sent := TRUE] ; later: <TRUE> c! (@ready)(value := TRUE)[sent := TRUE]");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertCtl(output, "sender-send & !sender-later & EX (!sender-send & sender-later)", true);
    }


    @Test
    public void repeatedSendLabelDenotesEitherAlternative() throws Exception {
        String model = source("observation-forall-constant")
            .replace("receive: <value>", "receive: <TRUE>")
            .replace("[sent := TRUE]", "[sent := TRUE] + send: <TRUE> c! (@ready)(value := FALSE)[sent := TRUE]");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertCtl(output, "sender-send & EX (receiver-receive & receiver-received)", true);
        assertCtl(output, "sender-send & EX (receiver-receive & !receiver-received)", true);
    }

    @Test
    public void sendLabelIsUsableInAnLtlSpecification() throws Exception {
        String output = ToNuXmv.transform(parse(source("observation-forall-constant")
            + "\nSPEC sender-send & !receiver-receive;\n"));
        assertLtl(output, "sender-send & !receiver-receive", true);
    }

    @Test
    public void alternativeSendCanBeEnabledWithoutBeingExecuted() throws Exception {
        String model = source("observation-forall-constant")
            .replace("[sent := TRUE]", "[sent := TRUE] + other: <TRUE> c! (@ready)(value := TRUE)[sent := TRUE]");
        String output = ToNuXmv.transform(parse(model));
        assertCtl(output, "sender-send & sender-other", true);
        assertCtl(output, "AX (!sender-send & !sender-other & receiver-receive)", true);
    }

    @Test
    public void sendAndEventCannotShareALabel() throws Exception {
        String model = source("observation-forall-constant")
            .replace("[sent := TRUE]", "[sent := TRUE] + send: <TRUE> c? [sent := TRUE]");
        org.junit.Assert.assertThrows(IllegalArgumentException.class, () -> ToNuXmv.transform(parse(model)));
    }

    @Test
    public void repeatedGetLabelRetainsBothRendezvousAlternatives() throws Exception {
        String model = source("supplier-locality")
            .replace("[received := value]", "[received := value] + receive: <TRUE> GET@TRUE [received := FALSE]");
        String output = ToNuXmv.transform(parse(model));
        assertValid(output);
        assertCtl(output, "EX (sender-send & first-receive & first-received)", true);
        assertCtl(output, "EX (sender-send & first-receive & !first-received)", true);
    }
}
