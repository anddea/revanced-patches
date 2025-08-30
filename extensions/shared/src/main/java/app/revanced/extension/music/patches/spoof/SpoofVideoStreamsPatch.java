package app.revanced.extension.music.patches.spoof;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData;

import java.nio.ByteBuffer;
import java.util.Map;

import app.revanced.extension.music.patches.spoof.requests.StreamingDataRequest;
import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class SpoofVideoStreamsPatch extends BlockRequestPatch {
    public interface StreamingDataMessage {
        // Methods are added to YT classes during patching.
        StreamingData parseFrom(ByteBuffer responseProto);
    }

    /**
     * This class can be null, as hooking and invoking are performed in different methods.
     */
    @Nullable
    private static StreamingDataMessage streamingDataMessage;

    /**
     * Parse the Proto Buffer and convert it to StreamingData (GeneratedMessage).
     *
     * @param responseProto Proto Buffer.
     * @return StreamingData (GeneratedMessage) parsed by ProtoParser.
     */
    @Nullable
    public static StreamingData parseFrom(ByteBuffer responseProto) {
        try {
            if (streamingDataMessage == null) {
                Logger.printDebug(() -> "Cannot parseFrom because streaming data is null");
            } else {
                return streamingDataMessage.parseFrom(responseProto);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "parseFrom failure", ex);
        }
        return null;
    }

    /**
     * Injection point.
     */
    public static void initialize(StreamingDataMessage message) {
        if (SPOOF_VIDEO_STREAMS && message != null) {
            streamingDataMessage = message;
        }
    }

    /**
     * Injection point.
     * This method is only invoked when playing a livestream on an iOS client.
     */
    public static boolean fixHLSCurrentTime(boolean original) {
        if (!SPOOF_VIDEO_STREAMS) {
            return original;
        }
        return false;
    }

    /**
     * Injection point.
     * Skip response encryption in OnesiePlayerRequest.
     */
    public static boolean skipResponseEncryption(boolean original) {
        if (!SPOOF_VIDEO_STREAMS) {
            return original;
        }
        return false;
    }

    /**
     * Injection point.
     * Turns off a feature flag that interferes with video playback.
     */
    public static boolean usePlaybackStartFeatureFlag(boolean original) {
        if (!SPOOF_VIDEO_STREAMS) {
            return original;
        }
        return false;
    }

    /**
     * Injection point.
     */
    public static void fetchStreams(String urlString, Map<String, String> requestHeaders) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                if (urlString != null) {
                    var uri = Uri.parse(urlString);
                    String path = uri.getPath();
                    if (path != null) {
                        if (path.contains("player") && uri.getQueryParameter("t") != null) {
                            String id = uri.getQueryParameter("id");
                            if (id != null) {
                                StreamingDataRequest.fetchRequestIfNeeded(id, requestHeaders);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "fetchStreams failure", ex);
            }
        }
    }

    private static boolean isValidVideoId(@Nullable String videoId) {
        return videoId != null && !videoId.isEmpty() && !"zzzzzzzzzzz".equals(videoId);
    }

    /**
     * Injection point.
     * Fix playback by replace the streaming data.
     * Called after {@link #fetchStreams(String, Map)}.
     */
    public static StreamingData getStreamingData(@Nullable String videoId) {
        if (SPOOF_VIDEO_STREAMS && isValidVideoId(videoId)) {
            try {
                StreamingDataRequest request = StreamingDataRequest.getRequestForVideoId(videoId);
                if (request != null) {
                    // This hook is always called off the main thread,
                    // but this can later be called for the same video id from the main thread.
                    // This is not a concern, since the fetch will always be finished
                    // and never block the main thread.
                    // But if debugging, then still verify this is the situation.
                    if (Settings.DEBUG.get() && !request.fetchCompleted() && Utils.isCurrentlyOnMainThread()) {
                        Logger.printException(() -> "Error: Blocking main thread");
                    }

                    var stream = request.getStream();
                    if (stream != null) {
                        Logger.printDebug(() -> "Overriding video stream: " + videoId);
                        return stream;
                    }
                }

                Logger.printDebug(() -> "Not overriding streaming data (video stream is null, it may be video ads): " + videoId);
            } catch (Exception ex) {
                Logger.printException(() -> "getStreamingData failure", ex);
            }
        }

        return null;
    }

}
