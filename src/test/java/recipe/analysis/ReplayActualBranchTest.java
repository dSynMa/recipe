package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayActualBranchTest {
    private void check(boolean supply) throws Exception {
        String text=source("observation-forall-constant");
        if(supply) text=text.replace("c! (@ready)","SUPPLY@(@ready)");
        String receive=supply?"GET@TRUE":"c?";
        text=text.replace("repeat: receive: <value> c? [received := value]",
            "repeat: one: <value> "+receive+"[received := value] + two: <value> "+receive+"[received := value]");
        var model=parse(text);
        NuXmvInteraction checker=new NuXmvInteraction(model);
        try {
            var result=checker.modelCheckBmc("G !receiver-two",true,2);
            assertTrue(result.getRight(),result.getLeft());
            assertTrue(result.getRight(),result.getRight().contains("is false"));
            var replay=Interpreter.ofTrace(model,result.getRight());
            assertEquals(2,replay.traceToJSON().size());
            assertEquals("two"+(supply?"":"?"),replay.getCurrentStep().toJSON().getJSONObject("state")
                .getJSONObject("receiver").getString("**last_label**"));
        } finally {checker.close();}
    }
    @Test(timeout=20000)
    public void realCounterexampleSelectsReceiveBranchByEvent() throws Exception {check(false);}
    @Test(timeout=20000)
    public void realCounterexampleSelectsGetBranchByEvent() throws Exception {check(true);}
    @Test(timeout=20000)
    public void actualInitialSimulationTraceCanBeReplayed() throws Exception {
        var model=parse(source("observation-forall-constant"));
        NuXmvInteraction checker=new NuXmvInteraction(model);
        try {
            assertTrue(checker.initialise(true).getLeft());
            String result = checker.execute(output -> output.trim().endsWith("nuXmv >"),
                "msat_pick_state -v -c \"TRUE\"");
            assertTrue(result, result.contains("Trace Type: Simulation"));
            var replay = Interpreter.ofTrace(model, result);
            assertEquals(1,replay.traceToJSON().size());
            assertFalse(replay.isDeadlocked());
        } finally {checker.close();}
    }
}
