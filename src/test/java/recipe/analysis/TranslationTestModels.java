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
        String result = runNuXmv(output, "go\ncheck_ctlspec -p \"" + formula + "\"\nquit\n");
        assertTrue(result, result.lines().anyMatch(line ->
            line.startsWith("-- specification ") && line.endsWith(" is " + expected)));
    }

    static void assertLtl(String output, String formula, boolean expected) throws Exception {
        String result = runNuXmv(output, "go\ncheck_ltlspec -p \"" + formula + "\"\nquit\n");
        assertTrue(result, result.lines().anyMatch(line ->
            line.startsWith("-- specification ") && line.endsWith(" is " + expected)));
    }

    static void assertValid(String output) throws Exception {
        String result = runNuXmv(output, "read_model\nflatten_hierarchy\nwrite_flat_model\nquit\n");
        assertTrue(result, result.contains("MODULE main"));
    }

    static void assertSmtInvariant(String output, String formula) throws Exception {
        String result = runNuXmv(output, "go_msat\ncheck_invar_ic3 -p \"" + formula + "\"\nquit\n");
        assertTrue(result, result.lines().anyMatch(line ->
            line.startsWith("-- invariant ") && line.endsWith(" is true")));
    }

    static String runNuXmv(String output, String script) throws Exception {
        Path executable = Path.of("nuxmv/bin/nuxmv").toAbsolutePath();
        assumeTrue("nuXmv executable is required for semantic checks", Files.isExecutable(executable));
        Path directory = Files.createTempDirectory("recipe-ctl-");
        Path model = directory.resolve("model.smv");
        Path commands = directory.resolve("commands.txt");
        Path log = directory.resolve("output.txt");
        Process process = null;
        try {
            Files.writeString(model, output);
            Files.writeString(commands, script);
            process = new ProcessBuilder(executable.toString(), "-source", commands.toString(), model.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            process.getOutputStream().close();
            assertTrue("nuXmv timed out", process.waitFor(20, TimeUnit.SECONDS));
            String result = Files.readString(log);
            assertEquals(result, 0, process.exitValue());
            assertFalse(result, result.contains("syntax error") || result.contains("aborting"));
            return result;
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
