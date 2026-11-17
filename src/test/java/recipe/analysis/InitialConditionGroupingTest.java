package recipe.analysis;

import org.junit.Test;

import static recipe.analysis.Smv.*;
import static recipe.analysis.TranslationTestModels.*;

public class InitialConditionGroupingTest {
    @Test
    public void disjunctionCannotBypassAnotherInitialCondition() throws Exception {
        Smv.Module module = new Smv.Module();
        module.variable("a", "boolean");
        module.variable("b", "boolean");
        module.initially(or(atom("a"), atom("b")));
        module.initially(not(atom("a")));
        String output = module.render();
        assertCtl(output, "!a & b", true);
        // Establish that the conjunction has initial states, rather than passing vacuously.
        assertCtl(output, "FALSE", false);
    }

    @Test
    public void agentDisjunctionCannotBypassInstanceInitializer() throws Exception {
        String model = source("getter-state")
            .replace("init: !sent", "init: sent | !sent")
            .replace("Sender(sender, TRUE)", "Sender(sender, !sent)");
        String output = ToNuXmv.transform(parse(model));
        assertCtl(output, "!sender-sent", true);
        assertCtl(output, "FALSE", false);
        // The repaired initial state can still take the sender's transition.
        assertCtl(output, "EX sender-sent", true);
    }
}
