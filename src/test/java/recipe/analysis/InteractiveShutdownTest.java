package recipe.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import static recipe.analysis.TranslationTestModels.*;

public class InteractiveShutdownTest {
    @Test(timeout = 15000)
    public void stopTerminatesWorkerAndChildAndIsRepeatable() throws Exception {
        Set<Long> before = ProcessHandle.current().children().map(ProcessHandle::pid).collect(Collectors.toSet());
        NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")));
        List<ProcessHandle> children = ProcessHandle.current().children().filter(p -> !before.contains(p.pid())).collect(Collectors.toList());
        try {
            assertTrue(checker.initialise(true).getLeft());
            assertFalse(children.isEmpty());
            checker.stopNuXmvThread();
            assertFalse(checker.process.isAlive());
            assertTrue(children.stream().noneMatch(ProcessHandle::isAlive));
            assertFalse(Files.exists(checker.path));
            checker.stopNuXmvThread();
            assertThrows(IOException.class, () -> checker.execute(s -> true, "show_vars"));
        } finally {
            children.forEach(ProcessHandle::destroyForcibly);
            checker.stopNuXmvThread();
        }
    }

    @Test(timeout = 15000)
    public void stopReleasesAnOutstandingResponseWait() throws Exception {
        NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch waiting = new CountDownLatch(1);
        try {
            assertTrue(checker.initialise(true).getLeft());
            Future<String> pending = executor.submit(() -> checker.execute(output -> {
                if (output.contains("nuXmv >")) waiting.countDown();
                return false;
            }, "show_vars"));
            assertTrue(waiting.await(3, TimeUnit.SECONDS));
            checker.stopNuXmvThread();
            ExecutionException error = assertThrows(ExecutionException.class, () -> pending.get(2, TimeUnit.SECONDS));
            assertTrue(error.getCause().toString(), error.getCause() instanceof IOException);
        } finally {
            checker.stopNuXmvThread();
            executor.shutdownNow();
        }
    }

    @Test(timeout = 15000)
    public void alreadyInterruptedCallerStillWaitsForShutdown() throws Exception {
        NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")));
        try {
            Thread.currentThread().interrupt();
            checker.stopNuXmvThread();
            assertTrue(Thread.currentThread().isInterrupted());
            assertFalse(checker.process.isAlive());
            assertFalse(Files.exists(checker.path));
        } finally {
            Thread.interrupted();
            checker.stopNuXmvThread();
        }
    }

    @Test(timeout = 15000)
    public void cancelledStartupClosesTheUnreturnedSession() throws Exception {
        var model = parse(source("supplier-locality"));
        Set<Long> before = ProcessHandle.current().children().map(ProcessHandle::pid).collect(Collectors.toSet());
        var createdPath = new java.util.concurrent.atomic.AtomicReference<java.nio.file.Path>();
        try {
            Thread.currentThread().interrupt();
            assertThrows(IOException.class, () -> new NuXmvInteraction(model) {
                @Override public Thread startNuXmvThread() throws IOException {
                    createdPath.set(this.path);
                    return super.startNuXmvThread();
                }
            });
            assertTrue(Thread.currentThread().isInterrupted());
            assertNotNull(createdPath.get());
            assertFalse("Cancelled construction retained its temporary model", Files.exists(createdPath.get()));
            assertTrue(ProcessHandle.current().children().noneMatch(p -> !before.contains(p.pid()) && p.isAlive()));
        } finally {
            Thread.interrupted();
            ProcessHandle.current().children().filter(p -> !before.contains(p.pid())).forEach(ProcessHandle::destroyForcibly);
            if (createdPath.get() != null) Files.deleteIfExists(createdPath.get());
        }
    }

    @Test(timeout = 20000)
    public void replacementCheckerStillReadsItsOwnInitialPrompt() throws Exception {
        for (int i = 0; i < 3; i++) {
            NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")));
            try {
                var result = checker.modelCheck("sender-sent", false, -1);
                assertTrue(result.getRight(), result.getLeft());
                assertTrue(result.getRight(), result.getRight().contains("is false"));
            } finally { checker.stopNuXmvThread(); }
        }
    }
    @Test(timeout = 10000)
    public void checkerExitReleasesAResponseWait() throws Exception {
        NuXmvInteraction checker = new NuXmvInteraction(parse(source("supplier-locality")));
        try {
            IOException failure = assertThrows(IOException.class,
                () -> checker.execute(output -> output.contains("nuXmv >"), "quit"));
            assertTrue(failure.getMessage(), failure.getMessage().contains("exited with status 0"));
            assertThrows(IOException.class, () -> checker.modelCheck("!sender-sent", false, 0));
        } finally {
            checker.stopNuXmvThread();
        }
    }
}
