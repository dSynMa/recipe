package recipe.analysis;

import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class OmittedLocalScopeTest {
    private String withoutReceiverLocals() throws Exception {
        return source("observation-forall-constant")
            .replace("    local: received: boolean\n", "")
            .replace("init: !received", "init: TRUE")
            .replace("ready <- !received", "ready <- TRUE")
            .replace("received := value", "");
    }

    @Test
    public void omittedLocalClauseDoesNotExposePreviousAgentVariables() throws Exception {
        String model = withoutReceiverLocals().replace("init: TRUE", "init: !sent");
        assertFalse(recipe.lang.System.parser().end().parse(model).isSuccess());
    }

    @Test
    public void agentsWithoutLocalsStillTranslateAndCommunicate() throws Exception {
        String output = ToNuXmv.transform(parse(withoutReceiverLocals()));
        assertValid(output);
        assertCtl(output, "EX sender-sent", true);
    }

    @Test
    public void explicitEmptyLocalClauseHasTheSameScope() throws Exception {
        String model = withoutReceiverLocals().replace("agent Receiver\n", "agent Receiver\n    local:\n");
        assertValid(ToNuXmv.transform(parse(model)));
        String invalid = model.replace("init: TRUE", "init: !sent");
        assertFalse(recipe.lang.System.parser().end().parse(invalid).isSuccess());
    }
}
