package app.revanced.extension.shared.patches.spoof;

import android.annotation.TargetApi;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import app.revanced.extension.shared.innertube.client.YouTubeAppClient.ClientType;
import app.revanced.extension.shared.innertube.utils.ThrottlingParameterUtils;
import app.revanced.extension.shared.patches.PatchStatus;
import app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.shared.VideoInformation;

@TargetApi(26)
@SuppressWarnings({"deprecation", "unused"})
public class SpoofStreamingDataPatch extends BlockRequestPatch {
    private static final boolean SPOOF_STREAMING_DATA_USE_IOS =
            PatchStatus.SpoofStreamingDataIOS() && BaseSettings.SPOOF_STREAMING_DATA_USE_IOS.get();
    private static final boolean SPOOF_STREAMING_DATA_USE_TV =
            SPOOF_STREAMING_DATA && BaseSettings.SPOOF_STREAMING_DATA_USE_TV.get();
    private static final boolean J2V8_LIBRARY_AVAILABILITY = checkJ2V8();
    private static final boolean SPOOF_STREAMING_DATA_USE_TV_ALL =
            SPOOF_STREAMING_DATA_USE_TV && BaseSettings.SPOOF_STREAMING_DATA_USE_TV_ALL.get();
    private static final boolean SPOOF_STREAMING_DATA_TV_USE_LATEST_JS =
            SPOOF_STREAMING_DATA_USE_TV && BaseSettings.SPOOF_STREAMING_DATA_TV_USE_LATEST_JS.get();
    private static final boolean SPOOF_STREAMING_DATA_TV_USE_V8_JS_ENGINE =
            SPOOF_STREAMING_DATA_USE_TV && BaseSettings.SPOOF_STREAMING_DATA_TV_USE_V8_JS_ENGINE.get() &&
                    J2V8_LIBRARY_AVAILABILITY;

    /**
     * Any unreachable ip address.  Used to intentionally fail requests.
     */
    private static final String UNREACHABLE_HOST_URI_STRING = "https://127.0.0.0";
    private static final Uri UNREACHABLE_HOST_URI = Uri.parse(UNREACHABLE_HOST_URI_STRING);

    /**
     * Parameters causing playback issues.
     */
    private static final String[] AUTOPLAY_PARAMETERS = {
            "YAHI", // Autoplay in feed.
            "SAFg"  // Autoplay in scrim.
    };
    /**
     * Parameters used when playing clips.
     */
    private static final String CLIPS_PARAMETERS = "kAIB";
    /**
     * Prefix present in all Short player parameters signature.
     */
    private static final String SHORTS_PLAYER_PARAMETERS = "8AEB";
    /**
     * If {@link SpoofStreamingDataPatch#SPOOF_STREAMING_DATA_USE_TV_ALL} is false,
     * Autoplay in feed, Clips, and Shorts will not use the TV client for fast playback.
     * The player parameter is used to detect the video type.
     */
    @NonNull
    private static volatile String reasonSkipped = "";

