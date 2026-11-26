package recipe.analysis;

import flak.Query;
import flak.Request;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ServerModelSwitchTest {
    private Request request(Map<String, String> values) {
        Object query = Proxy.newProxyInstance(Query.class.getClassLoader(), new Class<?>[]{Query.class},
            (p, m, a) -> m.getName().equals("get") ? values.get(a[0]) : null);
        return (Request) Proxy.newProxyInstance(Request.class.getClassLoader(), new Class<?>[]{Request.class},
            (p, m, a) -> m.getName().equals("getQuery") ? query : null);
    }

    private Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Test(timeout = 30000)
    public void modelSwitchChecksNewInitialState() throws Exception {
        checkCurrentModel(true);
    }

    @Test(timeout = 30000)
    public void setSystemAloneCanStartBatchChecking() throws Exception {
        checkCurrentModel(false);
    }

    @Test(timeout = 30000)
    public void selectingModelDropsPreviousReplayAndRenderedState() throws Exception {
        Class<?> type = Class.forName("Server");
        Object server = type.getDeclaredConstructor().newInstance();
        try {
            String model = source("observation-forall-constant");
            recipe.interpreter.Interpreter previous = new recipe.interpreter.Interpreter(parse(model));
            previous.init("TRUE");
            field(type, "interpreter").set(server, previous);
            ((Map) field(type, "latestDots").get(server)).put("sender", "old graph");
            ((Map) field(type, "latestDotsInterpreter").get(server)).put("sender", "old replay graph");
            String selected = (String) type.getMethod("setSystem", Request.class).invoke(server,
                request(Map.of("script", model.replace("init: !sent", "init: sent"))));
            assertFalse(selected, new JSONObject(selected).has("error"));
            assertNull("Replay must not retain the old model and trace", field(type, "interpreter").get(server));
            assertTrue(((Map) field(type, "latestDots").get(server)).isEmpty());
            assertTrue(((Map) field(type, "latestDotsInterpreter").get(server)).isEmpty());
        } finally {
            ((ExecutorService) field(type, "service").get(server)).shutdownNow();
        }
    }

    private void checkCurrentModel(boolean buildOld) throws Exception {
        Set<Long> existing = ProcessHandle.current().descendants().map(ProcessHandle::pid).collect(Collectors.toSet());
        Class<?> type = Class.forName("Server");
        Object server = type.getDeclaredConstructor().newInstance();
        String old = source("observation-forall-constant")
            .replace("SPEC G (<forall(TRUE)>true);", "SPEC G sender-sent;");
        try {
            if (buildOld) {
                String built = (String) type.getMethod("buildModel", Request.class).invoke(server,
                    request(Map.of("script", old, "symbolic", "true")));
                assertTrue(built, new JSONObject(built).getBoolean("success"));
            }
            String selected = (String) type.getMethod("setSystem", Request.class).invoke(server,
                request(Map.of("script", old.replace("init: !sent", "init: sent"))));
            assertFalse(selected, new JSONObject(selected).has("error"));
            Request check = request(Map.of("ic3", "true"));
            String prepared = (String) type.getMethod("modelCheck", Request.class).invoke(server, check);
            assertFalse(prepared, new JSONObject(prepared).has("error"));
            String result = (String) type.getMethod("modelCheck", Request.class, String.class).invoke(server, check, "0");
            assertEquals(result, "true", new JSONObject(result).optString("result"));
        } finally {
            NuXmvInteraction interactive = (NuXmvInteraction) field(type, "nuXmvInteraction").get(server);
            if (interactive != null) interactive.stopNuXmvThread();
            ((ExecutorService) field(type, "service").get(server)).shutdownNow();
            // Also clean children if a lifecycle regression prevents graceful shutdown.
            ProcessHandle.current().descendants().filter(p -> !existing.contains(p.pid())).forEach(ProcessHandle::destroyForcibly);
        }
    }
}
