/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-10.
 */

package app.morphe.extension.youtube.patches.general.offline;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.net.Uri;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Built-in video downloader: quality picker, chunked parallel download of the DASH video and
 * audio streams, MP4 muxing, and saving to {@code Movies/RVX}. Downloads run one at a time,
 * further downloads wait in a queue. The process is kept alive by the hooked
 * {@code OfflineKeepAliveService}.
 */
public final class OfflineDownloader {
    enum State {QUEUED, DOWNLOADING, MERGING, SAVING}

    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final String THUMBNAIL_URL = "https://i.ytimg.com/vi/%s/hqdefault.jpg";

    // measured 16 MiB response throttling; six connections saturate below it.
    private static final long CHUNK_SIZE = 8L * 1024 * 1024;
    private static final int PARALLEL_CONNECTIONS = 6;
    private static final int CHUNK_ATTEMPTS = 3;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final double REQUIRED_SPACE_FACTOR = 2.2;
    private static final long NOTIFICATION_INTERVAL_MS = 1000;

    /** A download in the queue or in progress. */
    static final class Task {
        final String videoId;
        final String title;
        final String qualityLabel;
        final AtomicLong done = new AtomicLong();
        // abort blocking reads and retain displayed progress across client restarts.
        final Set<HttpURLConnection> connections = ConcurrentHashMap.newKeySet();
        final AtomicInteger percent = new AtomicInteger(-1);
        volatile boolean transferStopped;
        volatile long total;
        volatile State state = State.QUEUED;
        volatile boolean cancelled;
        volatile boolean timedOut;
        @Nullable
        volatile Future<?> future;

        OfflineFormats.Resolved resolved;
        OfflineFormats.Stream video;

        Task(OfflineFormats.Resolved resolved, OfflineFormats.Stream video) {
            this.videoId = resolved.videoId();
            this.title = resolved.title();
            this.qualityLabel = qualityLabel(video);
            this.resolved = resolved;
            this.video = video;
        }

        State getState() {
            return state;
        }

        /** @return 0-100 without retreating during fallback, or -1 until a size is known. */
        int getPercent() {
            long totalBytes = total;
            if (totalBytes <= 0) return percent.get();
            int current = (int) Math.min(100, done.get() * 100 / totalBytes);
            return percent.accumulateAndGet(current, Math::max);
        }
    }

    /** HTTP 403: the stream url of this client is not accepted, another client may work. */
    private static final class ForbiddenException extends IOException {
        ForbiddenException() {
            super("HTTP 403");
        }
    }

