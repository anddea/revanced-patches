package app.morphe.extension.youtube.utils;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class GeminiRequestWatchdogTest {
    @Test
    public void silentRequestExpiresBeforeLongIdleAllowance() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch disconnected = new CountDownLatch(1);
        try (GeminiRequestWatchdog watchdog = new GeminiRequestWatchdog(
                scheduler, disconnected::countDown, () -> false, 100, 10000)) {
            assertTrue(disconnected.await(2, TimeUnit.SECONDS));
            assertTrue(watchdog.hasTimedOut());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void responsesExtendDeadlineButStalledStreamStillExpires() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch disconnected = new CountDownLatch(1);
        try (GeminiRequestWatchdog watchdog = new GeminiRequestWatchdog(
                scheduler, disconnected::countDown, () -> false, 100, 500)) {
            watchdog.onResponse();
            for (int i = 0; i < 4; i++) {
                assertFalse(disconnected.await(200, TimeUnit.MILLISECONDS));
                watchdog.onResponse();
            }
            assertTrue(disconnected.await(2, TimeUnit.SECONDS));
            assertTrue(watchdog.hasTimedOut());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void cancellationDisconnectsWithoutWaitingForTimeout() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch disconnected = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        AtomicBoolean timedOut = new AtomicBoolean(true);
        Thread worker = new Thread(() -> {
            try (GeminiRequestWatchdog watchdog = new GeminiRequestWatchdog(
                    scheduler, disconnected::countDown, Thread.currentThread()::isInterrupted, 10000, 10000)) {
                ready.countDown();
                // Simulate a network operation that does not immediately respond to interruption.
                while (releaseWorker.getCount() != 0) Thread.yield();
                timedOut.set(watchdog.hasTimedOut());
            }
        });
        try {
            worker.start();
            assertTrue(ready.await(2, TimeUnit.SECONDS));
            worker.interrupt();
            assertTrue(disconnected.await(2, TimeUnit.SECONDS));
        } finally {
            releaseWorker.countDown();
            worker.join(2000);
            scheduler.shutdownNow();
        }
        assertFalse(worker.isAlive());
        assertFalse(timedOut.get());
    }

    @Test
    public void cancellationSurvivesClearedThreadInterruptFlag() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch disconnected = new CountDownLatch(1);
        AtomicBoolean cancelled = new AtomicBoolean();
        try (GeminiRequestWatchdog watchdog = new GeminiRequestWatchdog(
                scheduler, disconnected::countDown, cancelled::get, 10000, 10000)) {
            cancelled.set(true);
            assertFalse(Thread.currentThread().isInterrupted());
            assertTrue(disconnected.await(2, TimeUnit.SECONDS));
            assertFalse(watchdog.hasTimedOut());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void completedRequestIsNeverDisconnectedByWatchdog() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch disconnected = new CountDownLatch(1);
        try {
            GeminiRequestWatchdog watchdog = new GeminiRequestWatchdog(
                    scheduler, disconnected::countDown, () -> false, 100, 100);
            watchdog.close();
            assertFalse(disconnected.await(300, TimeUnit.MILLISECONDS));
        } finally {
            scheduler.shutdownNow();
        }
    }
}
