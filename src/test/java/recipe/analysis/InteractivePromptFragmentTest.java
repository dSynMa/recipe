package recipe.analysis;

import java.io.File;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractivePromptFragmentTest {
    @Test(timeout = 15000)
    public void startupAndCommandPromptsCanSpanSeparateReads() throws Exception {
        Path directory = Files.createTempDirectory("recipe-fragmented-prompt-");
        Process child = null;
        try {
            Path executable = directory.resolve("nuxmv/bin/nuxmv");
            Files.createDirectories(executable.getParent());
            Files.writeString(executable, "#!/bin/sh\nprintf 'nuX'\nsleep 0.15\nprintf 'mv >'\n"
                + "while read -r command; do printf '\\ncompleted\\nnuX'; sleep 0.15; printf 'mv >'; done\n");
            assertTrue(executable.toFile().setExecutable(true));
            Files.writeString(directory.resolve("model.rcp"), source("supplier-locality"));
            String classpath = Arrays.stream(java.lang.System.getProperty("java.class.path").split(File.pathSeparator))
                .map(p -> Path.of(p).toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator));
            Path output = directory.resolve("output.txt");
            child = new ProcessBuilder(Path.of(java.lang.System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classpath, getClass().getName()).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            assertTrue("Fragmented nuXmv prompt was never recognised", child.waitFor(5, TimeUnit.SECONDS));
            assertEquals(Files.readString(output), 0, child.exitValue());
        } finally {
            if (child != null && child.isAlive()) {
                child.destroy();
                if (!child.waitFor(2, TimeUnit.SECONDS)) child.destroyForcibly();
            }
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList()))
                    Files.deleteIfExists(path);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        recipe.lang.System model = recipe.lang.System.parser().end()
            .parse(Files.readString(Path.of("model.rcp"))).get();
        NuXmvInteraction checker = new NuXmvInteraction(model);
        try {
            String response = checker.execute(s -> s.endsWith("nuXmv >"), "ping");
            assertTrue(response, response.contains("completed"));
        } finally { checker.stopNuXmvThread(); }
    }
}
