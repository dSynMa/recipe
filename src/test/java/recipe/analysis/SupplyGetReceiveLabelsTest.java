package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class SupplyGetReceiveLabelsTest {
    @Test
    public void getterCannotInventAReceiveEvent() throws Exception {
        assertCtl(ToNuXmv.transform(parse(source("receive-label-on-get"))),
            "AG !receiver-ghost", true);
    }

    @Test
    public void supplierCannotInventAReceiveEvent() throws Exception {
        String model = source("receive-label-on-get").replace(
            "send: <TRUE> SUPPLY@(@ready)(value := TRUE)[sent := TRUE]",
            "(send: <TRUE> SUPPLY@(@ready)(value := TRUE)[sent := TRUE] + phantom: <TRUE> c? [])");
        assertCtl(ToNuXmv.transform(parse(model)), "AG !sender-phantom", true);
    }

    @Test
    public void getClearsAPrecedingReceiveEvent() throws Exception {
        String model = source("send-observation-on-get")
            .replace("repeat: send:", "repeat: <TRUE> c! (TRUE)(value := TRUE)[]; send:")
            .replace("repeat: receive:", "repeat: actual: <TRUE> c? []; receive:");
        String output = ToNuXmv.transform(parse(model));
        assertCtl(output, "EX receiver-actual", true);
        assertCtl(output, "AG (receiver-actual -> AX !receiver-actual)", true);
        assertCtl(output, "EX EX receiver-received", true);
    }
}
