package recipe.interpreter;

import org.json.JSONObject;
import org.junit.Test;
import recipe.lang.System;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

public class LocationInterpreterTest {

    private static String resource(String name) throws Exception {
        try (InputStream input = LocationInterpreterTest.class.getResourceAsStream(
                "/recipe/analysis/deserialization/" + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void locationPairingAndMyselfWorkInTheInterpreter() throws Exception {
        assumeTrue(Files.isExecutable(Path.of("nuxmv/bin/nuxmv")));

        System system = System.deserialize(new JSONObject(resource("location-pair.json")));
        Interpreter interpreter = new Interpreter(system);
        interpreter.init("TRUE");

        JSONObject initialState = interpreter.getCurrentStep().toJSON().getJSONObject("state");
        assertEquals("s", initialState.getJSONObject("s").getString("myself"));
        assertEquals("r", initialState.getJSONObject("r").getString("myself"));

        assertFalse(interpreter.isDeadlocked());
        assertEquals(1, interpreter.getCurrentStep().transitions.size());
        interpreter.next(0);

        JSONObject getterState = interpreter.getCurrentStep().toJSON()
            .getJSONObject("state").getJSONObject("r");
        assertEquals("recv", getterState.getString("**last_label**"));
    }
}
