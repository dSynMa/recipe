package recipe.analysis;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.petitparser.context.Result;
import recipe.lang.System;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

final class TranslationTestModels {
    static String source(String name) throws Exception {
        try (InputStream input = TranslationTestModels.class.getResourceAsStream(
                "/recipe/analysis/adversarial/" + name + ".rcp")) {
            assertNotNull(name, input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static System parse(String source) throws Exception {
        Result result = System.parser().end().parse(source);
        assertTrue(result.toString(), result.isSuccess());
        return result.get();
    }

    static String definition(String output, String name) {
        int start = output.indexOf(name + " :=");
        assertTrue("Missing definition: " + name, start >= 0);
        return output.substring(start, output.indexOf(';', start));
    }

    static void assertCtl(String output, String formula, boolean expected) throws Exception {
        Path executable = Path.of("nuxmv/bin/nuxmv").toAbsolutePath();
        assumeTrue("nuXmv executable is required for semantic checks", Files.isExecutable(executable));
        Path directory = Files.createTempDirectory("recipe-ctl-");
        Path model = directory.resolve("model.smv");
        Path commands = directory.resolve("commands.txt");
        Path log = directory.resolve("output.txt");
        Process process = null;
        try {
            Files.writeString(model, output);
            Files.writeString(commands, "go\ncheck_ctlspec -p \"" + formula + "\"\nquit\n");
            process = new ProcessBuilder(executable.toString(), "-source", commands.toString(), model.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            assertTrue("nuXmv timed out", process.waitFor(20, TimeUnit.SECONDS));
            String result = Files.readString(log);
            assertEquals(result, 0, process.exitValue());
            assertTrue(result, result.lines().anyMatch(line ->
                line.startsWith("-- specification ") && line.endsWith(" is " + expected)));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
            Files.deleteIfExists(log);
            Files.deleteIfExists(commands);
            Files.deleteIfExists(model);
            Files.deleteIfExists(directory);
        }
    }
}
