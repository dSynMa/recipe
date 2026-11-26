package recipe.analysis;

import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class RepetitionKeywordBoundaryTest {
    @Test
    public void labelsBeginningWithRepRemainIdentifiers() throws Exception {
        for (String label : new String[] {"report", "reply", "replicate"}) {
            String model = source("observation-forall-constant").replace("send:", label + ":");
            String output = ToNuXmv.transform(parse(model));
            assertValid(output);
            assertCtl(output, "sender-" + label, true);
            assertCtl(output, "EX sender-sent", true);
        }
    }

    @Test
    public void repetitionStillAcceptsAdjacentParenthesesAndWhitespace() throws Exception {
        for (String prefix : new String[] {"rep(", "rep ("}) {
            String model = source("observation-forall-constant")
                .replace("repeat: send:", "repeat: " + prefix + "send:")
                .replace("[sent := TRUE]", "[sent := TRUE])");
            assertValid(ToNuXmv.transform(parse(model)));
        }
    }
}