    /**
     * If the app is installed via mounting, it will fail to load the J2V8 library.
     * So, when the class is loaded, it should first check if the J2V8 library is present.
     * <p>
     * TODO: A feature should be implemented in revanced-library to copy external libraries
     *       to the original app's path (/data/data/com.google.android.youtube/lib/*).
     * @return Whether the J2V8 library exists.
     */
    private static boolean checkJ2V8() {
        if (SPOOF_STREAMING_DATA_USE_TV) {
            try {
                String libraryDir = Utils.getContext()
                        .getApplicationContext()
                        .getApplicationInfo()
                        .nativeLibraryDir;
                File j2v8 = new File(libraryDir + "/libj2v8.so");
                return j2v8.exists();
            } catch (Exception ex) {
                Logger.printException(() -> "J2V8 native library not found", ex);
            }
        }

        return false;
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
     * Skip response encryption in OnesiePlayerRequest.
     */
    public static boolean skipResponseEncryption(boolean original) {
        if (!SPOOF_STREAMING_DATA) {
            return original;
        }
        return false;
    }

    /**
     * Injection point.
     * Turns off a feature flag that interferes with video playback.
     */
    public static boolean usePlaybackStartFeatureFlag(boolean original) {
        if (!SPOOF_STREAMING_DATA) {
            return original;
        }
        return false;
    }

    /**
     * Injection point.
     */
    public static void fetchStreams(String url, Map<String, String> requestHeader) {
        if (SPOOF_STREAMING_DATA) {
            String id = Utils.getVideoIdFromRequest(url);
            if (id == null) {
                Logger.printException(() -> "Ignoring request with no id: " + url);
                return;
            } else if (id.isEmpty()) {
                return;
            }

            StreamingDataRequest.fetchRequest(id, requestHeader, reasonSkipped);
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
    public static StreamingData getStreamingData(@NonNull String videoId) {
        if (SPOOF_STREAMING_DATA && isValidVideoId(videoId)) {
            try {
                StreamingDataRequest request = StreamingDataRequest.getRequestForVideoId(videoId);
                if (request != null) {
                    // This hook is always called off the main thread,
                    // but this can later be called for the same video id from the main thread.
                    // This is not a concern, since the fetch will always be finished
                    // and never block the main thread.
                    // But if debugging, then still verify this is the situation.
                    if (BaseSettings.DEBUG.get() && !request.fetchCompleted() && Utils.isCurrentlyOnMainThread()) {
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
     * <p>
     * Since {@link SpoofStreamingDataPatch} is on a shared path,
     * {@link VideoInformation#newPlayerResponseParameter(String, String, String, boolean)} is not used.
     */
    @Nullable
    public static String newPlayerResponseParameter(@NonNull String newlyLoadedVideoId, @Nullable String playerParameter,
                                                    @Nullable String newlyLoadedPlaylistId, boolean isShortAndOpeningOrPlaying) {
        if (SPOOF_STREAMING_DATA_USE_TV) {
            reasonSkipped = "";
            if (!SPOOF_STREAMING_DATA_USE_TV_ALL && playerParameter != null) {
                if (playerParameter.startsWith(SHORTS_PLAYER_PARAMETERS)) {
                    reasonSkipped = "Shorts";
                }  else if (StringUtils.startsWithAny(playerParameter, AUTOPLAY_PARAMETERS)) {
                    reasonSkipped = "Autoplay in feed";
                } else if (playerParameter.length() > 150 || playerParameter.startsWith(CLIPS_PARAMETERS)) {
                    reasonSkipped = "Clips";
                }
            }
        }

        return playerParameter; // Return the original value since we are observing and not modifying.
    }

    /**
     * Injection point.
     * <p>
     * It takes about 3-5 seconds to download the JavaScript and initialize the Cipher class.
     * Initialize it before the video starts.
     * Used for {@link ClientType#TV}, {@link ClientType#TV_SIMPLY} and {@link ClientType#TV_EMBEDDED}.
     */
    public static void initializeJavascript() {
        if (SPOOF_STREAMING_DATA_USE_TV) {
            // Download JavaScript and initialize the Cipher class
            CompletableFuture.runAsync(() -> ThrottlingParameterUtils.initializeJavascript(
                    SPOOF_STREAMING_DATA_TV_USE_LATEST_JS,
                    SPOOF_STREAMING_DATA_TV_USE_V8_JS_ENGINE
            ));
        }
    }

    /**
     * Injection point.
     */
    public static String appendSpoofedClient(String format) {
        try {
            if (SPOOF_STREAMING_DATA && BaseSettings.SPOOF_STREAMING_DATA_STATS_FOR_NERDS.get()
                    && !TextUtils.isEmpty(format)) {
                // Force LTR layout, to match the same LTR video time/length layout YouTube uses for all languages
                return "\u202D" + format + String.format("\u2009(%s)", StreamingDataRequest.getLastSpoofedClientName()); // u202D = left to right override
            }
        } catch (Exception ex) {
            Logger.printException(() -> "appendSpoofedClient failure", ex);
        }

        return format;
    }

    public static boolean notSpoofingToAndroidAudio() {
        return !PatchStatus.SpoofStreamingData()
                || !BaseSettings.SPOOF_STREAMING_DATA.get()
                || !BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().name().startsWith("ANDROID"); // IOS, TV
    }

    public static final class ClientAndroidVRAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().name().startsWith("ANDROID_VR");
        }
    }

    public static final class ClientAndroidVRNoAuthAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get() == ClientType.ANDROID_VR_NO_AUTH;
        }
    }

    public static final class ClientiOSAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().name().startsWith("IOS");
        }
    }

    public static final class ClientTVAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().name().startsWith("TV");
        }
    }

    public static final class ForceOriginalAudioAvailability implements Setting.Availability {
        private static final boolean AVAILABLE_ON_LAUNCH = SpoofStreamingDataPatch.notSpoofingToAndroidAudio();

        @Override
        public boolean isAvailable() {
            // Check conditions of launch and now. Otherwise if spoofing is changed
            // without a restart the setting will show as available when it's not.
            return AVAILABLE_ON_LAUNCH && SpoofStreamingDataPatch.notSpoofingToAndroidAudio();
        }
    }

    public static final class J2V8Availability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().name().startsWith("TV") &&
                    J2V8_LIBRARY_AVAILABILITY;
        }
    }

    public static final class HideAudioFlyoutMenuAvailability implements Setting.Availability {
        private static final boolean AVAILABLE_ON_LAUNCH = SpoofStreamingDataPatch.notSpoofingToAndroidAudio();

        @Override
        public boolean isAvailable() {
            // Check conditions of launch and now. Otherwise if spoofing is changed
            // without a restart the setting will show as available when it's not.
            return AVAILABLE_ON_LAUNCH && SpoofStreamingDataPatch.notSpoofingToAndroidAudio();
        }
    }
}
