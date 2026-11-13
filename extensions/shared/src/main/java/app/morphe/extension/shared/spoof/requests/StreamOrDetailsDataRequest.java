/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 *
 * Modified by lavinhoque33, 2026-10-21.
 */

package app.morphe.extension.shared.spoof.requests;

import static app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch.pageIDHeaderValue;
import static app.morphe.extension.shared.spoof.js.JavaScriptEngineSupport.supportsJavaScriptEngine;
import static app.morphe.extension.shared.spoof.js.JavaScriptManager.getDeobfuscatedStreamingData;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.submitOnBackgroundThread;

import app.morphe.extension.shared.spoof.js.JavaScriptManager;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.protobuf.InvalidProtocolBufferException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.AndroidMedialibConfig;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.ExoPlayerConfig;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.MediaCommonConfig;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.AudioConfig;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.Format;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.InlinePlaybackConfig;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayabilityStatus;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayerConfig;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayerGestureConfig;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayerResponse;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.StreamingData;
import app.morphe.extension.shared.innertube.ReelItemWatchResponseOuterClass.ReelItemWatchResponse;
import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.shared.oauth2.requests.OAuth2Requester;
import app.morphe.extension.shared.requests.Route;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.spoof.ClientType;
import app.morphe.extension.shared.spoof.PreferredClientCheck;
import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch;
import app.morphe.extension.shared.spoof.potoken.PoTokenManager;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

public class StreamOrDetailsDataRequest {

    /**
     * @param client Client the streams are of.
     */
    public record StreamData(byte[] streamingData, @Nullable byte[] playerConfig, boolean hasAndroidMedia,
                             ClientType client) {
    }

    public static boolean getLastSpoofedClientUseSABR() {
        ClientType client = lastSpoofedClientType;
        return client != null && client.requireSABR;
    }

    private static volatile ClientType[] clientStreamOrderToUse =
            Arrays.stream(ClientType.values())
                    .filter(client -> client.usePlayerEndpoint)
                    .toArray(ClientType[]::new);

    public static void setClientOrderToUse(List<ClientType> availableClients, ClientType preferredClient) {
        List<ClientType> orderToUse = buildClientOrder(availableClients, preferredClient);

        clientStreamOrderToUse = orderToUse.toArray(new ClientType[0]);
        Logger.printDebug(() -> "Available spoof clients: " + orderToUse);
    }

    /**
     * Prepares what a fetch of each client waits for, so the first videos do not wait for it:
     * the JavaScript engine, the access token of a client the user signed in to, and the visitor id.
     * The clients after the first one are prepared as well, because a video that the first client
     * cannot play waits for the next client.
     */
    public static void prepareClients() {
        ClientType[] clientOrder = clientStreamOrderToUse;
        // The engine starts on its own thread, and the requests below do not delay it.
        for (ClientType client : clientOrder) {
            if (client.requireJS && !skipClient(client)) {
                JavaScriptManager.warmUpInBackground();
                break;
            }
        }

        // Without a network, the fetch of a video gets the token and the visitor ids.
        if (!Utils.isNetworkConnected()) {
            return;
        }

        for (ClientType client : clientOrder) {
            if (skipClient(client)) {
                continue;
            }
            // Without an access token, the client is skipped.
            // A connection error is not shown here, because the network may not be ready yet
            // when the app starts, and the fetch of a video gets the token again.
            if (client.supportsOAuth2 && client.requireLogin
                    && !Utils.isNotEmpty(OAuth2Requester.getAndUpdateAccessTokenIfNeeded(false))) {
                continue;
            }
            VisitorIdRequester.getVisitorId(client);
        }
    }

    private static boolean skipClient(ClientType client) {
        if (client.requireJS && !supportsJavaScriptEngine()) {
            Logger.printDebug(() -> "Skipping JavaScript client: " + client.name());
            return true;
        }
        return false;
    }

    // public, the Music downloader tries the clients one by one.
    public static List<ClientType> buildClientOrder(List<ClientType> availableClients,
                                                    ClientType preferredClient) {
        Objects.requireNonNull(preferredClient);

        List<ClientType> orderToUse = new ArrayList<>(availableClients.size());
        orderToUse.add(preferredClient);
        // Wii U TV fallback (Morphe 5c6b8418).
        // YouTube serves each TV device separately, so another one can still play when the first is cut off.
        if (preferredClient == ClientType.TV_SABR) {
            orderToUse.add(ClientType.TV_SABR_WII_U);
        }

        for (ClientType client : availableClients) {
            if (client.requireJS && !supportsJavaScriptEngine()) {
                Logger.printDebug(() -> "Could not find JavaScript engine. Skipping JavaScript client: " + client.name());
                continue;
            }

            if (client != preferredClient) {
                orderToUse.add(client);
            }
        }

        return orderToUse;
    }

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String PAGE_ID_HEADER = "X-Goog-PageId";
    private static final String API_FORMAT_VERSION_HEADER = "X-GOOG-API-FORMAT-VERSION";
    private static final String VISITOR_ID_HEADER = "X-Goog-Visitor-Id";

    private static final int HTTP_TIMEOUT_MILLISECONDS = 10 * 1000;
    private static final int MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000;

    private static final Map<String, StreamOrDetailsDataRequest> streamCache = Collections.synchronizedMap(
            Utils.createSizeRestrictedMap(50));

