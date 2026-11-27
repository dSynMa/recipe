package recipe.analysis;

import org.json.JSONObject;
import org.junit.Test;
import recipe.lang.System;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static recipe.analysis.TranslationTestModels.assertLtl;
import static recipe.analysis.TranslationTestModels.assertValid;

public class DeserializationTest {

    private static String resource(String name) throws Exception {
        try (InputStream input = DeserializationTest.class.getResourceAsStream(
                "/recipe/analysis/deserialization/" + name)) {
            assertNotNull(name, input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void deserializedSystemProducesAValidModel() throws Exception {
        System system = System.deserialize(new JSONObject(resource("broadcast.json")));
        String output = ToNuXmv.transform(system);

        assertValid(output);
        assertTrue(output, output.contains("MODULE main"));
        assertTrue(output, output.contains("s-snd"));
        assertTrue(output, output.contains("r-rcv"));
    }

    @Test
    public void deserializedSupplyGetProducesAValidModel() throws Exception {
        System system = System.deserialize(new JSONObject(resource("supply-get.json")));
        String output = ToNuXmv.transform(system);

        assertValid(output);
        assertTrue(output, output.contains("s-send"));
        assertTrue(output, output.contains("g-recv"));
    }

    @Test
    public void deserializedNamedLocationEnablesThePairing() throws Exception {
        System system = System.deserialize(new JSONObject(resource("location-pair.json")));
        String output = ToNuXmv.transform(system);

        assertValid(output);
        assertLtl(output, "F r-recv", true);
    }

    @Test
    public void deserializedTwoWayMessageSwapWorks() throws Exception {
        System system = System.deserialize(new JSONObject(resource("twoway.json")));
        String output = ToNuXmv.transform(system);

        assertValid(output);
        assertLtl(output, "F (one-loc2 = two & two-loc2 = one)", true);
    }

    @Test
    public void deserializedLocationModelProducesAValidModel() throws Exception {
        System system = System.deserialize(new JSONObject(resource("get-supply.json")));
        String output = ToNuXmv.transform(system);

        assertValid(output);
        assertTrue(output, output.contains("one-myself"));
        assertTrue(output, output.contains("one-splyAtSelf"));
        assertTrue(output, output.contains("one-getAtX"));
    }
}
