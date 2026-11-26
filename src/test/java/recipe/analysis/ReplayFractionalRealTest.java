package recipe.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ReplayFractionalRealTest {
    private recipe.lang.System model(String value) throws Exception {
        return parse(source("arithmetic-multiplication")
            .replace("count: integer", "count: real")
            .replace("count == 2", "count == " + value)
            .replace("count * 2", "count + 0.5"));
    }
    @Test(timeout=20000)
    public void fractionalInitialSelectionKeepsTheRealValue() throws Exception {
        var model=model("0.5");
        try (NuXmvInteraction checker = new NuXmvInteraction(model)) {
            var raw=checker.simulation_pick_init_state("TRUE");
            assertTrue(raw.getRight(),raw.getLeft());
            Interpreter interpreter=new Interpreter(model);
            interpreter.init("TRUE");
            JSONObject state=interpreter.getCurrentStep().toJSON().getJSONObject("state");
            assertEquals(0,new java.math.BigDecimal("0.5").compareTo(new java.math.BigDecimal(state.getJSONObject("sender").getString("count"))));
        }
    }
    @Test(timeout=20000)
    public void terminatingFractionReplayMatchesConcreteArithmetic() throws Exception {
        var initial=new JSONObject("{sender:{sent:false,count:'0.5'},receiver:{received:false}}");
        var next=new JSONObject("{sender:{sent:true,count:'1.0'},receiver:{received:true}}");
        assertEquals(2,Interpreter.ofJSON(model("0.5"),new JSONArray().put(initial).put(next)).traceToJSON().size());
    }
    @Test(timeout=20000)
    public void actualFractionalBmcCounterexampleReplays() throws Exception {
        var model=model("0.25");
        try (NuXmvInteraction checker = new NuXmvInteraction(model)) {
            var raw=checker.modelCheckBmc("G !sender-sent",true,3);
            assertTrue(raw.getRight(),raw.getLeft());
            assertTrue(raw.getRight(),raw.getRight().contains("is false"));
            var replay=Interpreter.ofTrace(model,raw.getRight());
            assertEquals(2,replay.traceToJSON().size());
            assertEquals(0,new java.math.BigDecimal("0.75").compareTo(new java.math.BigDecimal(
                replay.getCurrentStep().toJSON().getJSONObject("state").getJSONObject("sender").getString("count"))));
        }
    }
    @Test(timeout=20000)
    public void negativeFractionalInitialStateKeepsItsSign() throws Exception {
        var model=model("(0 - 0.5)");
        try (NuXmvInteraction checker = new NuXmvInteraction(model)) {
            var raw=checker.simulation_pick_init_state("TRUE");
            assertTrue(raw.getRight(),raw.getLeft());
            Interpreter interpreter=new Interpreter(model);
            interpreter.init("TRUE");
            assertEquals(0,new java.math.BigDecimal("-0.5").compareTo(new java.math.BigDecimal(
                interpreter.getCurrentStep().toJSON().getJSONObject("state").getJSONObject("sender").getString("count"))));
        }
    }
    @Test(timeout=20000)
    public void traceAndInteractiveDecodersPreserveExactFractionValues() throws Exception {
        var model = model("0.5");
        var traceModel = model;
        java.util.function.Function<String, org.json.JSONObject> parser =
                output -> NuXmvInteraction.outputToJSON(traceModel, output);
        NuXmvInteraction interactive = new NuXmvInteraction(model) {
            @Override public Thread startNuXmvThread() { return new Thread(); }
        };
        String[][] cases = {
            {"f'1/2", "0.5"}, {"-f'1/2", "-0.5"}, {"f'1/8", "0.125"},
            {"f'12345678901234567890123456789/10", "1234567890123456789012345678.9"},
            {"0.75", "0.75"}, {"f'1/3", "f'1/3"}
        };
        try {
            for (var decode : java.util.List.<java.util.function.Function<String, JSONObject>>of(
                    parser, interactive::outputToJSON)) {
                for (String[] pair : cases) {
                    assertEquals(pair[1], decode.apply("  sender-count = " + pair[0] + "\n")
                        .getJSONObject("sender").getString("count"));
                }
            }
        } finally {
            java.nio.file.Files.deleteIfExists(interactive.path);
        }
    }
}