    private static final Map<String, StreamOrDetailsDataRequest> detailsCache = Collections.synchronizedMap(
            Utils.createSizeRestrictedMap(50));

    /**
     * Client of the streams last given to the player.
     * Several videos are fetched at once, such as the inline previews of the search results
     * or the next track of the queue, so the last fetch to finish can be of another video.
     */
    private static volatile ClientType lastSpoofedClientType;

    /**
     * Clients of the streams given to the player, by video id.
     * The streams of another video can be given before the player is set up for a video,
     * so the player chooses SABR from the client of that video and not from the last one.
     */
    private static final Map<String, ClientType> playerClients = Collections.synchronizedMap(
            Utils.createSizeRestrictedMap(50));

    /**
     * If the last TV SABR response built on the thread is of a live stream, which is fetched again with TV DASH.
     * A fetch runs on a single thread, and other fetches running at the same time must not see this.
     */
    private static final ThreadLocal<Boolean> fallbackWithTVDashOnThread = ThreadLocal.withInitial(() -> false);

    /**
     * If the last visionOS response built on the thread is of a visitor id in the attestation A/B test,
     * so the client is fetched again with another visitor id.
     * A fetch runs on a single thread, and other fetches running at the same time must not see this.
     */
    private static final ThreadLocal<Boolean> newVisitorIdOnThread = ThreadLocal.withInitial(() -> false);

    /**
     * Fetches of a visionOS client, each with another visitor id, before the next client is used.
     */
    private static final int VISITOR_ID_ATTEMPTS = 3;
    private static volatile boolean authHeadersOverrides;
    private static volatile Map<String, String> lastPlayerHeaders = Collections.emptyMap();

    public static String getLastSpoofedClientName() {
        ClientType client = lastSpoofedClientType;
        if (client == null) {
            return "Unknown";
        } else {
            String clientName = client.friendlyName;
            if (client.supportsOAuth2 && authHeadersOverrides) {
                clientName += " Signed in";
            }
            return clientName;
        }
    }

    /**
     * @param client Client of the streams given to the player for the video,
     *               or null if the player uses the streams of the app.
     */
    public static void setPlayerClient(String videoId, @Nullable ClientType client) {
        lastSpoofedClientType = client;
        if (client == null) {
            playerClients.remove(videoId);
        } else {
            playerClients.put(videoId, client);
        }
    }

    /**
     * @return If the streams given to the player for the video are SABR streams.
     */
    public static boolean playerClientUsesSABR(@Nullable String videoId) {
        ClientType client = videoId == null ? null : playerClients.get(videoId);
        return client != null && client.requireSABR;
    }

    /**
     * How long fetched streams are given to the player of a video that is loaded again without a request
     * that is seen. Streams expire some hours after they are fetched.
     */
    private static final long MAX_MILLISECONDS_TO_REUSE_STREAMS = 60 * 60 * 1000;

    /**
     * How long a fetch that found no streams is used for a video that is loaded again without a request
     * that is seen. The app can build the streams of the same response more than once.
     */
    private static final long MAX_MILLISECONDS_TO_REUSE_FAILED_FETCH = 30 * 1000;

    private static final AtomicInteger playerResponseReloadCount = new AtomicInteger();
    private final long fetchStartTime = System.currentTimeMillis();
    private final int playerResponseReloadCountAtFetch = playerResponseReloadCount.get();

    private final String videoId;
    private final boolean isInline;
    @Nullable
    private final String authorization;
    private final ClientType[] clientOrder;

    private final Future<Object> future;

    private StreamOrDetailsDataRequest(@Nullable Route.CompiledRoute endpoint,
                                       String videoId, boolean isInline, Map<String, String> playerHeaders) {
        this(endpoint, videoId, isInline, playerHeaders, null, false);
    }

    private StreamOrDetailsDataRequest(@Nullable Route.CompiledRoute endpoint,
                                       String videoId, boolean isInline, Map<String, String> playerHeaders,
                                       @Nullable ClientType[] clientStreamOrderOverride) {
        this(endpoint, videoId, isInline, playerHeaders, clientStreamOrderOverride, false);
    }

    private StreamOrDetailsDataRequest(@Nullable Route.CompiledRoute endpoint,
                                       String videoId, boolean isInline, Map<String, String> playerHeaders,
                                       @Nullable ClientType[] clientStreamOrderOverride,
                                       boolean forDownload) {
        if (endpoint == null) {
            Objects.requireNonNull(playerHeaders);
        }
        this.videoId = videoId;
        this.isInline = isInline;
        this.authorization = playerHeaders == null ? null : playerHeaders.get(AUTHORIZATION_HEADER);
        this.clientOrder = clientStreamOrderOverride;

        this.future = submitOnBackgroundThread(() ->
                fetch(endpoint, videoId, isInline, playerHeaders, clientStreamOrderOverride, forDownload));
    }

