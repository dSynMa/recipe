package recipe.analysis;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import flak.Request;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class ServerCheckCancellationTest {
    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @Test(timeout = 30000)
    public void parallelBmcAndIc3WorkersAreCancelledAndReaped() throws Exception {
        Path directory = Files.createTempDirectory("parallel-ui-checks-");
        try {
            Path executable = directory.resolve("nuxmv/bin/nuxmv");
            Files.createDirectories(executable.getParent());
            // A checker that initializes normally and then waits indefinitely for a check.
            Files.writeString(executable, "#!/bin/sh\nprintf 'nuXmv >'\n"
                + "while read -r command; do\ncase \"$command\" in\n"
                + "go*) printf 'nuXmv >';;\n"
                + "*) echo \"$2\" > pending-$$; mv pending-$$ started-$$; read -r ignored;;\nesac\ndone\n");
            assertTrue(executable.toFile().setExecutable(true));
            Files.writeString(directory.resolve("model.rcp"), source("observation-forall-constant")
                + "\nSPEC F receiver-received;\n");
            String classpath = Arrays.stream(java.lang.System.getProperty("java.class.path").split(File.pathSeparator))
                .map(p -> Path.of(p).toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator));
            for (String mode : List.of("BMC", "IC3")) {
                Path log = directory.resolve(mode + ".log");
                Process child = new ProcessBuilder(Path.of(java.lang.System.getProperty("java.home"), "bin", "java").toString(),
                    "-XX:ActiveProcessorCount=2", "-cp", classpath, getClass().getName(), mode)
                    .directory(directory.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
                try {
                    assertTrue("Parallel cancellation timed out", child.waitFor(10, TimeUnit.SECONDS));
                    assertEquals(Files.readString(log), 0, child.exitValue());
                } finally {
                    child.destroyForcibly();
                }
            }
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) Files.deleteIfExists(path);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName("Server");
        Object server = type.getDeclaredConstructor().newInstance();
        var model = recipe.lang.System.parser().end().parse(Files.readString(Path.of("model.rcp"))).get();
        field(type, "system").set(server, model);
        Class<?> kind = Class.forName("Server$MCType");
        Object config = Class.forName("Server$MCConfig").getConstructor(kind, int.class)
            .newInstance(java.lang.Enum.valueOf((Class) kind, args[0]), -1);
        field(type, "mcConfig").set(server, config);
        ExecutorService requests = Executors.newFixedThreadPool(3);
        try {
            Future<?> first = requests.submit(() -> type.getMethod("modelCheck", Request.class, String.class).invoke(server, null, "0"));
            Future<?> second = requests.submit(() -> type.getMethod("modelCheck", Request.class, String.class).invoke(server, null, "1"));
            long deadline = java.lang.System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
            List<ProcessHandle> checkers;
            while (true) {
                checkers = ProcessHandle.current().children().collect(Collectors.toList());
                if (checkers.size() == 2 && checkers.stream().allMatch(p -> Files.exists(Path.of("started-" + p.pid())))) break;
                assertTrue("Both checks must start concurrently", java.lang.System.nanoTime() < deadline);
                Thread.sleep(10);
            }
            Path sharedModel = Path.of(Files.readString(Path.of("started-" + checkers.get(0).pid())).trim());
            assertTrue(Files.exists(sharedModel));
            assertEquals(sharedModel.toString(), Files.readString(Path.of("started-" + checkers.get(1).pid())).trim());

            // With both pool threads busy, the third request must be cancelled in the queue.
            Future<?> queued = requests.submit(() -> type.getMethod("modelCheck", Request.class, String.class).invoke(server, null, "0"));
            var workers = (java.util.Set<?>) field(type, "modelCheckWorkers").get(server);
            long queueDeadline = java.lang.System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (workers.size() != 3) {
                assertTrue("Third check must be registered", java.lang.System.nanoTime() < queueDeadline);
                Thread.sleep(10);
            }
            type.getMethod("stopModelChecking", Request.class).invoke(server, new Object[]{null});
            assertTrue(first.get(2, TimeUnit.SECONDS).toString().contains("cancelled"));
            assertTrue(second.get(2, TimeUnit.SECONDS).toString().contains("cancelled"));
            assertTrue(queued.get(2, TimeUnit.SECONDS).toString().contains("cancelled"));
            for (ProcessHandle checker : checkers) checker.onExit().get(3, TimeUnit.SECONDS);
            assertTrue(workers.isEmpty());
            assertTrue(Files.exists(sharedModel));
        } finally {
            ((ExecutorService) field(type, "service").get(server)).shutdownNow();
            requests.shutdownNow();
        }
    }
}
