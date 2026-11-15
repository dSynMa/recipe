package recipe.analysis;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ServerObservationNumberingTest {
    private Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Test(timeout = 20000)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void checkingALaterSpecificationUsesItsOwnObservationVariables() throws Exception {
        for (String mode : new String[] {"BDD", "BMC", "IC3"}) {
            for (String first : new String[] {"F (<value>true)", "F (<value>true & <channel == c>true)"}) {
                String text = source("observation-forall-constant")
                    .replace("SPEC G (<forall(TRUE)>true);", "SPEC " + first + ";\nSPEC F (<!value>true);");
                recipe.lang.System model = parse(text);
                Class<?> serverType = Class.forName("Server");
                Object server = serverType.getDeclaredConstructor().newInstance();
                try {
                    field(serverType, "system").set(server, model);
                    Class<?> kind = Class.forName("Server$MCType");
                    Object backend = java.lang.Enum.valueOf((Class) kind, mode);
                    Object config = Class.forName("Server$MCConfig").getConstructor(kind, int.class).newInstance(backend, 5);
                    field(serverType, "mcConfig").set(server, config);
                    var method = serverType.getDeclaredMethod("doModelCheck", int.class);
                    method.setAccessible(true);
                    JSONObject firstResult = (JSONObject) method.invoke(server, 0);
                    assertEquals(firstResult.toString(), mode.equals("BMC") ? "unknown" : "true", firstResult.getString("result"));
                    JSONObject secondResult = (JSONObject) method.invoke(server, 1);
                    assertEquals(secondResult.toString(), "false", secondResult.getString("result"));
                } finally {
                    ((ExecutorService) field(serverType, "service").get(server)).shutdownNow();
                }
            }
        }
    }
}