    /**
     * Fetches one download candidate without replacing the playback cache or selecting a
     * playback client. The caller owns fallback because a valid URL can still return HTTP 403.
     */
    @Nullable
    public static StreamData fetchDownloadStream(String videoId, ClientType clientType,
                                                 Map<String, String> playerHeaders) {
        HttpURLConnection connection = send(clientType, videoId, playerHeaders, false, true);
        try {
            newVisitorIdOnThread.set(false);
            Object response = buildPlayerStreamOrDetailsResponse(clientType, connection, videoId, false, true);
            for (int attempt = 1; attempt < VISITOR_ID_ATTEMPTS && Boolean.TRUE.equals(newVisitorIdOnThread.get()); attempt++) {
                newVisitorIdOnThread.set(false);
                if (connection != null) connection.disconnect();
                connection = send(clientType, videoId, playerHeaders, false, true);
                response = buildPlayerStreamOrDetailsResponse(clientType, connection, videoId, false, true);
            }
            return response instanceof StreamData ? (StreamData) response : null;
        } finally {
            newVisitorIdOnThread.set(false);
            if (connection != null) connection.disconnect();
        }
    }

    public static void fetchStreamRequest(String videoId, Map<String, String> fetchHeaders) {
        fetchStreamRequest(videoId, false, fetchHeaders);
    }

    public static void fetchStreamRequest(String videoId, boolean isInline, Map<String, String> fetchHeaders) {
        fetchStreamRequest(videoId, isInline, fetchHeaders, (ClientType[]) null);
    }

    public static void fetchStreamRequest(String videoId, boolean isInline, Map<String, String> fetchHeaders,
                                          @Nullable ClientType... clientStreamOrderOverride) {
        if (fetchHeaders != null && !fetchHeaders.isEmpty()) {
            lastPlayerHeaders = fetchHeaders;
        }
        ClientType[] clientOrder = clientStreamOrderOverride == null || clientStreamOrderOverride.length == 0
                ? clientStreamOrderToUse : clientStreamOrderOverride;
        synchronized (streamCache) {
            // The app can ask for the same video again while the first fetch is still running.
            // A fetch that is done is never reused, because its streams can be expired or missing.
            StreamOrDetailsDataRequest existing = streamCache.get(videoId);
            if (existing != null && existing.isFetchInProgress(isInline, fetchHeaders, clientOrder)) {
                Logger.printDebug(() -> "Using the fetch in progress for: " + videoId);
                return;
            }
            streamCache.put(videoId, new StreamOrDetailsDataRequest(null, videoId, isInline, fetchHeaders,
                    clientOrder));
        }
    }

    private boolean isFetchInProgress(boolean isInline, Map<String, String> playerHeaders,
                                      ClientType[] clientOrder) {
        return !future.isDone()
                && this.clientOrder == clientOrder
                && Objects.equals(authorization, playerHeaders.get(AUTHORIZATION_HEADER));
    }

    /**
     * Uses headers captured during playback so a song can be downloaded without first playing it.
     * Deliberately not cached, so downloads cannot evict the streams of videos being watched.
     * <p>
     * The clients are given by the caller instead of taken from the playback order, which stays at
     * the enum default while spoofing is off and then begins with a SABR client that has no urls.
     *
     * @param preferredClient Tried first, unless it is a SABR client.
     */
    public static StreamOrDetailsDataRequest fetchRequestForDownload(String videoId,
                                                                     List<ClientType> downloadClients,
                                                                     ClientType preferredClient) {
        List<ClientType> clients = buildClientOrder(downloadClients, preferredClient);
        // SABR clients answer without the format URLs a download needs,
        // so the next client would never be tried.
        clients.removeIf(client -> client.requireSABR);
        ClientType[] clientOrder = clients.toArray(new ClientType[0]);
        // The video details name the saved file, so the download asks for them as well.
        // The auth hook keeps the headers even if spoofing is off, but it is not used by all apps.
        Map<String, String> headers = AuthUtils.getAuthorization().isEmpty()
                ? lastPlayerHeaders
                : AuthUtils.getRequestHeader();
        return new StreamOrDetailsDataRequest(null, videoId, false, headers, clientOrder, true);
    }

    @Nullable
    public static StreamOrDetailsDataRequest getStreamRequestForVideoId(String videoId) {
        return streamCache.get(videoId);
    }

    @Nullable
    public static StreamOrDetailsDataRequest getRequestForVideoId(String videoId) {
        return getStreamRequestForVideoId(videoId);
    }

    public static StreamOrDetailsDataRequest getDetailsRequest(Route.CompiledRoute videoDetailsEndpoint,
                                                               String videoId, Map<String, String> fetchHeaders) {
        StreamOrDetailsDataRequest request = new StreamOrDetailsDataRequest(videoDetailsEndpoint, videoId, false, fetchHeaders);
        detailsCache.put(videoId, request);
        return request;
    }

    private static void handleConnectionError(String toastMessage, @Nullable Exception ex, boolean showToast) {
        if (showToast) Utils.showToastShort(toastMessage);
        Logger.printInfo(() -> toastMessage, ex);
    }

    private static void handleDebugToast(String toastMessage, ClientType clientType) {
        if (BaseSettings.DEBUG.get() && BaseSettings.DEBUG_TOAST_ON_ERROR.get()) {
            Utils.showToastShort(String.format(toastMessage, clientType));
        }
    }

    @Nullable
    private static HttpURLConnection send(@Nullable ClientType clientType,
                                          @Nullable String videoId,
                                          Map<String, String> playerHeaders,
                                          boolean showErrorToasts) {
        return send(clientType, videoId, playerHeaders, showErrorToasts, false);
    }

