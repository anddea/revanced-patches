/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 *
 * Copyright (C) 2026 anddea (https://github.com/anddea)
 */

package app.morphe.extension.shared.spoof.requests;

import static app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch.pageIDHeaderValue;
import static app.morphe.extension.shared.spoof.js.JavaScriptEngineSupport.supportsJavaScriptEngine;
import static app.morphe.extension.shared.spoof.js.JavaScriptManager.getDeobfuscatedStreamingData;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.submitOnBackgroundThread;

import androidx.annotation.Nullable;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayerResponse;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayerConfig;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayabilityStatus;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.StreamingData;
import app.morphe.extension.shared.innertube.ReelItemWatchResponseOuterClass.ReelItemWatchResponse;
import app.morphe.extension.shared.oauth2.requests.OAuth2Requester;
import app.morphe.extension.shared.requests.Route;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.spoof.ClientType;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

public class StreamOrDetailsDataRequest {

    public record StreamData(byte[] streamingData, @Nullable byte[] playerConfig) {
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
        Objects.requireNonNull(preferredClient);

        List<ClientType> orderToUse = new ArrayList<>(availableClients.size());
        orderToUse.add(preferredClient);

        for (ClientType client : availableClients) {
            if (client.requireJS && !supportsJavaScriptEngine()) {
                Logger.printDebug(() -> "Could not find JavaScript engine. Skipping JavaScript client: " + client.name());
                continue;
            }

            if (client != preferredClient) {
                orderToUse.add(client);
            }
        }

        clientStreamOrderToUse = orderToUse.toArray(new ClientType[0]);
        Logger.printDebug(() -> "Available spoof clients: " + orderToUse);
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

    private static volatile ClientType lastSpoofedClientType;
    private static volatile boolean authHeadersOverrides;

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

    private final Future<Object> future;

    private StreamOrDetailsDataRequest(@Nullable Route.CompiledRoute endpoint,
                                       String videoId, Map<String, String> playerHeaders) {
        this(endpoint, videoId, playerHeaders, null);
    }

    private StreamOrDetailsDataRequest(@Nullable Route.CompiledRoute endpoint,
                                       String videoId, Map<String, String> playerHeaders,
                                       @Nullable ClientType[] clientStreamOrderOverride) {
        if (endpoint == null) {
            Objects.requireNonNull(playerHeaders);
        }

        this.future = submitOnBackgroundThread(() ->
                fetch(endpoint, videoId, playerHeaders, clientStreamOrderOverride));
    }

    public static void fetchStreamRequest(String videoId, Map<String, String> fetchHeaders) {
        streamCache.put(videoId, new StreamOrDetailsDataRequest(null, videoId, fetchHeaders));
    }

    public static void fetchStreamRequest(String videoId, Map<String, String> fetchHeaders,
                                          ClientType... clientStreamOrderOverride) {
        streamCache.put(videoId, new StreamOrDetailsDataRequest(
                null,
                videoId,
                fetchHeaders,
                clientStreamOrderOverride
        ));
    }

    @Nullable
    public static StreamOrDetailsDataRequest getStreamRequestForVideoId(String videoId) {
        return streamCache.get(videoId);
    }

    public static StreamOrDetailsDataRequest getDetailsRequest(Route.CompiledRoute videoDetailsEndpoint,
                                                               String videoId, Map<String, String> fetchHeaders) {
        StreamOrDetailsDataRequest request = new StreamOrDetailsDataRequest(videoDetailsEndpoint, videoId, fetchHeaders);
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
        Objects.requireNonNull(clientType);
        Objects.requireNonNull(videoId);

        final boolean isStream = clientType != ClientType.GET_CHANNEL_FROM_ID && clientType != ClientType.SAVE_TO_WATCH_LATER;

        try {
            HttpURLConnection connection = PlayerRoutes.getPlayerResponseConnectionFromRoute(clientType);
            connection.setConnectTimeout(HTTP_TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(HTTP_TIMEOUT_MILLISECONDS);

            authHeadersOverrides = false;

            String visitorId = "";
            if (isStream) {
                String authorization = playerHeaders.get(AUTHORIZATION_HEADER);
                boolean authHeadersIncludes = Utils.isNotEmpty(authorization);

                // Auth header is required, but the user is not logged in. These clients are skipped:
                // ANDROID_CREATOR, TV_SIMPLY, ANDROID_MUSIC_REEL, ANDROID_MUSIC_NO_SDK.
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
                else if (clientType.supportsOAuth2 && authHeadersIncludes) {
                    String oauth2Authorization = OAuth2Requester.getAndUpdateAccessTokenIfNeeded();
                    if (Utils.isNotEmpty(oauth2Authorization)) {
                        authHeadersOverrides = true;
                        connection.setRequestProperty(AUTHORIZATION_HEADER, oauth2Authorization);
                        Logger.printDebug(() -> "Set oauth2 auth header: " + clientType + ", videoId: " + videoId);
                    }
                }
                // These clients can play videos without the auth header:
                // ANDROID_VR (ANDROID_XR), TV_SABR, VISIONOS_1_02 (VISIONOS_1_03).
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
                                                             HttpURLConnection connection) {
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

                if (clientType.requireJS) {
                    var deobfuscatedStreamingData = getDeobfuscatedStreamingData(streamingData, clientType.requireSABR);
                    if (deobfuscatedStreamingData == null) {
                        return null;
                    }
                    responseBuilder.setStreamingData(deobfuscatedStreamingData);
                }

                byte[] streamingDataBuffer = responseBuilder.build().toByteArray();
                byte[] playerConfigBuffer = null;

                if (clientType.requireSABR && playerResponse.hasPlayerConfig()) {
                    PlayerConfig playerConfig = playerResponse.getPlayerConfig();

                    // It seems there is an issue when 'usePlatypus = true' when forcing the AVC codec.
                    // Override the 'usePlatypus' to false.
                    if (SharedYouTubeSettings.OVERRIDE_INITIAL_VIDEO_QUALITY.get()) {
                        PlayerConfig.Builder playerConfigBuilder = playerConfig.toBuilder();
                        var mediaCommonConfigBuilder = playerConfigBuilder
                                .getMediaCommonConfig().toBuilder();
                        mediaCommonConfigBuilder.setUsePlatypus(false);
                        playerConfigBuilder.setMediaCommonConfig(mediaCommonConfigBuilder);
                        playerConfig = playerConfigBuilder.build();
                    }
                    playerConfigBuffer = playerConfig.toByteArray();
                }

                return new StreamData(streamingDataBuffer, playerConfigBuffer);
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
                                String videoId, Map<String, String> playerHeaders) {
        return fetch(videoDetailsEndpoint, videoId, playerHeaders, null);
    }

    private static Object fetch(@Nullable Route.CompiledRoute videoDetailsEndpoint,
                                String videoId, Map<String, String> playerHeaders,
                                @Nullable ClientType[] clientStreamOrderOverride) {
        if (videoDetailsEndpoint == null) {
            final boolean debugEnabled = BaseSettings.DEBUG.get();
            ClientType[] clientOrderToUse = clientStreamOrderOverride == null || clientStreamOrderOverride.length == 0
                    ? clientStreamOrderToUse
                    : clientStreamOrderOverride;
            int i = 0;
            for (ClientType clientTypeStream : clientOrderToUse) {
                final boolean showErrorToast = (++i == clientOrderToUse.length) || debugEnabled;
                HttpURLConnection connection = send(clientTypeStream, videoId, playerHeaders, showErrorToast);
                if (connection != null) {
                    Object playerResponseBuffer = buildPlayerStreamOrDetailsResponse(clientTypeStream, connection);
                    if (playerResponseBuffer != null) {
                        lastSpoofedClientType = clientTypeStream;
                        return playerResponseBuffer;
                    }
                }
            }

            lastSpoofedClientType = null;
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
                    return buildPlayerStreamOrDetailsResponse(targetClient, connection);
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
}
