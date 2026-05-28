package app.morphe.extension.shared.spoof;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.requests.Route;
import app.morphe.extension.shared.settings.AppLanguage;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.spoof.requests.StreamOrDetailsDataRequest;

@SuppressWarnings("unused")
public class SpoofVideoStreamsPatch {
    public static volatile Map<String, String> currentVideoRequestHeader;
    public static String pageIDHeaderValue = "";

    public static final class JavaScriptClientAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return SharedYouTubeSettings.SPOOF_VIDEO_STREAMS.isAvailable() && preferredClient.requireJS;
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(SharedYouTubeSettings.SPOOF_VIDEO_STREAMS);
        }
    }

    public static final class JavaScriptHashAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return SharedYouTubeSettings.SPOOF_VIDEO_STREAMS.isAvailable() && preferredClient.requireJS &&
                    SharedYouTubeSettings.SPOOF_VIDEO_STREAMS_DISABLE_PLAYER_JS_UPDATE.get();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(
                    SharedYouTubeSettings.SPOOF_VIDEO_STREAMS,
                    SharedYouTubeSettings.SPOOF_VIDEO_STREAMS_DISABLE_PLAYER_JS_UPDATE
            );
        }
    }

    private static final String INTERNET_CONNECTION_CHECK_URI_STRING = "https://www.google.com/gen_204";
    private static final Uri INTERNET_CONNECTION_CHECK_URI = Uri.parse(INTERNET_CONNECTION_CHECK_URI_STRING);

    private static final boolean SPOOF_VIDEO_STREAMS = SharedYouTubeSettings.SPOOF_VIDEO_STREAMS.get();

    @Nullable
    private static volatile AppLanguage languageOverride;

    private static volatile ClientType preferredClient = ClientType.ANDROID_REEL_AUTH;

    private static WeakReference<Application> mainActivityRef = new WeakReference<>(null);

    public static void setMainActivity(Activity activity) {
        mainActivityRef = new WeakReference<>(activity.getApplication());
    }

    public static Application getApplication() {
        return mainActivityRef.get();
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    @Nullable
    public static AppLanguage getLanguageOverride() {
        return languageOverride;
    }

    public static void setLanguageOverride(@Nullable AppLanguage language) {
        languageOverride = language;
    }

    public static void setClientsToUse(List<ClientType> availableClients, ClientType client) {
        preferredClient = Objects.requireNonNull(client);
        StreamOrDetailsDataRequest.setClientOrderToUse(availableClients, client);
    }

    public static ClientType getPreferredClient() {
        return preferredClient;
    }

    public static boolean spoofingToClientWithNoMultiAudioStreams() {
        return isPatchIncluded()
                && SPOOF_VIDEO_STREAMS
                && !preferredClient.supportsMultiAudioTracks;
    }

    public static Uri blockGetWatchRequest(Uri playerRequestUri) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                String path = playerRequestUri.getPath();

                if (path != null && path.contains("get_watch")) {
                    Logger.printDebug(() -> "Blocking 'get_watch' by returning internet connection check URI");
                    return INTERNET_CONNECTION_CHECK_URI;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockGetWatchRequest failure", ex);
            }
        }
        return playerRequestUri;
    }

    public static Uri.Builder blockGetWatchRequest(Uri.Builder playerRequestBuilder) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                String path = playerRequestBuilder.build().getPath();

                if (path != null && path.contains("get_watch")) {
                    Logger.printDebug(() -> "Blocking 'get_watch' by returning internet connection check URI");
                    return INTERNET_CONNECTION_CHECK_URI.buildUpon();
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockGetWatchRequest failure", ex);
            }
        }
        return playerRequestBuilder;
    }

    public static String blockGetAttRequest(String originalUrlString) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                var originalUri = Uri.parse(originalUrlString);
                String path = originalUri.getPath();

                if (path != null && path.contains("att/get")) {
                    Logger.printDebug(() -> "Blocking 'att/get' by returning internet connection check URI");
                    return INTERNET_CONNECTION_CHECK_URI_STRING;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockGetAttRequest failure", ex);
            }
        }
        return originalUrlString;
    }

    public static String blockInitPlaybackRequest(String originalUrlString) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                var originalUri = Uri.parse(originalUrlString);
                String path = originalUri.getPath();

                if (path != null && path.contains("initplayback")) {
                    Logger.printDebug(() -> "Blocking 'initplayback' by returning internet connection check URI");
                    return INTERNET_CONNECTION_CHECK_URI_STRING;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockInitPlaybackRequest failure", ex);
            }
        }
        return originalUrlString;
    }

    public static boolean isSpoofingEnabled() {
        return SPOOF_VIDEO_STREAMS;
    }

    public static boolean fixHLSCurrentTime(boolean original) {
        if (!SPOOF_VIDEO_STREAMS) {
            return original;
        }
        return false;
    }

    public static boolean disableSABR() {
        return SPOOF_VIDEO_STREAMS;
    }

    public static boolean useMediaFetchHotConfigReplacement(boolean original) {
        if (original) {
            Logger.printDebug(() -> "useMediaFetchHotConfigReplacement is set on");
        }
        if (!SPOOF_VIDEO_STREAMS) {
            return original;
        }
        return false;
    }

    public static boolean usePlaybackStartFeatureFlag(boolean original) {
        if (original) {
            Logger.printDebug(() -> "usePlaybackStartFeatureFlag is set on");
        }
        if (!SPOOF_VIDEO_STREAMS) {
            return original;
        }
        return false;
    }

    public static boolean useReelItemWatchResponseFeatureFlag(boolean original) {
        if (original) {
            Logger.printDebug(() -> "useReelItemWatchResponse is set on");
        }
        if (!SPOOF_VIDEO_STREAMS) {
            return original;
        }
        return false;
    }

    public static boolean useMediaSessionFeatureFlag(boolean original) {
        if (original) {
            Logger.printDebug(() -> "useMediaSessionFeatureFlag is set on");
        }
        if (!SPOOF_VIDEO_STREAMS) {
            return original;
        }
        return false;
    }

    public static void fetchStreams(String url, Map<String, String> requestHeaders) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                Uri uri = Uri.parse(url);
                String path = uri.getPath();
                if (path == null || !path.contains("player")) {
                    return;
                }

                if (path.contains("get_drm_license") || path.contains("heartbeat")
                        || path.contains("refresh") || path.contains("ad_break")) {
                    return;
                }

                String id = uri.getQueryParameter("id");
                if (id == null) {
                    return;
                }

                currentVideoRequestHeader = requestHeaders;
                StreamOrDetailsDataRequest.fetchStreamRequest(id, currentVideoRequestHeader);
            } catch (Exception ex) {
                Logger.printException(() -> "buildRequest failure", ex);
            }
        }
    }

    @Nullable
    public static byte[] getStreamingData(String videoId) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                StreamOrDetailsDataRequest request = StreamOrDetailsDataRequest.getStreamRequestForVideoId(videoId);
                if (request != null) {
                    var stream = (byte[]) request.getStreamDetails();
                    if (stream != null) {
                        Logger.printDebug(() -> "Overriding video stream: " + videoId);
                        return stream;
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "getStreamingData failure", ex);
            }
        }
        return null;
    }

    public static StreamOrDetailsDataRequest fetchDetails(Route.CompiledRoute videoDetailsEndpoint, String videoId) {
        return StreamOrDetailsDataRequest.getDetailsRequest(videoDetailsEndpoint, videoId, currentVideoRequestHeader);
    }

    @Nullable
    public static byte[] removeVideoPlaybackPostBody(Uri uri, int method, byte[] postData) {
        if (SPOOF_VIDEO_STREAMS) {
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

    public static String appendSpoofedClient(String videoFormat) {
        try {
            if (SPOOF_VIDEO_STREAMS && SharedYouTubeSettings.SPOOF_VIDEO_STREAMS_STATS_FOR_NERDS.get()
                    && !TextUtils.isEmpty(videoFormat)) {
                return "\u202D" + videoFormat + "\u2009("
                        + StreamOrDetailsDataRequest.getLastSpoofedClientName() + ")";
            }
        } catch (Exception ex) {
            Logger.printException(() -> "appendSpoofedClient failure", ex);
        }
        return videoFormat;
    }

    public static void setAccountIdentity(@Nullable String newlyPageIDHeaderValue, boolean newlyLoadedIncognitoStatus) {
        if (newlyPageIDHeaderValue != null) {
            var newlyPageIDHeaderEmpty = newlyPageIDHeaderValue.isEmpty();
            pageIDHeaderValue = newlyPageIDHeaderEmpty ? "" : newlyPageIDHeaderValue;
            if (!newlyPageIDHeaderEmpty) {
                Logger.printDebug(() -> "new PageID Header value loaded: " + newlyPageIDHeaderValue);
            }
        }
    }
}