    @Nullable
    private static HttpURLConnection send(@Nullable ClientType clientType,
                                          @Nullable String videoId,
                                          Map<String, String> playerHeaders,
                                          boolean showErrorToasts, boolean forDownload) {
        Objects.requireNonNull(clientType);
        Objects.requireNonNull(videoId);

        final boolean isStream = clientType != ClientType.GET_CHANNEL_FROM_ID && clientType != ClientType.SAVE_TO_WATCH_LATER;

        try {
            HttpURLConnection connection = PlayerRoutes.getPlayerResponseConnectionFromRoute(clientType, forDownload);
            connection.setConnectTimeout(HTTP_TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(HTTP_TIMEOUT_MILLISECONDS);

            if (!forDownload) authHeadersOverrides = false;

            String visitorId = "";
            if (isStream) {
                String authorization = playerHeaders.get(AUTHORIZATION_HEADER);
                boolean authHeadersIncludes = Utils.isNotEmpty(authorization);

                // Auth header is required, but the user is not logged in. These clients are skipped:
                // ANDROID_CREATOR, ANDROID_MUSIC_REEL, ANDROID_MUSIC_NO_SDK.
                if (clientType.canLogin && clientType.requireLogin && !authHeadersIncludes) {
                    Logger.printDebug(() -> "Skipping client since user is not logged in: " + clientType
                            + ", videoId: " + videoId);
                    return null;
                }
                // If the Bearer token is compatible and the user is logged in, the header is set:
                // ANDROID_CREATOR, ANDROID_MUSIC_REEL, ANDROID_MUSIC_NO_SDK, TV_SABR, TV_SIMPLY.
                else if (clientType.canLogin && authHeadersIncludes) {
                    connection.setRequestProperty(AUTHORIZATION_HEADER, authorization);
                    Logger.printDebug(() -> "Set auth header: " + clientType + ", videoId: " + videoId);
                }
                // If oauth2 login is supported and the user is logged in via oauth2 flow, the header is set:
                // ANDROID_VR (ANDROID_XR).
                else if (clientType.supportsOAuth2 && clientType.requireLogin) {
                    String oauth2Authorization = OAuth2Requester.getAndUpdateAccessTokenIfNeeded(true);
                    if (Utils.isNotEmpty(oauth2Authorization)) {
                        if (!forDownload) authHeadersOverrides = true;
                        connection.setRequestProperty(AUTHORIZATION_HEADER, oauth2Authorization);
                        Logger.printDebug(() -> "Set oauth2 auth header: " + clientType + ", videoId: " + videoId);
                    }
                    // Oauth2 login is required, but the user is not logged in.
                    // ANDROID_VR (ANDROID_XR).
                    else {
                        Logger.printDebug(() -> "Skipping client since user is not signed in to " + clientType
                                + ", videoId: " + videoId);
                        return null;
                    }
                }
                // These clients can play videos without the auth header:
                // TV_SABR, TV_SIMPLY, VISIONOS_1_02 (VISIONOS_1_03), VISIONOS_1_06.
                else {
                    Logger.printDebug(() -> "Do not set auth header: " + clientType + ", videoId: " + videoId);
                }

                Logger.printDebug(() -> "Fetching video stream for: " + videoId + " using client: " + clientType);

                // Using the same visitorId across multiple clients increases the bot score.
                // To prevent this, each client uses a different visitorId.
                // See: https://github.com/MorpheApp/morphe-patches/issues/2283.
                visitorId = VisitorIdRequester.getVisitorId(clientType);
                if (Utils.isNotEmpty(visitorId)) {
                    connection.setRequestProperty(VISITOR_ID_HEADER, visitorId);
                } else {
                    // A few requests without visitorId are okay, but if repeated excessively, increase the bot score.
                    Logger.printDebug(() -> "Do not set visitorId: " + clientType + ", videoId: " + videoId);
                }

                // Only 'X-GOOG-API-FORMAT-VERSION = 2' can have a proto response.
                connection.setRequestProperty(API_FORMAT_VERSION_HEADER, "2");
            } else if (playerHeaders != null) {
                String authorization = playerHeaders.get(AUTHORIZATION_HEADER);
                if (authorization != null) {
                    connection.setRequestProperty(AUTHORIZATION_HEADER, authorization);
                    if (!pageIDHeaderValue.isEmpty()) {
                        connection.setRequestProperty(PAGE_ID_HEADER, pageIDHeaderValue);
                    }
                }
            }

            String innerTubeBody = PlayerRoutes.createInnertubeBody(clientType, videoId, visitorId);
            byte[] requestBody = innerTubeBody.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(requestBody.length);
            connection.getOutputStream().write(requestBody);

            final int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) return connection;

            if (isStream) {
                handleConnectionError("Playback error " + clientType + ": " + responseCode + " " + connection.getResponseMessage(), null, showErrorToasts);
            }
        } catch (SocketTimeoutException ex) {
            handleConnectionError("Connection timeout", ex, showErrorToasts);
        } catch (IOException ex) {
            handleConnectionError("Network error", ex, showErrorToasts);
        } catch (Exception ex) {
            Logger.printException(() -> "send failed", ex);
        }

        return null;
    }

    @Nullable
    private static Object buildPlayerStreamOrDetailsResponse(@Nullable ClientType clientType,
                                                             @Nullable HttpURLConnection connection,
                                                             String videoId,
                                                             boolean isInline) {
        return buildPlayerStreamOrDetailsResponse(clientType, connection, videoId, isInline, false);
    }

