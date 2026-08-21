/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.shared.spoof;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import app.morphe.extension.shared.requests.Route;
import app.morphe.extension.shared.settings.AppLanguage;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.spoof.requests.StreamOrDetailsDataRequest;
import app.morphe.extension.shared.spoof.requests.VisitorIdRequester;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

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

    private static final String INTERNET_CONNECTION_CHECK_URI_STRING = "https://www.google.com/gen_204";
    private static final Uri INTERNET_CONNECTION_CHECK_URI = Uri.parse(INTERNET_CONNECTION_CHECK_URI_STRING);

    private static final boolean SPOOF_VIDEO_STREAMS = isPatchIncluded() && SharedYouTubeSettings.SPOOF_VIDEO_STREAMS.get();

    @NonNull
    private static volatile Locale localeOverride = AppLanguage.DEFAULT.getLocale();

    private static volatile ClientType preferredClient = ClientType.VISIONOS_1_02;

    /**
     * Older YouTube Music versions use {@code get_watch} instead of a player URL
     * containing the video id. The id is held until that request reaches the
     * request builder, where its authentication headers are available.
     */
    private static final AtomicReference<String> pendingLegacyVideoId = new AtomicReference<>("");

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

    @NonNull
    public static Locale getLocaleOverride() {
        return localeOverride;
    }

    /**
     * @param locale Locale override for non-authenticated requests.
     */
    public static void setLocaleOverride(@Nullable Locale locale) {
        if (locale != null) {
            localeOverride = locale;
        }
    }

    public static void setClientsToUse(List<ClientType> availableClients, ClientType client) {
        preferredClient = Objects.requireNonNull(client);

        if (SPOOF_VIDEO_STREAMS) {
            StreamOrDetailsDataRequest.setClientOrderToUse(availableClients, client);

            // Prefetch visitorId for default client.
            Utils.runOnBackgroundThread(() -> VisitorIdRequester.getVisitorId(client));
        }
    }

    public static ClientType getPreferredClient() {
        return preferredClient;
    }

    public static boolean spoofingToClientWithNoMultiAudioStreams() {
        return SPOOF_VIDEO_STREAMS && !preferredClient.supportsMultiAudioTracks;
    }

    public static boolean spoofingToClientWithSABROrSpoofingDisabled() {
        return !SPOOF_VIDEO_STREAMS || preferredClient.requireSABR;
    }

    /**
     * Injection point for legacy YouTube Music player responses.
     */
    public static void setPendingLegacyVideoId(@NonNull String videoId) {
        if (SPOOF_VIDEO_STREAMS) {
            pendingLegacyVideoId.set(videoId);
        }
    }

    public static Uri blockGetWatchRequest(Uri playerRequestUri) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                String path = playerRequestUri.getPath();

                if (path != null && path.contains("get_watch")) {
                    if (!TextUtils.isEmpty(pendingLegacyVideoId.get())) {
                        return playerRequestUri;
                    }
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
                Uri playerRequestUri = playerRequestBuilder.build();
                String path = playerRequestUri.getPath();

                if (path != null && path.contains("get_watch")) {
                    if (!TextUtils.isEmpty(pendingLegacyVideoId.get())) {
                        return playerRequestBuilder;
                    }
                    Logger.printDebug(() -> "Blocking 'get_watch' by returning internet connection check URI");
                    return INTERNET_CONNECTION_CHECK_URI.buildUpon();
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockGetWatchRequest failure", ex);
            }
        }
        return playerRequestBuilder;
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
        if (SPOOF_VIDEO_STREAMS) {
            return false;
        }
        return original;
    }

    public static boolean disableSABR() {
        return SPOOF_VIDEO_STREAMS && !StreamOrDetailsDataRequest.getLastSpoofedClientUseSABR();
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
                if (path != null && path.contains("get_watch")) {
                    String legacyVideoId = pendingLegacyVideoId.getAndSet("");
                    if (!TextUtils.isEmpty(legacyVideoId)) {
                        currentVideoRequestHeader = requestHeaders;
                        StreamOrDetailsDataRequest.fetchStreamRequest(
                                legacyVideoId, false, currentVideoRequestHeader);
                    }
                    return;
                }
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
                boolean isInline = "1".equals(uri.getQueryParameter("inline"));

                currentVideoRequestHeader = requestHeaders;
                StreamOrDetailsDataRequest.fetchStreamRequest(id, isInline, currentVideoRequestHeader);
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
                    var buffers = (StreamOrDetailsDataRequest.StreamData) request.getStreamDetails();
                    if (buffers != null) {
                        byte[] stream = buffers.streamingData();
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

    /**
     * Injection point.
     * Fix playback by replace the player config.
     * Called after {@link #getStreamingData(String)}.
     */
    @Nullable
    public static byte[] getPlayerConfig(String videoId) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                StreamOrDetailsDataRequest request = StreamOrDetailsDataRequest.getStreamRequestForVideoId(videoId);
                if (request != null) {
                    var buffers = (StreamOrDetailsDataRequest.StreamData) request.getStreamDetails();
                    if (buffers != null) {
                        byte[] config = buffers.playerConfig();
                        if (config != null) {
                            Logger.printDebug(() -> "Overriding player config: " + videoId);
                            return config;
                        }
                    }
                }

                Logger.printDebug(() -> "Not overriding player config: " + videoId);
            } catch (Exception ex) {
                Logger.printException(() -> "getPlayerConfig failure", ex);
            }
        }

        return null;
    }

    /**
     * Injection point.
     * Called after {@link #getPlayerConfig(String)}.
     */
    public static boolean hasAndroidMedia(String videoId) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                StreamOrDetailsDataRequest request = StreamOrDetailsDataRequest.getStreamRequestForVideoId(videoId);
                if (request != null) {
                    var buffers = (StreamOrDetailsDataRequest.StreamData) request.getStreamDetails();
                    if (buffers != null) {
                        return buffers.hasAndroidMedia();
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "hasAndroidMedia failure", ex);
            }
        }

        return false;
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
