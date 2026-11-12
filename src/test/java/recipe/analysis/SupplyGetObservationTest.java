package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class SupplyGetObservationTest {
    @Test
    public void getCannotInventAChannelSendObservation() throws Exception {
        String output = ToNuXmv.transform(parse(source("send-observation-on-get")));
        assertCtl(output, "EX receiver-received", true);
        assertCtl(output, "AG !obs0", true);
    }

    @Test
    public void getClearsObservationOfPrecedingSend() throws Exception {
        String model = source("send-observation-on-get")
            .replace("repeat: send:", "repeat: <TRUE> c! (TRUE)(value := TRUE)[]; send:")
            .replace("repeat: receive:", "repeat: actual: <TRUE> c? []; receive:");
        String output = ToNuXmv.transform(parse(model));
        assertCtl(output, "EX obs0", true);
        assertCtl(output, "AG (obs0 -> AX !obs0)", true);
        assertCtl(output, "EX EX receiver-received", true);
    }
}
