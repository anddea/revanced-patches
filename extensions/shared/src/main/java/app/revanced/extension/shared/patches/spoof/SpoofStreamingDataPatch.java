package app.revanced.extension.shared.patches.spoof;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass$StreamingData;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.revanced.extension.shared.patches.BlockRequestPatch;
import app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class SpoofStreamingDataPatch extends BlockRequestPatch {
    /**
     * Even if the default client is not iOS, videos that cannot be played on Android VR or Android TV will fall back to iOS.
     * Do not add a dependency that checks whether the default client is iOS or not.
     */
    private static final boolean SPOOF_STREAMING_DATA_SYNC_VIDEO_LENGTH =
            SPOOF_STREAMING_DATA && BaseSettings.SPOOF_STREAMING_DATA_SYNC_VIDEO_LENGTH.get();

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

    /**
     * Injection point.
     */
    public static void fetchStreams(String url, Map<String, String> requestHeaders) {
        if (SPOOF_STREAMING_DATA) {
            try {
                Uri uri = Uri.parse(url);
                String path = uri.getPath();

                // 'heartbeat' has no video id and appears to be only after playback has started.
                // 'refresh' has no video id and appears to happen when waiting for a livestream to start.
                if (path != null && path.contains("player") && !path.contains("heartbeat")
                        && !path.contains("refresh")) {
                    String id = uri.getQueryParameter("id");
                    if (id == null) {
                        Logger.printException(() -> "Ignoring request that has no video id." +
                                " Url: " + url + " headers: " + requestHeaders);
                        return;
                    }

                    StreamingDataRequest.fetchRequest(id, requestHeaders);
                }
            } catch (Exception ex) {
                Logger.printException(() -> "buildRequest failure", ex);
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
    public static void setApproxDurationMs(String videoId, String approxDurationMsFieldName,
                                           StreamingDataOuterClass$StreamingData originalStreamingData, StreamingDataOuterClass$StreamingData spoofedStreamingData) {
        if (SPOOF_STREAMING_DATA_SYNC_VIDEO_LENGTH) {
            if (formatsIsEmpty(spoofedStreamingData)) {
                List<?> originalFormats = getFormatsFromStreamingData(originalStreamingData);
                Long approxDurationMs = getApproxDurationMs(originalFormats, approxDurationMsFieldName);
                if (approxDurationMs != null) {
                    approxDurationMsMap.put(videoId, approxDurationMs);
                    Logger.printDebug(() -> "New approxDurationMs loaded, video id: " + videoId + ", video length: " + approxDurationMs);
                } else {
                    Logger.printDebug(() -> "Ignoring as original approxDurationMs is not found, video id: " + videoId);
                }
            } else {
                Logger.printDebug(() -> "Ignoring as spoofed formats is not empty, video id: " + videoId);
            }
        }
    }

    /**
     * Looks like the initial value for the videoId field.
     */
    private static final String MASKED_VIDEO_ID = "zzzzzzzzzzz";

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
    public static long getApproxDurationMsFromOriginalResponse(String videoId, long lengthMilliseconds) {
        if (SPOOF_STREAMING_DATA_SYNC_VIDEO_LENGTH) {
            try {
                if (videoId != null && !videoId.equals(MASKED_VIDEO_ID)) {
                    Long approxDurationMs = approxDurationMsMap.get(videoId);
                    if (approxDurationMs != null) {
                        Logger.printDebug(() -> "Replacing video length from " + lengthMilliseconds + " to " + approxDurationMs + " , videoId: " + videoId);
                        approxDurationMsMap.remove(videoId);
                        return approxDurationMs;
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "getOriginalFormats failure", ex);
            }
        }
        return lengthMilliseconds;
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

    // Utils

    private static boolean formatsIsEmpty(StreamingDataOuterClass$StreamingData streamingData) {
        List<?> formats = getFormatsFromStreamingData(streamingData);
        return formats == null || formats.size() == 0;
    }

    private static List<?> getFormatsFromStreamingData(StreamingDataOuterClass$StreamingData streamingData) {
        try {
            // Field e: 'formats'.
            // Field name is always 'e', regardless of the client version.
            Field field = streamingData.getClass().getDeclaredField("e");
            field.setAccessible(true);
            if (field.get(streamingData) instanceof List<?> list) {
                return list;
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Logger.printException(() -> "Reflection error accessing formats", ex);
        }
        return null;
    }

    private static Long getApproxDurationMs(List<?> list, String approxDurationMsFieldName) {
        try {
            if (list != null) {
                var iterator = list.listIterator();
                if (iterator.hasNext()) {
                    var formats = iterator.next();
                    Field field = formats.getClass().getDeclaredField(approxDurationMsFieldName);
                    field.setAccessible(true);
                    if (field.get(formats) instanceof Long approxDurationMs) {
                        return approxDurationMs;
                    } else {
                        Logger.printDebug(() -> "Field type is null: " + approxDurationMsFieldName);
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Logger.printException(() -> "Reflection error accessing field: " + approxDurationMsFieldName, ex);
        }
        return null;
    }
}