    @Nullable
    private static Object buildPlayerStreamOrDetailsResponse(@Nullable ClientType clientType,
                                                             @Nullable HttpURLConnection connection,
                                                             String videoId,
                                                             boolean isInline, boolean forDownload) {
        if (connection == null) {
            return null;
        }
        Objects.requireNonNull(clientType);
        final boolean returnStreamObject = clientType != ClientType.GET_CHANNEL_FROM_ID
                && clientType != ClientType.SAVE_TO_WATCH_LATER;

        if (connection.getContentLength() == 0) {
            handleDebugToast(String.format("Debug: Ignoring empty %s client (%%s)", returnStreamObject ? "spoof stream" : "get details"), clientType);
            return null;
        }

        try (InputStream inputStream = connection.getInputStream()) {
            if (returnStreamObject) {
                PlayerResponse playerResponse;
                if (clientType.usePlayerEndpoint) {
                    playerResponse = PlayerResponse.parseFrom(inputStream);
                    VisitorIdRequester.updateVisitorIdIfNeed(clientType, playerResponse.getResponseContext().getVisitorData());
                } else {
                    ReelItemWatchResponse reelItemWatchResponse = ReelItemWatchResponse.parseFrom(inputStream);
                    VisitorIdRequester.updateVisitorIdIfNeed(clientType, reelItemWatchResponse.getResponseContext().getVisitorData());
                    playerResponse = reelItemWatchResponse.getPlayerResponse();
                }
                PlayabilityStatus playabilityStatus = playerResponse.getPlayabilityStatus();
                String status = playabilityStatus.getStatus().name();

                if (!"OK".equals(status)) {
                    return null;
                }

                PlayerResponse.Builder responseBuilder = playerResponse.toBuilder();
                if (!playerResponse.hasStreamingData()) {
                    return null;
                }

                StreamingData streamingData = playerResponse.getStreamingData();
                if (streamingData.getAdaptiveFormatsCount() == 0) {
                    return null;
                }

                // In YouTube 20.21.37, manifestless livestreams cannot be played using the SABR protocol, or there are playback issues.
                // Until code to assemble the manifestUrl is implemented or code to override the exoPlayerConfig is ready,
                // TV SABR clients in livestreams will be temporarily fallbacked to TV DASH clients.
                //
                // TODO: Override other playerConfigs such as exoPlayerConfig.
                // Include TV_SABR_WII_U (Morphe 5c6b8418).
                if (!forDownload && clientType.requireSABR
                        && (clientType == ClientType.TV_SABR || clientType == ClientType.TV_SABR_WII_U)
                        && Utils.containsAny(streamingData.getServerAbrStreamingUrl(), "yt_live_broadcast", "yt_premiere_broadcast")) {
                    Logger.printDebug(() -> "Livestream detected, fallback to TV dash");
                    fallbackWithTVDashOnThread.set(true);
                    return null;
                }

                // Every response of visionOS has an attestation challenge, which the app answers on the device
                // and the spoofed client cannot. The server puts some visitor ids in an A/B test whose streams
                // need the answer, and playback can then stop after about a minute. The urls of their streams
                // have 'pcm2=yes', which is signed and cannot be changed, so another visitor id is used instead.
                if (clientType.clientName.equals(ClientType.VISIONOS_1_02.clientName)) {
                    for (Format format : streamingData.getAdaptiveFormatsList()) {
                        if ("yes".equals(Uri.parse(format.getUrl()).getQueryParameter("pcm2"))) {
                            Logger.printDebug(() -> "Visitor id is in the attestation A/B test: " + clientType
                                    + ", videoId: " + videoId);
                            VisitorIdRequester.discardVisitorId(clientType);
                            newVisitorIdOnThread.set(true);
                            return null;
                        }
                    }
                }

                if (clientType.requireJS) {
                    String poToken = clientType.requirePoToken
                            ? PoTokenManager.getStreamingPoToken(clientType, videoId)
                            : "";

                    var deobfuscatedStreamingData = getDeobfuscatedStreamingData(streamingData, poToken, !forDownload && clientType.requireSABR);
                    if (deobfuscatedStreamingData == null) {
                        return null;
                    }
                    responseBuilder.setStreamingData(deobfuscatedStreamingData);
                }

                // Strip VP9 formats when Force AVC is on (Morphe bb3e3ff0).
                // 'Force AVC' only keeps the player from using VP9, its formats are still in the stream.
                // A default quality that only VP9 formats reach then has no format the player can use,
                // and with SABR the video restarts at 0:01 without end, each time with a new playback nonce.
                // HDR formats are a different codec string ('vp09.02...') and are kept.
                // Without any AVC format nothing is removed, so the video still plays.
                if (!forDownload && SpoofVideoStreamsPatch.getForceAVC()) {
                    List<Format> adaptiveFormats = responseBuilder.getStreamingData().getAdaptiveFormatsList();
                    List<Format> withoutVP9 = new ArrayList<>(adaptiveFormats.size());
                    boolean hasAVC = false;
                    for (Format format : adaptiveFormats) {
                        String mimeType = format.getMimeType();
                        if (mimeType.contains("codecs=\"vp9\"")) continue;

                        if (mimeType.startsWith("video") && mimeType.contains("avc")) {
                            hasAVC = true;
                        }
                        withoutVP9.add(format);
                    }

                    if (hasAVC && withoutVP9.size() < adaptiveFormats.size()) {
                        Logger.printDebug(() -> "Removing VP9 formats, videoId: " + videoId);
                        responseBuilder.setStreamingData(responseBuilder.getStreamingData().toBuilder()
                                .clearAdaptiveFormats()
                                .addAllAdaptiveFormats(withoutVP9));
                    }
                }

                byte[] streamingDataBuffer = responseBuilder.build().toByteArray();
                byte[] playerConfigBuffer = null;
                boolean hasAndroidMedia = false;

                if (clientType.requireSABR && playerResponse.hasPlayerConfig()) {
                    PlayerConfig.Builder playerConfigBuilder = playerResponse.getPlayerConfig().toBuilder();

                    // If 'androidMedialibConfig' exists in the response, all playerConfigs are compatible.
                    // Override all playerConfigs.
                    hasAndroidMedia = playerConfigBuilder.hasAndroidMedialibConfig();

                    if (hasAndroidMedia) {
                        // In some clients, 'playerGestureConfig' is missing from the response.
                        // Add 'playerGestureConfig' using proto builder.
                        PlayerGestureConfig.Builder playerGestureConfigBuilder = playerConfigBuilder.getPlayerGestureConfig().toBuilder();
                        playerGestureConfigBuilder.setDownAndOutPortraitAllowed(true);
                        playerGestureConfigBuilder.setDownAndOutLandscapeAllowed(true);
                        playerConfigBuilder.setPlayerGestureConfig(playerGestureConfigBuilder);

                        // How the video is played inline is added when the config is given to the player,
                        // so the same fetch can be used by an inline player and by the full player.
                    }

                    playerConfigBuffer = playerConfigBuilder.build().toByteArray();
                }

                return new StreamData(streamingDataBuffer, playerConfigBuffer, hasAndroidMedia, clientType);
            } else {
                String response = new BufferedReader(new InputStreamReader(inputStream))
                        .lines()
                        .collect(Collectors.joining("\n"));

                JSONObject jsonResponse = new JSONObject(response);

                if (clientType == ClientType.GET_CHANNEL_FROM_ID) {
                    return jsonResponse
                            .getJSONObject("videoDetails")
                            .getString("channelId");
                } else if (clientType == ClientType.SAVE_TO_WATCH_LATER) {
                    return response;
                }
            }
        } catch (IOException | JSONException ex) {
            Logger.printException(() -> "Failed to write player response", ex);
        }
        return null;
    }

