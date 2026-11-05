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
 *
 * Modified by lavinhoque33, 2026-10-13.
 */

package app.morphe.extension.shared.spoof;

import android.net.Uri;
import java.nio.ByteBuffer;
import app.morphe.extension.shared.patches.MediaHttpRequest;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import app.morphe.extension.shared.requests.Route;
import app.morphe.extension.shared.patches.PatchStatus;
import app.morphe.extension.shared.settings.AppLanguage;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.spoof.requests.StreamOrDetailsDataRequest;
import app.morphe.extension.shared.spoof.requests.VisitorIdRequester;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class SpoofVideoStreamsPatch {
    private static final String STREAM_URL_PATH = "/videoplayback";

    /**
     * Injection point.
     * <p>
     * Called off the main thread when native code starts a media request.
     * The app makes the requests of SABR streams itself, and names itself and the device it runs on,
     * even when the streams are from a TV client. These requests name the TV client instead.
     *
     * @param callbacks Native callbacks of the request, which are also given the response.
     * @return The request to start.
     */
    public static MediaHttpRequest onMediaRequest(MediaHttpRequest request, Object callbacks) {
        if (!SPOOF_VIDEO_STREAMS) {
            return request;
        }
        try {
            String url = request.patch_getUrl();
            if (url == null || !url.contains(STREAM_URL_PATH)) {
                return request;
            }
            Uri uri = Uri.parse(url);
            if (!STREAM_URL_PATH.equals(uri.getPath()) || uri.getQueryParameter("sabr") == null) {
                return request;
            }

            String urlClient = uri.getQueryParameter("c");
            MediaHttpRequest requestToStart = request;
            byte[] body = request.patch_getBody();
            // The other TV clients that use SABR are the same client on another device.
            if (body != null && ClientType.TV_SABR.clientName.equals(urlClient)) {
                byte[] clientInfo = SabrRequestBody.buildClientInfo(ClientType.TV_SABR, getLocaleOverride());
                requestToStart = request.patch_withBody(SabrRequestBody.replaceClientInfo(body, clientInfo));
            }

            if (SabrMediaLogger.isEnabled()) {
                SabrMediaLogger.onRequest(urlClient, requestToStart.patch_getBody(), callbacks);
            }
            return requestToStart;
        } catch (Exception ex) {
            Logger.printException(() -> "onMediaRequest failure", ex);
            return request;
        }
    }

    /**
     * Injection point.
     * Called with each chunk of a media response, before native code reads it.
     */
    public static void onMediaResponseChunk(Object callbacks, @Nullable ByteBuffer chunk) {
        if (!SPOOF_VIDEO_STREAMS || chunk == null) {
            return;
        }
        try {
            SabrMediaLogger.onResponseChunk(callbacks, chunk);
        } catch (Exception ex) {
            Logger.printException(() -> "onMediaResponseChunk failure", ex);
        }
    }

    public static volatile Map<String, String> currentVideoRequestHeader;
    public static String pageIDHeaderValue = "";

    public static final class SpoofVideoStreamsAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return !PatchStatus.PoTokenProvider() || !SharedYouTubeSettings.POTOKEN_PROVIDER.get();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(SharedYouTubeSettings.POTOKEN_PROVIDER);
        }
    }

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

    private static final boolean SPOOF_VIDEO_STREAMS = isPatchIncluded() && SharedYouTubeSettings.SPOOF_VIDEO_STREAMS.get();

    @NonNull
    private static volatile Locale localeOverride = AppLanguage.DEFAULT.getLocale();

    private static final String INNERTUBE_PATH = "/youtubei/";

    /**
     * Path of the request that loads a video, which holds a player request and a watch next request.
     */
    private static final String WATCH_REQUEST_PATH = "get_watch";

    /**
     * Path of the request for what is shown below the player. The app sends it when it loads a video,
     * also when the player response comes with a request that is not seen, such as the Onesie request.
     */
    private static final String WATCH_NEXT_REQUEST_PATH = "/next";

    /**
     * Path of the request that loads a Short, which holds a player request.
     */
    private static final String REEL_WATCH_REQUEST_PATH = "reel_item_watch";

    private static volatile ClientType preferredClient = ClientType.VISIONOS_1_02;

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

    // Force AVC support, strips VP9 formats on SABR (Morphe bb3e3ff0).
    private static volatile boolean forceAVC;

    /**
     * Injection point (called by the app wrapper).
     */
    public static void setForceAVC(boolean force) {
        forceAVC = force;
    }

    public static boolean getForceAVC() {
        return forceAVC;
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

    public static boolean isSpoofingEnabled() {
        return SPOOF_VIDEO_STREAMS;
    }

    public static boolean fixHLSCurrentTime(boolean original) {
        if (SPOOF_VIDEO_STREAMS) {
            return false;
        }
        return original;
    }

    /**
     * Injection point.
     * Called when the player is set up for a video, after its streams were given by {@link #getStreamingData}.
     * Fix audio stuttering in YouTube Music.
     */
    public static boolean disableSABR(@Nullable String videoId) {
        if (!SPOOF_VIDEO_STREAMS) {
            return false;
        }
        final boolean useSABR = StreamOrDetailsDataRequest.playerClientUsesSABR(videoId);
        Logger.printDebug(() -> "Player uses SABR: " + useSABR + " for: " + videoId);
        return !useSABR;
    }

    /**
     * @return If SABR is turned off for the streams last given to the player.
     */
    public static boolean disableSABR() {
        return SPOOF_VIDEO_STREAMS && !StreamOrDetailsDataRequest.getLastSpoofedClientUseSABR();
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

    /**
     * Injection point.
     * Starts fetching the streams of a video when the app requests the video, so both requests run at the same time.
     * The app can also load a video with a request that is not seen here, and then the streams are fetched
     * when the app builds the streams of the video.
     *
     * @param body Body of the request, or null if it has none.
     */
    public static void fetchStreams(String url, Map<String, String> requestHeaders, @Nullable byte[] body) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                if (!Utils.isNetworkConnected()) {
                    return;
                }

                Uri uri = Uri.parse(url);
                String path = uri.getPath();
                if (path == null || !path.startsWith(INNERTUBE_PATH)) {
                    return;
                }
                StreamOrDetailsDataRequest.setAppHeaders(requestHeaders);
                currentVideoRequestHeader = requestHeaders;

                // The app can also send a player request for the same video,
                // so these start a fetch only if there is none that can still be used.
                final boolean isReelWatchRequest = path.contains(REEL_WATCH_REQUEST_PATH);
                if (isReelWatchRequest || path.endsWith(WATCH_NEXT_REQUEST_PATH)) {
                    // Requests for more comments or related videos have no video id.
                    String id = body == null ? null : InnerTubeRequestBody.readVideoId(body, isReelWatchRequest);
                    if (id != null) {
                        StreamOrDetailsDataRequest.fetchRequestIfNeeded(id);
                    }
                    return;
                }

                final boolean isWatchRequest = path.contains(WATCH_REQUEST_PATH);
                if (!isWatchRequest && !path.contains("player")) {
                    return;
                }

                // 'get_drm_license' has no video ID and appears to happen when waiting for a paid video to start.
                // 'heartbeat' has no video and appears to be only after playback has started.
                // 'refresh' has no video ID and appears to happen when waiting for a live stream to start.
                // 'ad_break' has no video ID.
                if (path.contains("get_drm_license") ||
                        path.contains("heartbeat") ||
                        path.contains("refresh") ||
                        path.contains("ad_break")) {
                    Logger.printDebug(() -> "Ignoring path: " + path);
                    return;
                }

                String urlId = uri.getQueryParameter("id");
                // The url of a watch request has no video id.
                final String id = urlId == null && body != null
                        ? InnerTubeRequestBody.readVideoId(body, isWatchRequest)
                        : urlId;
                if (id == null) {
                    Logger.printException(() -> "Ignoring request with no ID: " + url);
                    return;
                }

                StreamOrDetailsDataRequest.fetchStreamRequest(id, requestHeaders);
            } catch (Exception ex) {
                Logger.printException(() -> "fetchStreams failure", ex);
            }
        }
    }

    @Nullable
    public static byte[] getStreamingData(String videoId) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                StreamOrDetailsDataRequest request = StreamOrDetailsDataRequest.getOrFetchRequest(videoId);
                var buffers = request == null ? null
                        : (StreamOrDetailsDataRequest.StreamData) request.getStreamDetails();
                // The player chooses SABR from the client of the streams it is given for this video.
                StreamOrDetailsDataRequest.setPlayerClient(videoId, buffers == null ? null : buffers.client());
                if (request != null) {
                    if (buffers != null) {
                        byte[] stream = buffers.streamingData();
                        Logger.printInfo(() -> "SpoofVideoStreams: Overriding video stream: " + videoId);
                        return stream;
                    } else {
                        Logger.printWarn(() -> "SpoofVideoStreams: Stream buffers null or fetch failed for: " + videoId);
                    }
                } else {
                    Logger.printWarn(() -> "SpoofVideoStreams: StreamRequest not found in cache for: " + videoId);
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
    public static byte[] getPlayerConfig(String videoId, @Nullable byte[] appPlayerConfig) {
        if (SPOOF_VIDEO_STREAMS) {
            try {
                StreamOrDetailsDataRequest request = StreamOrDetailsDataRequest.getStreamRequestForVideoId(videoId);
                if (request != null) {
                    var buffers = (StreamOrDetailsDataRequest.StreamData) request.getStreamDetails();
                    if (buffers != null) {
                        byte[] config = buffers.playerConfig();
                        if (config != null) {
                            Logger.printDebug(() -> "Overriding player config: " + videoId);
                            // This config replaces the whole config of the app, which tells how the
                            // video is played inline, such as a muted preview in the feed.
                            return buffers.hasAndroidMedia() && appPlayerConfig != null
                                    ? StreamOrDetailsDataRequest.addInlinePlayback(config, appPlayerConfig)
                                    : config;
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
     * Called after {@link #getPlayerConfig(String, byte[])}.
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

    /**
     * Injection point.
     * Called when a media response asks the player to load the player response of its video again.
     * The app then loads the video again, which can be with requests that are not seen,
     * and the streams fetched before are not given to the player again.
     */
    public static void onReloadPlayerResponse() {
        if (SPOOF_VIDEO_STREAMS) {
            Logger.printDebug(() -> "Player response reload requested");
            StreamOrDetailsDataRequest.onPlayerResponseReloadRequested();
        }
    }
}
