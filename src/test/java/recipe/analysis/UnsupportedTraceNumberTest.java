package recipe.analysis;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import recipe.interpreter.Interpreter;
import recipe.lang.store.ConcreteStore;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class UnsupportedTraceNumberTest {
    private recipe.lang.System model(String initial) throws Exception {
        return parse(source("arithmetic-multiplication").replace("count: integer", "count: real")
                .replace("count == 2", initial).replace("count * 2", "count + 0.5"));
    }

    private void diagnostic(String error, String value) {
        assertTrue(error, error.contains("sender-count"));
        assertTrue(error, error.contains(value));
        assertTrue(error, error.contains("exact finite decimal"));
    }

    @Test
    public void storeRejectsUnsupportedNumbersInsteadOfDroppingVariables() throws Exception {
        var model = model("count == 0.5");
        var sender = model.getAgentInstances().stream().filter(a -> a.getLabel().equals("sender")).findFirst().orElseThrow();
        var state = new JSONObject().put("automaton-state", "0").put("count", "f'1/3");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new ConcreteStore(state, sender));
        diagnostic(error.getMessage(), "f'1/3");
        state.put("count", "f'1/2");
        var store = new ConcreteStore(state, sender);
        assertEquals("0.5", store.getValue(sender.getAgent().getStore().getAttribute("count")).getValue().toString());
    }

    @Test
    public void replayRejectsUnsupportedNumbersAtEveryPositionBeforeLoading() throws Exception {
        var model = model("count == 0.5");
        for (int index : new int[] {0, 1}) {
            JSONArray states = new JSONArray().put(new JSONObject("{sender:{count:'0.5'}}"));
            if (index == 1) states.put(new JSONObject("{sender:{count:'0.5'}}"));
            states.getJSONObject(index).getJSONObject("sender").put("count", "f'1/3");
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Interpreter.ofJSON(model, states));
            diagnostic(error.getMessage(), "f'1/3");
            assertTrue(error.getMessage(), error.getMessage().contains("step " + index));
        }
    }

    @Test(timeout = 20000)
    public void interpreterInitialisationReportsTheUnsupportedCheckerValue() throws Exception {
        Interpreter interpreter = new Interpreter(model("count * 3 == 1"));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> interpreter.init("TRUE"));
        diagnostic(error.getMessage(), "f'1/3");
        assertNull(interpreter.getCurrentStep());
    }

    private Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }

    private void serverError(String method, recipe.lang.System model, Map<String, String> params) throws Exception {
        Class<?> serverType = Class.forName("Server");
        Object server = serverType.getDeclaredConstructor().newInstance();
        Class<?> queryType = Class.forName("flak.Query");
        Object query = Proxy.newProxyInstance(queryType.getClassLoader(), new Class<?>[] {queryType},
                (proxy, called, args) -> called.getName().equals("get") ? params.get(args[0]) : null);
        Class<?> requestType = Class.forName("flak.Request");
        Object request = Proxy.newProxyInstance(requestType.getClassLoader(), new Class<?>[] {requestType},
                (proxy, called, args) -> called.getName().equals("getQuery") ? query : null);
        Interpreter previous = new Interpreter(model);
        try {
            field(serverType, "system").set(server, model);
            field(serverType, "interpreter").set(server, previous);
            String response = (String) serverType.getMethod(method, requestType).invoke(server, request);
            diagnostic(new JSONObject(response).getString("error"), "f'1/3");
            assertSame(previous, field(serverType, "interpreter").get(server));
        } finally {
            ((ExecutorService) field(serverType, "service").get(server)).shutdownNow();
        }
    }

    @Test(timeout = 20000)
    public void resetAndReplayEndpointsReturnJsonErrorsAndPreserveTheInterpreter() throws Exception {
        serverError("interpretNext", model("count * 3 == 1"), Map.of("reset", "true"));
        serverError("loadInterpreterJSON", model("count == 0.5"),
                Map.of("trace", new JSONArray().put(new JSONObject().put("sender",
                        new JSONObject().put("count", "f'1/3"))).toString()));
        serverError("loadInterpreter", model("count == 0.5"), Map.of("output",
                "Trace Type: Counterexample\n-> State: 1.1 <-\n  sender-count = f'1/3\n"));
    }
}