    private static Object fetch(@Nullable Route.CompiledRoute videoDetailsEndpoint,
                                String videoId, boolean isInline, Map<String, String> playerHeaders) {
        return fetch(videoDetailsEndpoint, videoId, isInline, playerHeaders, null, false);
    }

    private static Object fetch(@Nullable Route.CompiledRoute videoDetailsEndpoint,
                                String videoId, boolean isInline, Map<String, String> playerHeaders,
                                @Nullable ClientType[] clientStreamOrderOverride) {
        return fetch(videoDetailsEndpoint, videoId, isInline, playerHeaders, clientStreamOrderOverride, false);
    }

    private static Object fetch(@Nullable Route.CompiledRoute videoDetailsEndpoint,
                                String videoId, boolean isInline, Map<String, String> playerHeaders,
                                @Nullable ClientType[] clientStreamOrderOverride,
                                boolean isDownload) {
        if (videoDetailsEndpoint == null) {
            final boolean debugEnabled = BaseSettings.DEBUG.get();
            ClientType[] clientOrderToUse = clientStreamOrderOverride == null || clientStreamOrderOverride.length == 0
                    ? StreamOrDetailsDataRequest.clientStreamOrderToUse
                    : clientStreamOrderOverride;
            int i = 0;
            for (ClientType clientTypeStream : clientOrderToUse) {
                final boolean showErrorToast = ((++i == clientOrderToUse.length) || debugEnabled) && !isDownload;

                if (clientTypeStream.requireJS) {
                    JavaScriptManager.warmUpInBackground();
                }

                HttpURLConnection connection = send(clientTypeStream, videoId, playerHeaders, showErrorToast, isDownload);
                fallbackWithTVDashOnThread.set(false);
                newVisitorIdOnThread.set(false);
                Object playerResponseBuffer = buildPlayerStreamOrDetailsResponse(clientTypeStream, connection, videoId, isInline, isDownload);

                for (int attempt = 1; attempt < VISITOR_ID_ATTEMPTS && Boolean.TRUE.equals(newVisitorIdOnThread.get()); attempt++) {
                    newVisitorIdOnThread.set(false);
                    playerResponseBuffer = buildPlayerStreamOrDetailsResponse(clientTypeStream,
                            send(clientTypeStream, videoId, playerHeaders, showErrorToast, isDownload), videoId, isInline, isDownload);
                }
                newVisitorIdOnThread.set(false);

                // Include TV_SABR_WII_U (Morphe 5c6b8418).
                if ((clientTypeStream == ClientType.TV_SABR || clientTypeStream == ClientType.TV_SABR_WII_U)
                        && fallbackWithTVDashOnThread.get()) {
                    fallbackWithTVDashOnThread.set(false);
                    clientTypeStream = ClientType.TV_DASH;
                    HttpURLConnection fallBackConnection = send(clientTypeStream, videoId, playerHeaders, showErrorToast, isDownload);
                    playerResponseBuffer = buildPlayerStreamOrDetailsResponse(clientTypeStream, fallBackConnection, videoId, isInline, isDownload);
                }

                if (playerResponseBuffer != null) {
                    if (!isDownload) {
                        // Stats for nerds and the SABR choice describe what is playing,
                        // so the client is set when the streams are given to the player, and not here.
                        // The first attempt is with the default client.
                        PreferredClientCheck.onStreamsFetched(videoId, i == 1, clientTypeStream);
                    }
                    return playerResponseBuffer;
                }
            }

            if (isDownload) {
                Logger.printDebug(() -> "No client could resolve the download: " + videoId);
                return null;
            }

            handleConnectionError(str("morphe_spoof_video_streams_no_clients_toast"), null, true);
        } else {
            ClientType targetClient = null;
            if (videoDetailsEndpoint.equals(PlayerRoutes.GET_CHANNEL_FROM_ID)) {
                targetClient = ClientType.GET_CHANNEL_FROM_ID;
            } else if (videoDetailsEndpoint.equals(PlayerRoutes.SEND_SAVE_VIDEO_TO_WATCH_LATER)) {
                targetClient = ClientType.SAVE_TO_WATCH_LATER;
            }
            if (targetClient != null) {
                HttpURLConnection connection = send(targetClient, videoId, playerHeaders, false);
                if (connection != null) {
                    return buildPlayerStreamOrDetailsResponse(targetClient, connection, videoId, isInline);
                }
            }
        }
        return null;
    }

