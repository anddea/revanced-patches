package app.morphe.extension.youtube.utils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Bounds time to the first complete API response and inactivity between streamed responses.
 * HTTP headers, SSE comments and incomplete events do not extend the deadline. Disconnecting
 * also releases a blocking network read when the request's worker is interrupted by cancellation.
 */
final class GeminiRequestWatchdog implements AutoCloseable {
    private final BooleanSupplier cancelled;
    private final Runnable disconnect;
    private final long idleTimeoutNanos;
    private final ScheduledFuture<?> task;
    private long deadlineNanos;
    private boolean closed;
    private volatile boolean timedOut;

    GeminiRequestWatchdog(ScheduledExecutorService scheduler, Runnable disconnect, BooleanSupplier cancelled,
                          long firstResponseTimeoutMs, long idleTimeoutMs) {
        this.disconnect = disconnect;
        this.cancelled = cancelled;
        idleTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(idleTimeoutMs);
        deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(firstResponseTimeoutMs);
        task = scheduler.scheduleWithFixedDelay(this::check, 100, 100, TimeUnit.MILLISECONDS);
    }

    synchronized void onResponse() {
        if (!closed && !timedOut) {
            deadlineNanos = System.nanoTime() + idleTimeoutNanos;
        }
    }

    boolean hasTimedOut() {
        return timedOut;
    }

    boolean isCancelled() {
        return cancelled.getAsBoolean();
    }

    private synchronized void check() {
        if (closed) return;
        boolean wasCancelled = cancelled.getAsBoolean();
        if (wasCancelled || System.nanoTime() - deadlineNanos >= 0) {
            timedOut = !wasCancelled;
            closed = true;
            disconnect.run();
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        task.cancel(false);
    }
}
