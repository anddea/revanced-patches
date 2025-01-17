package app.revanced.extension.music.patches.misc.requests;

import android.annotation.SuppressLint;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import app.revanced.extension.shared.requests.Requester;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

public class PipedRequester {
    private static final long MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 4 * 1000; // 4 seconds

    @GuardedBy("itself")
    private static final Map<String, PipedRequester> cache = new LinkedHashMap<>() {
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 10;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };

    @SuppressLint("ObsoleteSdkInt")
    public static void fetchRequestIfNeeded(@NonNull String videoId, @NonNull String playlistId, final int playlistIndex) {
        synchronized (cache) {
            if (!cache.containsKey(videoId)) {
                PipedRequester pipedRequester = new PipedRequester(videoId, playlistId, playlistIndex);
                cache.put(videoId, pipedRequester);
            }
        }
    }

    @Nullable
    public static PipedRequester getRequestForVideoId(@Nullable String videoId) {
        synchronized (cache) {
            return cache.get(videoId);
        }
    }

    /**
     * TCP timeout
     */
    private static final int TIMEOUT_TCP_DEFAULT_MILLISECONDS = 2 * 1000; // 2 seconds

    /**
     * HTTP response timeout
     */
    private static final int TIMEOUT_HTTP_DEFAULT_MILLISECONDS = 4 * 1000; // 4 seconds

    @Nullable
    private static JSONObject send(@NonNull String videoId, @NonNull String playlistId, final int playlistIndex) {
        final long startTime = System.currentTimeMillis();
        Logger.printDebug(() -> "Fetching piped instances (videoId: '" + videoId +
                "', playlistId: '" + playlistId + "', playlistIndex: '" + playlistIndex + "')");

        try {
            HttpURLConnection connection = PipedRoutes.getPlaylistConnectionFromRoute(playlistId);
            connection.setConnectTimeout(TIMEOUT_TCP_DEFAULT_MILLISECONDS);
            connection.setReadTimeout(TIMEOUT_HTTP_DEFAULT_MILLISECONDS);

            final int responseCode = connection.getResponseCode();
            if (responseCode == 200) return Requester.parseJSONObject(connection);

            handleConnectionError("API not available: " + responseCode);
        } catch (SocketTimeoutException ex) {
            handleConnectionError("Connection timeout", ex);
        } catch (IOException ex) {
            handleConnectionError("Network error", ex);
        } catch (Exception ex) {
            Logger.printException(() -> "send failed", ex);
        } finally {
            Logger.printDebug(() -> "playlist: " + playlistId + " took: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return null;
    }

    @Nullable
    private static String fetch(@NonNull String videoId, @NonNull String playlistId, final int playlistIndex) {
        final JSONObject playlistJson = send(videoId, playlistId, playlistIndex);
        if (playlistJson != null) {
            try {
                final String songId = playlistJson.getJSONArray("relatedStreams")
                        .getJSONObject(playlistIndex)
                        .getString("url")
                        .replaceAll("/.+=", "");
                if (songId.isEmpty()) {
                    handleConnectionError("Url is empty!");
                } else if (!songId.equals(videoId)) {
                    Logger.printDebug(() -> "Video found (videoId: '" + videoId +
                            "', songId: '" + songId + "')");
                    return songId;
                }
            } catch (JSONException e) {
                Logger.printDebug(() -> "Fetch failed while processing response data for response: " + playlistJson);
            }
        }

        return null;
    }

    private static void handleConnectionError(@NonNull String errorMessage) {
        handleConnectionError(errorMessage, null);
    }

    private static void handleConnectionError(@NonNull String errorMessage, @Nullable Exception ex) {
        if (ex != null) {
            Logger.printInfo(() -> errorMessage, ex);
        }
    }


    /**
     * Time this instance and the fetch future was created.
     */
    private final Future<String> future;

    private PipedRequester(@NonNull String videoId, @NonNull String playlistId, final int playlistIndex) {
        this.future = Utils.submitOnBackgroundThread(() -> fetch(videoId, playlistId, playlistIndex));
    }

    /**
     * @return if the fetch call has completed.
     */
    public boolean fetchCompleted() {
        return future.isDone();
    }

    public String getStream() {
        try {
            return future.get(MAX_MILLISECONDS_TO_WAIT_FOR_FETCH, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            Logger.printInfo(() -> "getStream timed out", ex);
        } catch (InterruptedException ex) {
            Logger.printException(() -> "getStream interrupted", ex);
            Thread.currentThread().interrupt(); // Restore interrupt status flag.
        } catch (ExecutionException ex) {
            Logger.printException(() -> "getStream failure", ex);
        }

        return null;
    }
}