    public boolean fetchIsDone() {
        return future.isDone();
    }

    @Nullable
    public Object getStreamDetails() {
        try {
            if (BaseSettings.DEBUG.get() && !fetchIsDone() && Utils.isCurrentlyOnMainThread()) {
                Logger.printException(() -> "Debug: Blocking main thread");
            }
            return future.get(MAX_MILLISECONDS_TO_WAIT_FOR_FETCH, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException | CancellationException ex) {
            future.cancel(true);
        }
        return null;
    }

    @Nullable
    public StreamData getStream() {
        Object details = getStreamDetails();
        return details instanceof StreamData ? (StreamData) details : null;
    }

    @NonNull
    @Override
    public String toString() {
        return "StreamOrDetailsDataRequest{" + "videoId='" + videoId + "', isInline='" + isInline + '\'' + '}';
    }

    /**
     * Keeps the headers of an InnerTube request of the app, for the fetches that are started without
     * a request, and so downloads can resolve tracks that were never opened.
     * Called for every InnerTube request, because a video can be loaded before any request for a video is seen.
     */
    public static void setAppHeaders(@Nullable Map<String, String> requestHeaders) {
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            lastPlayerHeaders = requestHeaders;
        }
    }

    /**
     * Starts fetching the streams of a video that the app loads with a request that is not seen,
     * unless a fetch for the video can still be used.
     */
    public static void fetchRequestIfNeeded(String videoId) {
        synchronized (streamCache) {
            StreamOrDetailsDataRequest existing = streamCache.get(videoId);
            if (existing == null || !existing.canReuse()) {
                fetchWithLastHeaders(videoId);
            }
        }
    }

    /**
     * @return The fetch of the streams to give to the player of the video. If no fetch can be used,
     *         a fetch is started now.
     */
    public static StreamOrDetailsDataRequest getOrFetchRequest(String videoId) {
        synchronized (streamCache) {
            StreamOrDetailsDataRequest existing = streamCache.get(videoId);
            if (existing != null && existing.canReuse()) {
                return existing;
            }
            return fetchWithLastHeaders(videoId);
        }
    }

    private static StreamOrDetailsDataRequest fetchWithLastHeaders(String videoId) {
        Logger.printDebug(() -> "Fetching streams without a player request for: " + videoId);
        StreamOrDetailsDataRequest request = new StreamOrDetailsDataRequest(null, videoId, false, lastPlayerHeaders,
                clientStreamOrderToUse, false);
        streamCache.put(videoId, request);
        return request;
    }

    /**
     * Called when a media response asks the player to load the player response of its video again.
     * The fetches started before are not reused, since they can hold the streams the player is asked to replace.
     * This does not know the video, but a reload is rare.
     */
    public static void onPlayerResponseReloadRequested() {
        playerResponseReloadCount.incrementAndGet();
    }

    /**
     * @return If the streams of this fetch can be given to a player that is set up with the headers
     *         of the latest request that loads a video.
     */
    private boolean canReuse() {
        // The account can be changed since this fetch.
        if (!Objects.equals(authorization, lastPlayerHeaders.get(AUTHORIZATION_HEADER))) {
            return false;
        }
        if (playerResponseReloadCountAtFetch != playerResponseReloadCount.get()) {
            return false;
        }
        if (!future.isDone()) {
            return true;
        }
        final long age = System.currentTimeMillis() - fetchStartTime;
        return age < MAX_MILLISECONDS_TO_REUSE_FAILED_FETCH
                || (age < MAX_MILLISECONDS_TO_REUSE_STREAMS && getStreamDetails() instanceof StreamData);
    }

