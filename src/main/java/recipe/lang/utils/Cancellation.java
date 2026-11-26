package recipe.lang.utils;

import java.util.concurrent.CancellationException;

public final class Cancellation {
    private Cancellation() {}

    public static void checkCancellation() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Operation cancelled");
        }
    }
}
