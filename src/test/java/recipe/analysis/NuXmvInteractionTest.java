package recipe.analysis;

import org.junit.BeforeClass;
import org.junit.Test;
import recipe.lang.System;
import recipe.lang.types.Enum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NuXmvInteractionTest {

    static NuXmvInteraction nuXmvInteraction;
    static System system;

    @BeforeClass
    public static void init() throws Exception {
        Enum.clear();
        String script = String.join("\n", Files.readAllLines(Paths.get("./example-current-syntax.txt")));
        System system = System.parser().parse(script).get();
        nuXmvInteraction = new NuXmvInteraction(system);
        nuXmvInteraction.simulation_next("TRUE");
        nuXmvInteraction.simulation_next("TRUE");
    }

    public NuXmvInteractionTest() throws Exception {
    }

    @Test
    public void read() throws InterruptedException, IOException {

    }

    @Test
    public void write() {
    }
}