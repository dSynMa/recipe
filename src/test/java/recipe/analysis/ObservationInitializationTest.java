package recipe.analysis;

import org.junit.Test;

import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ObservationInitializationTest {
    @Test
    public void initializesObservationOnceWithoutLeadingConjunction() throws Exception {
        String output = ToNuXmv.transform(parse(source("negated-observation")));
        String init = output.substring(output.indexOf("INIT\n") + 5, output.indexOf("TRANS\n"));
        assertFalse(init.stripLeading().startsWith("&"));
        assertEquals(1, init.split("obs0 = FALSE", -1).length - 1);
        assertValid(output);
        assertCtl(output, "!obs0", true);
    }

    @Test
    public void alsoInitializesSystemsWithoutObservations() throws Exception {
        String output = ToNuXmv.transform(parse(source("getter-state")));
        assertCtl(output, "!sender-sent & !receiver-received", true);
    }
}
