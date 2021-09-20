package recipe.analysis;

import org.junit.BeforeClass;
import org.junit.Test;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import recipe.lang.System;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.types.Enum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class NuXmvSimulationTest {

    static NuXmvSimulation nuXmvSimulation;
    static System system;

    @BeforeClass
    public static void init() throws Exception {
        Enum.clear();
        String script = String.join("\n", Files.readAllLines(Paths.get("./example-current-syntax.txt")));
        String nuxmvScript = ToNuXmv.transform(System.parser().parse(script).get());
        nuXmvSimulation = new NuXmvSimulation(nuxmvScript);
        nuXmvSimulation.initialise();
        nuXmvSimulation.simulation_next("TRUE");
        nuXmvSimulation.simulation_next("TRUE");
    }

    public NuXmvSimulationTest() throws Exception {
    }

    @Test
    public void read() throws InterruptedException, IOException {

    }

    @Test
    public void write() {
    }
}