    /**
     * The player config of the client replaces the whole player config of the app.
     * It does not tell how the video is played inline, such as a muted preview in the feed,
     * and it buffers as the player of the client app does, which can start a video later
     * (such as after 2.5 seconds of media, instead of 1.6 seconds with the app player config).
     *
     * The quality menu belongs to the app, so the app player config also tells if the quality selected
     * in the quality menu is used only for the video it was selected for. Without it, the quality selected
     * is also used for the next videos.
     *
     * @param hasAndroidMedia If the streams are the streams of an Android client,
     *                        so the app player config tells how to play them inline and how to buffer them.
     * @return The player config with how the app player config plays the video inline, buffers it,
     *         and uses the quality selected in the quality menu.
     */
    public static byte[] addAppPlayerConfig(byte[] playerConfig, byte[] appPlayerConfig, boolean hasAndroidMedia) {
        try {
            PlayerConfig appConfig = PlayerConfig.parseFrom(appPlayerConfig);
            PlayerConfig.Builder builder = PlayerConfig.parseFrom(playerConfig).toBuilder();

            if (appConfig.hasStickyQualitySelectionConfig()) {
                builder.setStickyQualitySelectionConfig(appConfig.getStickyQualitySelectionConfig());
            } else {
                builder.clearStickyQualitySelectionConfig();
            }
            if (!hasAndroidMedia) {
                return builder.build().toByteArray();
            }

            if (appConfig.getAudioConfig().getMuteOnStart()) {
                builder.setAudioConfig(builder.getAudioConfig().toBuilder().setMuteOnStart(true));
            }
            if (appConfig.hasInlinePlaybackConfig()) {
                builder.setInlinePlaybackConfig(appConfig.getInlinePlaybackConfig());
            }

            // Only the buffering fields are copied, the other fields of the client config are kept.
            if (builder.hasExoPlayerConfig()) {
                ExoPlayerConfig appExoConfig = appConfig.getExoPlayerConfig();
                ExoPlayerConfig.Builder exoConfig = builder.getExoPlayerConfig().toBuilder();
                if (appExoConfig.hasLowWatermarkMs()) {
                    exoConfig.setLowWatermarkMs(appExoConfig.getLowWatermarkMs());
                }
                if (appExoConfig.hasHighWatermarkMs()) {
                    exoConfig.setHighWatermarkMs(appExoConfig.getHighWatermarkMs());
                }
                if (appExoConfig.hasMinDurationForPlaybackStartMs()) {
                    exoConfig.setMinDurationForPlaybackStartMs(appExoConfig.getMinDurationForPlaybackStartMs());
                }
                if (appExoConfig.hasMinDurationForPlaybackRestartMs()) {
                    exoConfig.setMinDurationForPlaybackRestartMs(appExoConfig.getMinDurationForPlaybackRestartMs());
                }
                if (appExoConfig.hasUsePredictedBuffer()) {
                    exoConfig.setUsePredictedBuffer(appExoConfig.getUsePredictedBuffer());
                }
                if (appExoConfig.hasMaxReadAheadMediaTimeMs()) {
                    exoConfig.setMaxReadAheadMediaTimeMs(appExoConfig.getMaxReadAheadMediaTimeMs());
                }
                if (appExoConfig.hasUseMediaTimeCappedLoadControl()) {
                    exoConfig.setUseMediaTimeCappedLoadControl(appExoConfig.getUseMediaTimeCappedLoadControl());
                }
                builder.setExoPlayerConfig(exoConfig);
            }

            if (builder.hasAndroidMedialibConfig()) {
                AndroidMedialibConfig appMedialibConfig = appConfig.getAndroidMedialibConfig();
                AndroidMedialibConfig.Builder medialibConfig = builder.getAndroidMedialibConfig().toBuilder();
                if (appMedialibConfig.hasEnablePrerollPrebuffer()) {
                    medialibConfig.setEnablePrerollPrebuffer(appMedialibConfig.getEnablePrerollPrebuffer());
                }
                if (appMedialibConfig.hasPrebufferOptimizeForViewportSize()) {
                    medialibConfig.setPrebufferOptimizeForViewportSize(appMedialibConfig.getPrebufferOptimizeForViewportSize());
                }
                builder.setAndroidMedialibConfig(medialibConfig);
            }

            // The media uStreamer request config of the client is kept, it belongs to the streams of the client.
            if (builder.hasMediaCommonConfig()) {
                MediaCommonConfig appMediaConfig = appConfig.getMediaCommonConfig();
                MediaCommonConfig.Builder mediaConfig = builder.getMediaCommonConfig().toBuilder();
                if (appMediaConfig.hasDynamicReadaheadConfig()) {
                    mediaConfig.setDynamicReadaheadConfig(appMediaConfig.getDynamicReadaheadConfig());
                }
                if (appMediaConfig.hasServerPlaybackStartConfig()) {
                    mediaConfig.setServerPlaybackStartConfig(appMediaConfig.getServerPlaybackStartConfig());
                }
                if (appMediaConfig.getMediaCacheConfig().hasCacheLoadPolicy()) {
                    mediaConfig.setMediaCacheConfig(mediaConfig.getMediaCacheConfig().toBuilder()
                            .setCacheLoadPolicy(appMediaConfig.getMediaCacheConfig().getCacheLoadPolicy()));
                }
                builder.setMediaCommonConfig(mediaConfig);
            }

            // When the next video is prefetched, which waits for the current video to be buffered.
            if (appConfig.hasTaskCoordinatorConfig()) {
                builder.setTaskCoordinatorConfig(appConfig.getTaskCoordinatorConfig());
            }

            return builder.build().toByteArray();
        } catch (InvalidProtocolBufferException ex) {
            Logger.printException(() -> "addAppPlayerConfig failure", ex);
            return playerConfig;
        }
    }
}
