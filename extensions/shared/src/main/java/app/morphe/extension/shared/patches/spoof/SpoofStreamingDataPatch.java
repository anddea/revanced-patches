package app.morphe.extension.shared.patches.spoof;

import static app.morphe.extension.shared.spoof.js.JavaScriptEngineSupport.supportsJavaScriptEngine;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import app.morphe.extension.shared.patches.AppCheckPatch;
import app.morphe.extension.shared.patches.PatchStatus;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.spoof.ClientType;
import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch;
import app.morphe.extension.shared.spoof.js.JavaScriptManager;
import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class SpoofStreamingDataPatch {
    // Keep the old name for existing availability classes and settings that still reference it.
    private static final boolean J2V8_LIBRARY_AVAILABILITY = supportsJavaScriptEngine();
    private static final boolean JAVASCRIPT_ENGINE_AVAILABILITY = supportsJavaScriptEngine();

    public static final boolean SPOOF_STREAMING_DATA =
            BaseSettings.SPOOF_STREAMING_DATA.get() && PatchStatus.SpoofStreamingData();

    private static boolean isSpoofingEnabled() {
        return SPOOF_STREAMING_DATA;
    }

    @NonNull
    private static ClientType getSelectedClientType() {
        if (AppCheckPatch.IS_YOUTUBE) {
            return app.morphe.extension.youtube.settings.Settings.SPOOF_VIDEO_STREAMS_CLIENT_TYPE.get();
        }

        return app.morphe.extension.music.settings.Settings.SPOOF_VIDEO_STREAMS_CLIENT_TYPE.get();
    }

    public static String blockGetAttRequest(String originalUrlString) {
        return SpoofVideoStreamsPatch.blockGetAttRequest(originalUrlString);
    }

    public static Uri blockGetWatchRequest(Uri playerRequestUri) {
        return SpoofVideoStreamsPatch.blockGetWatchRequest(playerRequestUri);
    }

    public static String blockInitPlaybackRequest(String originalUrlString) {
        return SpoofVideoStreamsPatch.blockInitPlaybackRequest(originalUrlString);
    }

    public static boolean disableSABR() {
        return SpoofVideoStreamsPatch.disableSABR();
    }

    public static boolean fixHLSCurrentTime(boolean original) {
        return SpoofVideoStreamsPatch.fixHLSCurrentTime(original);
    }

    public static boolean useMediaFetchHotConfigReplacement(boolean original) {
        return SpoofVideoStreamsPatch.useMediaFetchHotConfigReplacement(original);
    }

    public static boolean usePlaybackStartFeatureFlag(boolean original) {
        return SpoofVideoStreamsPatch.usePlaybackStartFeatureFlag(original);
    }

    public static void fetchStreams(String url, Map<String, String> requestHeader) {
        if (AppCheckPatch.IS_YOUTUBE) {
            app.morphe.extension.youtube.patches.spoof.SpoofVideoStreamsPatch.setClientOrderToUse();
        } else {
            app.morphe.extension.music.patches.spoof.SpoofVideoStreamsPatch.setClientOrderToUse();
        }
        SpoofVideoStreamsPatch.fetchStreams(url, requestHeader);
    }

    @Nullable
    public static StreamingData getStreamingData(@NonNull String videoId) {
        if (!isSpoofingEnabled()) {
            return null;
        }

        try {
            byte[] playerResponse = SpoofVideoStreamsPatch.getStreamingData(videoId);
            if (playerResponse == null) {
                return null;
            }

            return StreamingDataOuterClassPatch.parseFrom(ByteBuffer.wrap(playerResponse));
        } catch (Exception ex) {
            Logger.printException(() -> "getStreamingData failure", ex);
            return null;
        }
    }

    public static List<Object> prioritizeVideoQuality(@Nullable String videoId, @NonNull List<Object> adaptiveFormats) {
        return adaptiveFormats;
    }

    @Nullable
    public static byte[] removeVideoPlaybackPostBody(Uri uri, int method, byte[] postData) {
        return SpoofVideoStreamsPatch.removeVideoPlaybackPostBody(uri, method, postData);
    }

    @Nullable
    public static String newPlayerResponseParameter(@NonNull String newlyLoadedVideoId, @Nullable String playerParameter) {
        return playerParameter;
    }

    @Nullable
    public static String newPlayerResponseParameter(@NonNull String newlyLoadedVideoId,
                                                    @Nullable String playerParameter,
                                                    @Nullable String newlyLoadedPlaylistId,
                                                    boolean isShortAndOpeningOrPlaying) {
        return playerParameter;
    }

    public static void initializeJavascript() {
        if (isSpoofingEnabled()
                && getSelectedClientType().requireJS
                && JAVASCRIPT_ENGINE_AVAILABILITY) {
            app.morphe.extension.shared.utils.Utils.runOnBackgroundThread(JavaScriptManager::getSignatureTimestamp);
        }
    }

    public static String appendSpoofedClient(String format) {
        return SpoofVideoStreamsPatch.appendSpoofedClient(format);
    }

    public static boolean multiAudioTrackAvailable() {
        return !SpoofVideoStreamsPatch.spoofingToClientWithNoMultiAudioStreams();
    }

    public static final class ClientAndroidVRAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get()
                    && getSelectedClientType().name().startsWith("ANDROID_VR");
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(BaseSettings.SPOOF_STREAMING_DATA);
        }
    }

    public static final class HideAudioFlyoutMenuAvailability implements Setting.Availability {
        private static final boolean AVAILABLE_ON_LAUNCH = SpoofStreamingDataPatch.multiAudioTrackAvailable();

        @Override
        public boolean isAvailable() {
            return AVAILABLE_ON_LAUNCH && SpoofStreamingDataPatch.multiAudioTrackAvailable();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(BaseSettings.SPOOF_STREAMING_DATA);
        }
    }

    public static final class J2V8Availability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() && J2V8_LIBRARY_AVAILABILITY;
        }
    }
}
