import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import recipe.lang.ltol.Atom;
import recipe.lang.expressions.TypedVariable;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServerModelCheckErrorTest {
    @Test(timeout = 10000)
    public void uiReportsCheckerDiagnosticsAsErrors() throws Exception {
        String source = Files.readString(Path.of(
            "src/test/resources/recipe/analysis/adversarial/arithmetic-multiplication.rcp"))
            + "\nSPEC G (sender-count >= 0);\n";
        Server server = new Server();
        server.system = recipe.lang.System.parser().end().parse(source).get();
        // Supply an undefined checker symbol to exercise the server's diagnostic response.
        server.system.getSpecs().set(0, new Atom(new TypedVariable<>(
            recipe.lang.types.Boolean.getType(), "undefined_checker_symbol")));
        server.mcConfig = new Server.MCConfig(Server.MCType.BMC, 3);
        try {
            JSONObject result = new JSONObject(server.modelCheck(null, "0"));
            assertEquals(result.toString(), "error", result.getString("result"));
            assertTrue(result.toString(), result.getString("output").contains("SMT model not built"));
        } finally {
            Field service = Server.class.getDeclaredField("service");
            service.setAccessible(true);
            ((ExecutorService) service.get(server)).shutdownNow();
        }
    }
}
