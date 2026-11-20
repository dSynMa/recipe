package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class MissingObservationFieldTest {
    private String model(String fieldType, String observation) throws Exception {
        return source("arithmetic-multiplication")
            .replace("count: integer", "count: 0..8")
            .replace("send: <TRUE>", "send: <!sent>")
            .replace("message-structure: value: boolean", "message-structure: value: boolean, extra: " + fieldType)
            + "\nSPEC G (<" + observation + ">true);\n";
    }

    @Test
    public void absentBooleanFieldDoesNotSatisfyPositiveOrNegativeObservation() throws Exception {
        for (String observation : new String[] {"extra", "!extra"}) {
            String output = ToNuXmv.transform(parse(model("boolean", observation)));
            assertValid(output);
            assertCtl(output, "EX sender-sent", true);
            assertCtl(output, "AG !obs0", true);
        }
    }

    @Test
    public void suppliedBooleanFieldStillDeterminesObservation() throws Exception {
        for (String value : new String[] {"TRUE", "FALSE"}) {
            String source = model("boolean", "extra").replace("(value := TRUE)",
                "(value := TRUE, extra := " + value + ")");
            assertCtl(ToNuXmv.transform(parse(source)), "EX obs0", value.equals("TRUE"));
        }
    }

    @Test
    public void numericMessageFieldCanBeAbsentOrSupplied() throws Exception {
        String source = model("integer", "extra == 1");
        assertCtl(ToNuXmv.transform(parse(source)), "AG !obs0", true);
        assertCtl(ToNuXmv.transform(parse(source.replace("(value := TRUE)",
            "(value := TRUE, extra := 1)"))), "EX obs0", true);
    }

    @Test
    public void unreferencedMissingFieldDoesNotDisableObservation() throws Exception {
        assertCtl(ToNuXmv.transform(parse(model("boolean", "channel == c"))), "EX obs0", true);
    }
}
