package recipe.analysis;

import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayActualTerminalLoopTest {
    @Test(timeout=30000)
    public void realLivenessCounterexampleRetainsTerminalLoopMarker() throws Exception {
        var model=parse(source("observation-forall-constant"));
        NuXmvInteraction checker=new NuXmvInteraction(model);
        try {
            var result=checker.modelCheckBmc("G F !sender-sent",true,5);
            assertTrue(result.getRight(),result.getLeft());
            assertTrue(result.getRight(),result.getRight().contains("is false"));
            assertTrue(result.getRight(),result.getRight().contains("-- Loop starts here"));
            var replay=Interpreter.ofTrace(model,result.getRight());
            assertTrue(replay.isDeadlocked());
            var states = replay.traceToJSON();
            assertEquals(2, states.size());
            assertFalse(states.get(0).optBoolean("___LOOP___", false));
            assertTrue(states.toString(), states.get(1).optBoolean("___LOOP___", false));
        } finally { checker.close(); }
    }
    @Test(timeout=20000)
    public void markedDeadlockCannotHideLaterStateChanges() throws Exception {
        var model = parse(source("observation-forall-constant").replace("send: <TRUE>", "send: <FALSE>"));
        var initial = new org.json.JSONObject("{sender:{sent:false},receiver:{received:false},___STUCK___:true}");
        var changed = new org.json.JSONObject("{sender:{sent:true}}");
        Exception error = assertThrows(Exception.class, () -> Interpreter.ofJSON(model,
            new org.json.JSONArray().put(initial).put(new org.json.JSONObject()).put(changed)));
        assertTrue(error.getMessage(), error.getMessage().contains("step 2"));
    }
}
