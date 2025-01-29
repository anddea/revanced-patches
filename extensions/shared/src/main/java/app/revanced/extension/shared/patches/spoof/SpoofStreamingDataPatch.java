package app.revanced.extension.shared.patches.spoof;

import static app.revanced.extension.shared.patches.PatchStatus.SpoofStreamingDataMusic;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import app.revanced.extension.shared.patches.client.AppClient.ClientType;
import app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class SpoofStreamingDataPatch {
    private static final boolean SPOOF_STREAMING_DATA = BaseSettings.SPOOF_STREAMING_DATA.get();
    private static final boolean SPOOF_STREAMING_DATA_YOUTUBE = SPOOF_STREAMING_DATA && !SpoofStreamingDataMusic();
    private static final boolean SPOOF_STREAMING_DATA_MUSIC = SPOOF_STREAMING_DATA && SpoofStreamingDataMusic();
    private static final String PO_TOKEN =
            BaseSettings.SPOOF_STREAMING_DATA_PO_TOKEN.get();
    private static final String VISITOR_DATA =
            BaseSettings.SPOOF_STREAMING_DATA_VISITOR_DATA.get();

    /**
     * Any unreachable ip address.  Used to intentionally fail requests.
     */
    private static final String UNREACHABLE_HOST_URI_STRING = "https://127.0.0.0";
    private static final Uri UNREACHABLE_HOST_URI = Uri.parse(UNREACHABLE_HOST_URI_STRING);

    /**
     * Key: video id
     * Value: original video length [streamingData.formats.approxDurationMs]
     */
    private static final Map<String, Long> approxDurationMsMap = Collections.synchronizedMap(
            new LinkedHashMap<>(10) {
                private static final int CACHE_LIMIT = 5;

                @Override
                protected boolean removeEldestEntry(Entry eldest) {
                    return size() > CACHE_LIMIT; // Evict the oldest entry if over the cache limit.
                }
            });

    /**
     * Injection point.
     * Blocks /get_watch requests by returning an unreachable URI.
     *
     * @param playerRequestUri The URI of the player request.
     * @return An unreachable URI if the request is a /get_watch request, otherwise the original URI.
     */
    public static Uri blockGetWatchRequest(Uri playerRequestUri) {
        if (SPOOF_STREAMING_DATA) {
            // An exception may be thrown when the /get_watch request is blocked when connected to Wi-Fi in YouTube Music.
            if (SPOOF_STREAMING_DATA_YOUTUBE || Utils.getNetworkType() == Utils.NetworkType.MOBILE) {
                try {
                    String path = playerRequestUri.getPath();

                    if (path != null && path.contains("get_watch")) {
                        Logger.printDebug(() -> "Blocking 'get_watch' by returning unreachable uri");

                        return UNREACHABLE_HOST_URI;
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "blockGetWatchRequest failure", ex);
                }
            }
        }

        return playerRequestUri;
    }

    /**
     * Injection point.
     * <p>
     * Blocks /initplayback requests.
     */
    public static String blockInitPlaybackRequest(String originalUrlString) {
        if (SPOOF_STREAMING_DATA) {
            try {
                var originalUri = Uri.parse(originalUrlString);
                String path = originalUri.getPath();

                if (path != null && path.contains("initplayback")) {
                    Logger.printDebug(() -> "Blocking 'initplayback' by clearing query");

                    return originalUri.buildUpon().clearQuery().build().toString();
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockInitPlaybackRequest failure", ex);
            }
        }

        return originalUrlString;
    }

    /**
     * Injection point.
     */
    public static boolean isSpoofingEnabled() {
        return SPOOF_STREAMING_DATA;
    }

    /**
     * Injection point.
     * This method is only invoked when playing a livestream on an iOS client.
     */
    public static boolean fixHLSCurrentTime(boolean original) {
        if (!SPOOF_STREAMING_DATA) {
            return original;
        }
        return false;
    }

    private static volatile String auth = "";
    private static volatile Map<String, String> requestHeader;

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String[] REQUEST_HEADER_KEYS = {
            AUTHORIZATION_HEADER,
            "X-GOOG-API-FORMAT-VERSION",
            "X-Goog-Visitor-Id"
    };

    /**
     * If the /get_watch request is not blocked,
     * fetchRequest will not be invoked at the point where the video starts.
     * <p>
     * An additional method is used to invoke fetchRequest in YouTube Music:
     * 1. Save the requestHeader in a field.
     * 2. Invoke fetchRequest with the videoId used in PlaybackStartDescriptor.
     * <p>
     *
     * @param requestHeaders Save the request Headers used for login to a field.
     *                       Only used in YouTube Music where login is required.
     */
    private static void setRequestHeaders(Map<String, String> requestHeaders) {
        if (SPOOF_STREAMING_DATA_MUSIC) {
            try {
                // Save requestHeaders whenever an account is switched.
                String authorization = requestHeaders.get(AUTHORIZATION_HEADER);
                if (authorization == null || auth.equals(authorization)) {
                    return;
                }
                for (String key : REQUEST_HEADER_KEYS) {
                    if (requestHeaders.get(key) == null) {
                        return;
                    }
                }
                auth = authorization;
                requestHeader = requestHeaders;
            } catch (Exception ex) {
                Logger.printException(() -> "setRequestHeaders failure", ex);
            }
        }
    }

    /**
     * Injection point.
     */
    public static void fetchStreams(@NonNull String videoId) {
        if (SPOOF_STREAMING_DATA_MUSIC) {
            try {
                if (requestHeader != null) {
                    StreamingDataRequest.fetchRequest(videoId, requestHeader, VISITOR_DATA, PO_TOKEN);
                } else {
                    Logger.printDebug(() -> "Ignoring request with no header.");
                }
            } catch (Exception ex) {
                Logger.printException(() -> "fetchStreams failure", ex);
            }
        }
    }

    /**
     * Injection point.
     */
    public static void fetchStreams(String url, Map<String, String> requestHeaders) {
        setRequestHeaders(requestHeaders);

        if (SPOOF_STREAMING_DATA) {
            try {
                Uri uri = Uri.parse(url);
                String path = uri.getPath();
                if (path == null || !path.contains("player")) {
                    return;
                }

                // 'get_drm_license' has no video id and appears to happen when waiting for a paid video to start.
                // 'heartbeat' has no video id and appears to be only after playback has started.
                // 'refresh' has no video id and appears to happen when waiting for a livestream to start.
                // 'ad_break' has no video id.
                if (path.contains("get_drm_license") || path.contains("heartbeat") || path.contains("refresh") || path.contains("ad_break")) {
                    Logger.printDebug(() -> "Ignoring path: " + path);
                    return;
                }

                String id = uri.getQueryParameter("id");
                if (id == null) {
                    Logger.printException(() -> "Ignoring request with no id: " + url);
                    return;
                }

                StreamingDataRequest.fetchRequest(id, requestHeaders, VISITOR_DATA, PO_TOKEN);
            } catch (Exception ex) {
                Logger.printException(() -> "fetchStreams failure", ex);
            }
        }
    }

    /**
     * Injection point.
     * Fix playback by replace the streaming data.
     * Called after {@link #fetchStreams(String, Map)}.
     */
    @Nullable
    public static ByteBuffer getStreamingData(String videoId) {
        if (SPOOF_STREAMING_DATA) {
            try {
                StreamingDataRequest request = StreamingDataRequest.getRequestForVideoId(videoId);
                if (request != null) {
                    // This hook is always called off the main thread,
                    // but this can later be called for the same video id from the main thread.
                    // This is not a concern, since the fetch will always be finished
                    // and never block the main thread.
                    // But if debugging, then still verify this is the situation.
                    if (BaseSettings.ENABLE_DEBUG_LOGGING.get() && !request.fetchCompleted() && Utils.isCurrentlyOnMainThread()) {
                        Logger.printException(() -> "Error: Blocking main thread");
                    }

                    var stream = request.getStream();
                    if (stream != null) {
                        Logger.printDebug(() -> "Overriding video stream: " + videoId);
                        return stream;
                    }
                }

                Logger.printDebug(() -> "Not overriding streaming data (video stream is null): " + videoId);
            } catch (Exception ex) {
                Logger.printException(() -> "getStreamingData failure", ex);
            }
        }

        return null;
    }

    /**
     * Injection point.
     * <p>
     * If spoofed [streamingData.formats] is empty,
     * Put the original [streamingData.formats.approxDurationMs] into the HashMap.
     * <p>
     * Called after {@link #getStreamingData(String)}.
     */
    public static void setApproxDurationMs(String videoId, long approxDurationMs) {
        if (SPOOF_STREAMING_DATA_YOUTUBE && approxDurationMs != Long.MAX_VALUE) {
            approxDurationMsMap.put(videoId, approxDurationMs);
            Logger.printDebug(() -> "New approxDurationMs loaded, video id: " + videoId + ", video length: " + approxDurationMs);
        }
    }

    /**
     * Injection point.
     * <p>
     * When measuring the length of a video in an Android YouTube client,
     * the client first checks if the streaming data contains [streamingData.formats.approxDurationMs].
     * <p>
     * If the streaming data response contains [approxDurationMs] (Long type, actual value), this value will be the video length.
     * <p>
     * If [streamingData.formats] (List type) is empty, the [approxDurationMs] value cannot be accessed,
     * So it falls back to the value of [videoDetails.lengthSeconds] (Integer type, approximate value) multiplied by 1000.
     * <p>
     * For iOS clients, [streamingData.formats] (List type) is always empty, so it always falls back to the approximate value.
     * <p>
     * Called after {@link #getStreamingData(String)}.
     */
    public static long getApproxDurationMs(String videoId) {
        if (SPOOF_STREAMING_DATA_YOUTUBE && videoId != null) {
            final Long approxDurationMs = approxDurationMsMap.get(videoId);
            if (approxDurationMs != null) {
                Logger.printDebug(() -> "Replacing video length: " + approxDurationMs + " for videoId: " + videoId);
                return approxDurationMs;
            }
        }
        return Long.MAX_VALUE;
    }

    /**
     * Injection point.
     * Called after {@link #getStreamingData(String)}.
     */
    @Nullable
    public static byte[] removeVideoPlaybackPostBody(Uri uri, int method, byte[] postData) {
        if (SPOOF_STREAMING_DATA) {
            try {
                final int methodPost = 2;
                if (method == methodPost) {
                    String path = uri.getPath();
                    if (path != null && path.contains("videoplayback")) {
                        return null;
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "removeVideoPlaybackPostBody failure", ex);
            }
        }

        return postData;
    }

    /**
     * Injection point.
     */
    public static String appendSpoofedClient(String videoFormat) {
        try {
            if (SPOOF_STREAMING_DATA && BaseSettings.SPOOF_STREAMING_DATA_STATS_FOR_NERDS.get()
                    && !TextUtils.isEmpty(videoFormat)) {
                // Force LTR layout, to match the same LTR video time/length layout YouTube uses for all languages
                return "\u202D" + videoFormat + String.format("\u2009(%s)", StreamingDataRequest.getLastSpoofedClientName()); // u202D = left to right override
            }
        } catch (Exception ex) {
            Logger.printException(() -> "appendSpoofedClient failure", ex);
        }

        return videoFormat;
    }

    public static final class AudioStreamLanguageOverrideAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_TYPE.get() == ClientType.ANDROID_VR_NO_AUTH;
        }
    }
}
