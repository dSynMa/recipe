package recipe.analysis;

import flak.App;
import flak.Query;
import flak.Request;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ServerInteractiveLifecycleTest {
    private Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @Test(timeout = 20000) public void resettingSimulationStopsItsChecker() throws Exception { exercise("resetSimulation"); }
    @Test(timeout = 20000) public void reinitialisingStopsThePreviousChecker() throws Exception { exercise("init"); }
    @Test(timeout = 20000) public void preparingChecksClearsTheStoppedSimulationChecker() throws Exception { exercise("modelCheck"); }

    private void exercise(String operation) throws Exception {
        Class<?> type = Class.forName("Server");
        Object server = type.getDeclaredConstructor().newInstance();
        Field interaction = field(type, "nuXmvInteraction");
        Field app = field(type, "app");
        Object oldApp = app.get(null);
        app.set(null, Proxy.newProxyInstance(App.class.getClassLoader(), new Class<?>[]{App.class}, (p, m, a) -> null));
        Object query = Proxy.newProxyInstance(Query.class.getClassLoader(), new Class<?>[]{Query.class}, (p, m, a) -> null);
        Request req = (Request) Proxy.newProxyInstance(Request.class.getClassLoader(), new Class<?>[]{Request.class},
            (p, m, a) -> m.getName().equals("getQuery") ? query : null);
        recipe.lang.System model = parse(source("observation-forall-constant"));
        field(type, "system").set(server, model);
        NuXmvInteraction previous = new NuXmvInteraction(model);
        interaction.set(server, previous);
        try {
            type.getMethod(operation, Request.class).invoke(server, req);
            assertFalse(operation + " must terminate its previous worker", previous.process.isAlive());
            Object current = interaction.get(server);
            if (operation.equals("init")) {
                assertNotSame(previous, current);
                assertNotNull(current);
            } else {
                assertNull("A stopped checker must not be reused by simulation", current);
            }
        } finally {
            NuXmvInteraction current = (NuXmvInteraction) interaction.get(server);
            if (current != null) current.stopNuXmvThread();
            previous.stopNuXmvThread();
            app.set(null, oldApp);
            ((ExecutorService) field(type, "service").get(server)).shutdownNow();
        }
    }
}
