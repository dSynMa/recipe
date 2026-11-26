package recipe.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import recipe.lang.System;

/** The build and its live checker processes share one immutable translation. */
public final class NuXmvModelFile implements AutoCloseable {
    final System system;
    final Path path;
    private int references = 1;
    private boolean closed;

    public NuXmvModelFile(System system) throws Exception {
        this.system = system;
        String translation = ToNuXmv.transform(system);
        path = Files.createTempFile("rcheck-interactive-", ".smv");
        path.toFile().deleteOnExit();
        try {
            Files.writeString(path, translation);
        } catch (IOException error) {
            Files.deleteIfExists(path);
            throw error;
        }
    }

    public NuXmvInteraction openChecker() throws IOException {
        synchronized (this) {
            if (closed) throw new IOException("Model has been replaced");
            references++;
        }
        // Starting a process can block. Each session owns its reference from here.
        return new NuXmvInteraction(this);
    }

    synchronized void release() throws IOException {
        if (--references == 0) Files.deleteIfExists(path);
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            release();
        }
    }
}
