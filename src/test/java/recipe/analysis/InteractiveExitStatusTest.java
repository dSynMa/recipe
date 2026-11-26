package recipe.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveExitStatusTest {
    @Test(timeout = 15000)
    public void reportsNaturalExitStatusAndUnfinishedOutput() throws Exception {
        runChecker("exit 7\n", "exited with status 7");
    }

    @Test(timeout = 15000)
    public void closedOutputDoesNotReportAStatusCausedByCleanup() throws Exception {
        runChecker("exec 1>&- 2>&-\nexec sleep 30\n", "closed its output");
    }

    private void runChecker(String ending, String expected) throws Exception {
        Path directory = Files.createTempDirectory("recipe-checker-exit-");
        Process child = null;
        try {
            Path executable = directory.resolve("nuxmv/bin/nuxmv");
            Files.createDirectories(executable.getParent());
            Files.writeString(executable, "#!/bin/sh\nprintf 'nuXmv >'\nread -r command\n"
                    + "printf 'solver stopped mid-response\\n'\n" + ending);
            assertTrue(executable.toFile().setExecutable(true));
            Files.writeString(directory.resolve("model.rcp"), source("supplier-locality"));
            String classpath = Arrays.stream(java.lang.System.getProperty("java.class.path").split(File.pathSeparator))
                    .map(p -> Path.of(p).toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator));
            Path output = directory.resolve("output.txt");
            child = new ProcessBuilder(Path.of(java.lang.System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", classpath, getClass().getName(), expected).directory(directory.toFile())
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            assertTrue("Checker exit left a response waiting", child.waitFor(5, TimeUnit.SECONDS));
            assertEquals(Files.readString(output), 0, child.exitValue());
        } finally {
            if (child != null && child.isAlive()) {
                child.destroyForcibly();
                child.waitFor(2, TimeUnit.SECONDS);
            }
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList()))
                    Files.deleteIfExists(path);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        recipe.lang.System model = recipe.lang.System.parser().end().parse(Files.readString(Path.of("model.rcp"))).get();
        try (var checker = new NuXmvInteraction(model)) {
            IOException failure = assertThrows(IOException.class,
                    () -> checker.execute(s -> s.endsWith("nuXmv >"), "ping"));
            assertTrue(failure.getMessage(), failure.getMessage().contains(args[0]));
            assertTrue(failure.getMessage(), failure.getMessage().contains("solver stopped mid-response"));
            if (args[0].equals("closed its output")) assertFalse(failure.getMessage().contains("status"));
        }
    }
}