    private static final Map<String, Task> tasks = new LinkedHashMap<>();
    private static final Set<String> resolving = ConcurrentHashMap.newKeySet();
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private static final AtomicLong lastNotification = new AtomicLong();
    private static final ExecutorService queue = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "rvx-offline-download");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });

    private OfflineDownloader() {
    }

    // region entry point and picker

    /**
     * Entry point of every video download button while the built-in downloader is on.
     */
    public static void download(@Nullable String videoId) {
        Utils.runOnMainThread(() -> downloadOnMainThread(videoId));
    }

    private static void downloadOnMainThread(@Nullable String videoId) {
        if (videoId == null || !VIDEO_ID.matcher(videoId).matches()) {
            Logger.printInfo(() -> "Refusing to download an unusable video id: " + videoId);
            Utils.showToastShort(str("revanced_offline_download_unavailable"));
            return;
        }
        synchronized (tasks) {
            if (tasks.containsKey(videoId) || resolving.contains(videoId)) {
                Utils.showToastShort(str("revanced_offline_already_downloading"));
                return;
            }
        }
        Activity activity = Utils.getActivity();
        if (activity == null || activity.isFinishing()) {
            Logger.printInfo(() -> "No activity to show the quality picker");
            return;
        }

        resolving.add(videoId);
        // Set once: either the user dismissed the sheet while loading, or the formats arrived.
        AtomicBoolean settled = new AtomicBoolean();
        OfflinePickerSheet sheet = new OfflinePickerSheet(activity, videoId, () -> {
            if (settled.compareAndSet(false, true)) resolving.remove(videoId);
        });
        sheet.show();

        Utils.runOnBackgroundThread(() -> {
            OfflineVideo existing = OfflineIndex.get(videoId);
            final boolean alreadySaved = existing != null
                    && OfflineMediaStore.exists(Utils.getContext(), existing.contentUri);
            final OfflineFormats.Resolved resolved = alreadySaved ? null : OfflineFormats.resolve(videoId, 0);
            Utils.runOnMainThread(() -> {
                if (!settled.compareAndSet(false, true)) return; // The user cancelled.
                resolving.remove(videoId);
                if (alreadySaved) {
                    sheet.dismiss();
                    Utils.showToastShort(str("revanced_offline_already_downloaded"));
                } else if (resolved == null) {
                    sheet.dismiss();
                    Utils.showToastLong(str("revanced_offline_no_formats"));
                } else if (activity.isFinishing()) {
                    sheet.dismiss();
                } else {
                    sheet.showFormats(resolved, video -> {
                        sheet.dismiss();
                        enqueue(resolved, video);
                    });
                }
            });
        });
    }

    static String qualityLabel(OfflineFormats.Stream video) {
        return video.height() + "p" + (video.fps() > 30 ? String.valueOf(video.fps()) : "");
    }

    private static void enqueue(OfflineFormats.Resolved resolved, OfflineFormats.Stream video) {
        Task task = new Task(resolved, video);
        synchronized (tasks) {
            if (tasks.containsKey(task.videoId)) {
                Utils.showToastShort(str("revanced_offline_already_downloading"));
                return;
            }
            tasks.put(task.videoId, task);
            task.future = queue.submit(() -> run(task));
        }
        Logger.printInfo(() -> "Queued " + task.videoId + " at " + task.qualityLabel + " " + video.codec()
                + " (itag " + video.itag() + ", audio itag " + resolved.audioFor(video).itag() + ")");
        Utils.showToastShort(str("revanced_offline_download_started"));
        // The user just picked a quality, so the app is in the foreground and may start the service.
        OfflineServiceHooks.startDownloadForeground();
        notifyChanged(true);
    }

    // endregion

    // region queue state

    static boolean hasWork() {
        synchronized (tasks) {
            return !tasks.isEmpty();
        }
    }

    static List<Task> getTasks() {
        synchronized (tasks) {
            return new ArrayList<>(tasks.values());
        }
    }

    static void cancel(String videoId) {
        Task task;
        synchronized (tasks) {
            task = tasks.get(videoId);
        }
        if (task == null) return;
        Logger.printInfo(() -> "Cancelling download " + videoId);
        task.cancelled = true;
        // cancellation must unblock socket reads, not wait for their timeout.
        stopTransfer(task);
        if (task.state == State.QUEUED) {
            Future<?> future = task.future;
            if (future != null) future.cancel(false);
            finish(task);
        }
    }

    static void cancelAll(boolean timedOut) {
        for (Task task : getTasks()) {
            task.timedOut = timedOut;
            cancel(task.videoId);
        }
    }

    static void addListener(Runnable listener) {
        listeners.addIfAbsent(listener);
    }

    static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static void notifyChanged(boolean force) {
        if (!force) {
            long now = System.currentTimeMillis();
            long previous = lastNotification.get();
            if (now - previous < NOTIFICATION_INTERVAL_MS || !lastNotification.compareAndSet(previous, now)) {
                return;
            }
        }
        Utils.runOnMainThread(() -> {
            OfflineServiceHooks.updateDownloadNotification();
            for (Runnable listener : listeners) listener.run();
        });
    }

    static Notification buildProgressNotification(Context context) {
        Task current = null;
        int count;
        synchronized (tasks) {
            count = tasks.size();
            for (Task task : tasks.values()) {
                if (task.state != State.QUEUED) {
                    current = task;
                    break;
                }
            }
            if (current == null && !tasks.isEmpty()) current = tasks.values().iterator().next();
        }
        return OfflineNotifications.buildDownloadProgress(context, current, Math.max(0, count - 1));
    }

    private static void finish(Task task) {
        boolean empty;
        synchronized (tasks) {
            tasks.remove(task.videoId);
            empty = tasks.isEmpty();
        }
        Utils.runOnMainThread(() -> {
            if (empty) OfflineServiceHooks.stopDownloadForeground();
            else OfflineServiceHooks.updateDownloadNotification();
            for (Runnable listener : listeners) listener.run();
        });
    }

    // endregion

    // region download

    private static File workDirectory(Context context) {
        File base = context.getExternalCacheDir();
        if (base == null) base = context.getCacheDir();
        File directory = new File(base, "rvx_offline_tmp");
        //noinspection ResultOfMethodCallIgnored
        directory.mkdirs();
        return directory;
    }

    private static void run(Task task) {
        if (task.cancelled) return;
        final Context context = Utils.getContext();
        final File work = workDirectory(context);
        final File videoFile = new File(work, task.videoId + ".video");
        final File audioFile = new File(work, task.videoId + ".audio");
        try {
            OfflineFormats.Resolved resolved = task.resolved;
            OfflineFormats.Stream video = task.video;
            task.state = State.DOWNLOADING;
            notifyChanged(true);

            while (true) {
                try {
                    downloadStreams(task, resolved, video, work, videoFile, audioFile);
                    break;
                } catch (ForbiddenException ex) {
                    // The urls of this client are refused. Start over with the next client,
                    // so the two files never mix chunks of different clients.
                    int next = resolved.clientIndex() + 1;
                    OfflineFormats.Resolved other = next < OfflineFormats.clientCount()
                            ? OfflineFormats.resolve(task.videoId, next)
                            : null;
                    OfflineFormats.Stream match = null;
                    if (other != null) {
                        for (OfflineFormats.Stream candidate : other.videos()) {
                            if (candidate.key().equals(video.key())) {
                                match = candidate;
                                break;
                            }
                        }
                    }
                    if (match == null) throw ex;
                    final int clientIndex = other.clientIndex();
                    Logger.printInfo(() -> "HTTP 403 for " + task.videoId + ", retrying with client #" + clientIndex);
                    resolved = other;
                    video = match;
                    task.resolved = other;
                    task.video = match;
                }
            }

            checkCancelled(task);
            task.state = State.MERGING;
            notifyChanged(true);
            Logger.printInfo(() -> "Merging " + task.videoId);
            // merge straight into the pending MediaStore item.
            // .webm for VP9 downloads.
            final String displayName = displayName(resolved.title(), task.videoId, video.fileExtension());
            final long size;
            final Uri uri;
            OfflineMediaStore.Pending pending = OfflineMediaStore.create(context, displayName);
            try {
                if (video.isWebm()) {
                    Mp4Merger.mergeWebm(videoFile, audioFile, pending.fileDescriptor(), () -> task.cancelled);
                } else {
                    Mp4Merger.merge(videoFile, audioFile, pending.fileDescriptor(), () -> task.cancelled);
                }
                deleteQuietly(videoFile);
                deleteQuietly(audioFile);

                checkCancelled(task);
                task.state = State.SAVING;
                notifyChanged(true);
                size = pending.length();
                uri = pending.commit();
            } catch (Throwable ex) {
                pending.abort();
                throw ex;
            }
            Logger.printInfo(() -> "Saved " + task.videoId + " to " + uri + " (" + size + " bytes)");

            String thumbnail = saveThumbnail(context, task.videoId, resolved.thumbnailUrl());
            OfflineIndex.put(new OfflineVideo(task.videoId, resolved.title(), resolved.author(),
                    resolved.channelId(), resolved.lengthMs(), uri.toString(), displayName, size,
                    task.qualityLabel, video.codec(), System.currentTimeMillis(), thumbnail, 0));

            OfflineNotifications.showDownloadFinished(context, task.videoId, task.title);
            Utils.showToastShort(str("revanced_offline_download_complete_toast", task.title));
        } catch (Throwable ex) {
            if (task.cancelled) {
                Logger.printInfo(() -> "Download cancelled: " + task.videoId);
                if (task.timedOut) {
                    OfflineNotifications.showDownloadFailed(context, task.videoId, task.title,
                            str("revanced_offline_error_time_limit"));
                }
            } else {
                Logger.printException(() -> "Download failed: " + task.videoId, ex);
                String reason = ex instanceof ForbiddenException
                        ? str("revanced_offline_error_forbidden")
                        : ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                OfflineNotifications.showDownloadFailed(context, task.videoId, task.title, reason);
                Utils.showToastLong(str("revanced_offline_download_failed", reason));
            }
        } finally {
            deleteQuietly(videoFile);
            deleteQuietly(audioFile);
            finish(task);
        }
    }

    // share workers across both streams and reuse complete bounded responses.
    private static void downloadStreams(Task task, OfflineFormats.Resolved resolved, OfflineFormats.Stream video,
                                        File work, File videoFile, File audioFile) throws IOException {
        checkCancelled(task);
        task.transferStopped = false;
        final String userAgent = resolved.userAgent();
        final OfflineFormats.Stream audio = resolved.audioFor(video);
        long videoLength = video.contentLength() > 0 ? video.contentLength() : probeLength(task, video.url(), userAgent);
        long audioLength = audio.contentLength() > 0 ? audio.contentLength() : probeLength(task, audio.url(), userAgent);
        task.done.set(0);
        long knownBytes = Math.max(0, videoLength) + Math.max(0, audioLength);
        task.total = videoLength > 0 && audioLength > 0 ? knownBytes : 0;

        long required = (long) (knownBytes * REQUIRED_SPACE_FACTOR);
        if (knownBytes > 0 && work.getUsableSpace() < required) {
            throw new IOException(str("revanced_offline_error_space",
                    Formatter.formatShortFileSize(Utils.getContext(), required)));
        }

        deleteQuietly(videoFile);
        deleteQuietly(audioFile);
        Logger.printInfo(() -> "Downloading " + task.videoId + ": video itag " + video.itag()
                + " (" + videoLength + " bytes), audio itag " + audio.itag() + " (" + audioLength + " bytes)");
        DownloadSource[] sources = {
                new DownloadSource(video.url(), videoLength, videoFile),
                new DownloadSource(audio.url(), audioLength, audioFile)
        };
        RangeQueue ranges = new RangeQueue(sources);
        int workers = (int) Math.min(PARALLEL_CONNECTIONS,
                (Math.max(0, videoLength) + CHUNK_SIZE - 1) / CHUNK_SIZE
                        + (Math.max(0, audioLength) + CHUNK_SIZE - 1) / CHUNK_SIZE
                        + (videoLength <= 0 ? 1 : 0) + (audioLength <= 0 ? 1 : 0));
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        ExecutorCompletionService<Void> completed = new ExecutorCompletionService<>(pool);
        try {
            for (int i = 0; i < workers; i++) {
                completed.submit(() -> {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    RandomAccessFile[] outputs = new RandomAccessFile[sources.length];
                    try {
                        DownloadRange range;
                        while ((range = ranges.take()) != null) {
                            checkTransfer(task);
                            int index = range.sourceIndex;
                            DownloadSource source = sources[index];
                            if (source.length <= 0) {
                                downloadWhole(task, source, userAgent, buffer);
                            } else {
                                RandomAccessFile output = outputs[index];
                                if (output == null) outputs[index] = output = new RandomAccessFile(source.file, "rw");
                                downloadChunk(task, source.url, range.start, range.end, output, userAgent, buffer);
                            }
                        }
                        return null;
                    } finally {
                        for (RandomAccessFile output : outputs) {
                            if (output != null) output.close();
                        }
                    }
                });
            }
            for (int i = 0; i < workers; i++) {
                try {
                    completed.take().get();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof IOException io) throw io;
                    if (cause instanceof RuntimeException runtime) throw runtime;
                    throw new IOException(cause);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", ex);
                }
            }
            checkTransfer(task);
            task.total = task.done.get(); // Unknown-length streams now have an exact total.
            notifyChanged(true);
        } finally {
            stopTransfer(task);
            pool.shutdownNow();
            // No worker may still write when fallback truncates files or cancellation deletes them.
            boolean interrupted = false;
            while (!pool.isTerminated()) {
                try {
                    pool.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static final class DownloadSource {
        final String url;
        final long length;
        final File file;
        long next;
        boolean claimed;

        DownloadSource(String url, long length, File file) throws IOException {
            this.url = url;
            this.length = length;
            this.file = file;
            if (length > 0) {
                try (RandomAccessFile output = new RandomAccessFile(file, "rw")) {
                    output.setLength(length);
                }
            }
        }
    }

    private record DownloadRange(int sourceIndex, long start, long end) {
    }

    /** Assign bounded ranges from the largest unscheduled remainder of either stream. */
    private static final class RangeQueue {
        final DownloadSource[] sources;

        RangeQueue(DownloadSource[] sources) {
            this.sources = sources;
        }

        synchronized DownloadRange take() {
            int largest = -1;
            long remaining = 0;
            for (int i = 0; i < sources.length; i++) {
                DownloadSource source = sources[i];
                if (source.length <= 0 && !source.claimed) {
                    source.claimed = true;
                    return new DownloadRange(i, 0, -1);
                }
                long candidate = source.length - source.next;
                if (candidate > remaining) {
                    largest = i;
                    remaining = candidate;
                }
            }
            if (largest < 0) return null;
            DownloadSource source = sources[largest];
            long size = Math.min(remaining, CHUNK_SIZE);
            long start = source.next;
            source.next += size;
            return new DownloadRange(largest, start, source.next - 1);
        }
    }

    private static void downloadChunk(Task task, String url, long start, long end, RandomAccessFile output,
                                      @Nullable String userAgent, byte[] buffer) throws IOException {
        long position = start;
        for (int attempt = 1; ; attempt++) {
            checkTransfer(task);
            HttpURLConnection connection = openTransfer(task, url, position, end, userAgent);
            boolean reusable = false;
            try {
                int code = connection.getResponseCode();
                if (code == HttpURLConnection.HTTP_FORBIDDEN) throw new ForbiddenException();
                if (code != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException("Range request returned HTTP " + code);
                }
                String expectedRange = "bytes " + position + "-" + end + "/";
                String actualRange = connection.getHeaderField("Content-Range");
                if (actualRange == null || !actualRange.startsWith(expectedRange)) {
                    throw new IOException("Unexpected Content-Range: " + actualRange);
                }
                try (InputStream input = connection.getInputStream()) {
                    output.seek(position);
                    while (position <= end) {
                        int read = readBuffer(task, input, buffer,
                                (int) Math.min(buffer.length, end - position + 1));
                        if (read == 0) throw new IOException("Incomplete range " + position + "-" + end);
                        output.write(buffer, 0, read);
                        position += read;
                        task.done.addAndGet(read);
                        notifyChanged(false);
                    }
                    // Consume EOF before closing so Android can return the socket to its keep-alive pool.
                    if (input.read() != -1) throw new IOException("Oversized range response");
                    reusable = true;
                }
                return;
            } catch (ForbiddenException | CancellationException ex) {
                throw ex;
            } catch (IOException ex) {
                checkTransfer(task);
                if (attempt >= CHUNK_ATTEMPTS || position > end) throw ex;
                final int failedAttempt = attempt;
                Logger.printInfo(() -> "Chunk " + start + "-" + end + " failed (attempt " + failedAttempt + "), resuming", ex);
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", interrupted);
                }
            } finally {
                releaseTransfer(task, connection, reusable);
            }
        }
    }

    /** Fallback for streams whose size the server does not reveal. */
    private static void downloadWhole(Task task, DownloadSource source, @Nullable String userAgent,
                                      byte[] buffer) throws IOException {
        HttpURLConnection connection = openTransfer(task, source.url, -1, -1, userAgent);
        boolean reusable = false;
        try {
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_FORBIDDEN) throw new ForbiddenException();
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code);
            try (InputStream input = connection.getInputStream();
                 OutputStream output = new FileOutputStream(source.file)) {
                int read;
                while ((read = readBuffer(task, input, buffer, buffer.length)) != 0) {
                    output.write(buffer, 0, read);
                    task.done.addAndGet(read);
                    notifyChanged(false);
                }
                reusable = true;
            }
        } finally {
            releaseTransfer(task, connection, reusable);
        }
    }

    // coalesce Android's small socket reads into one cache write without copying.
    private static int readBuffer(Task task, InputStream input, byte[] buffer, int length) throws IOException {
        int filled = 0;
        while (filled < length) {
            checkTransfer(task);
            int read = input.read(buffer, filled, length - filled);
            if (read < 0) break;
            filled += read;
        }
        return filled;
    }

    private static long probeLength(Task task, String url, @Nullable String userAgent) throws IOException {
        HttpURLConnection connection = openTransfer(task, url, 0, 0, userAgent);
        try {
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_FORBIDDEN) throw new ForbiddenException();
            String range = connection.getHeaderField("Content-Range");
            if (code == HttpURLConnection.HTTP_PARTIAL && range != null) {
                int slash = range.lastIndexOf('/');
                if (slash >= 0) {
                    try {
                        return Long.parseLong(range.substring(slash + 1).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return -1;
        } finally {
            releaseTransfer(task, connection, false);
        }
    }

    private static HttpURLConnection openTransfer(Task task, String url, long start, long end,
                                                   @Nullable String userAgent) throws IOException {
        HttpURLConnection connection = open(url, start, end, userAgent);
        synchronized (task.connections) {
            try {
                checkTransfer(task);
                task.connections.add(connection);
            } catch (CancellationException ex) {
                connection.disconnect();
                throw ex;
            }
        }
        return connection;
    }

    private static void releaseTransfer(Task task, HttpURLConnection connection, boolean reusable) {
        if (!reusable) connection.disconnect();
        task.connections.remove(connection);
    }

    private static void stopTransfer(Task task) {
        synchronized (task.connections) {
            task.transferStopped = true;
            for (HttpURLConnection connection : task.connections) connection.disconnect();
        }
    }

    private static void checkTransfer(Task task) {
        checkCancelled(task);
        if (task.transferStopped || Thread.currentThread().isInterrupted()) throw new CancellationException();
    }

    private static HttpURLConnection open(String url, long start, long end, @Nullable String userAgent) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Encoding", "identity");
        if (userAgent != null) connection.setRequestProperty("User-Agent", userAgent);
        if (start >= 0) connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
        return connection;
    }

    private static void checkCancelled(Task task) {
        if (task.cancelled) throw new CancellationException();
    }

    // endregion

    // region files

    static String displayName(String title, String videoId, String extension) {
        String clean = title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").replaceAll("\\s+", " ").trim();
        if (clean.startsWith(".")) clean = "_" + clean.substring(1);
        if (clean.length() > 120) clean = clean.substring(0, 120).trim();
        if (clean.isEmpty()) clean = "video";
        return String.format(Locale.US, "%s [%s].%s", clean, videoId, extension);
    }

    @NonNull
    private static String saveThumbnail(Context context, String videoId, @Nullable String preferredUrl) {
        File destination = OfflineIndex.getThumbnailFile(context, videoId);
        String[] urls = {preferredUrl, String.format(Locale.US, THUMBNAIL_URL, videoId)};
        for (String url : urls) {
            if (url == null || url.isEmpty()) continue;
            HttpURLConnection connection = null;
            try {
                connection = open(url, -1, -1, null);
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) continue;
                try (InputStream input = connection.getInputStream();
                     OutputStream output = new FileOutputStream(destination)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                }
                return destination.getAbsolutePath();
            } catch (Exception ex) {
                Logger.printDebug(() -> "Could not fetch thumbnail " + url, ex);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return "";
    }

    private static void deleteQuietly(File file) {
        if (file.exists() && !file.delete()) {
            Logger.printDebug(() -> "Could not delete " + file);
        }
    }

    // endregion
}
