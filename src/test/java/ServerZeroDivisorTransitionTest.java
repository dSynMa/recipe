import flak.Query;
import flak.Request;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServerZeroDivisorTransitionTest {
    @Test(timeout = 10000)
    public void uiDisablesZeroDivisorTransitionsWithoutCheckerErrors() throws Exception {
        String source = Files.readString(Path.of(
            "src/test/resources/recipe/analysis/adversarial/arithmetic-multiplication.rcp"))
            .replace("count * 2", "count / (count - 2)")
            + "\nSPEC G !sender-sent;\n";
        Server server = new Server();
        try {
            Map<String, String> params = Map.of("script", source, "symbolic", "true");
            Query query = (Query) Proxy.newProxyInstance(Query.class.getClassLoader(), new Class<?>[]{Query.class},
                (proxy, method, args) -> method.getName().equals("get") ? params.get(args[0]) : null);
            Request request = (Request) Proxy.newProxyInstance(Request.class.getClassLoader(), new Class<?>[]{Request.class},
                (proxy, method, args) -> method.getName().equals("getQuery") ? query : null);
            JSONObject build = new JSONObject(server.buildModel(request));
            assertTrue(build.toString(), build.getBoolean("success"));
            server.mcConfig = new Server.MCConfig(Server.MCType.BMC, 3);
            JSONObject result = new JSONObject(server.modelCheck(null, "0"));
            assertEquals(result.toString(), "unknown", result.getString("result"));
            String output = result.getString("output");
            assertTrue(output, output.contains("no counterexample found with bound 3"));
            assertFalse(output, output.contains("Division by zero"));
            assertFalse(output, output.contains("There is a division by 0"));
        } finally {
            if (server.nuXmvInteraction != null) server.nuXmvInteraction.stopNuXmvThread();
            Field service = Server.class.getDeclaredField("service");
            service.setAccessible(true);
            ((ExecutorService) service.get(server)).shutdownNow();
        }
    }
}
