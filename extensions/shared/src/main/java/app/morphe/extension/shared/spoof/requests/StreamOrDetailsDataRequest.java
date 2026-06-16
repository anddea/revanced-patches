/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 *
 * Copyright (C) 2026 anddea (https://github.com/anddea)
 */

package app.morphe.extension.shared.spoof.requests;

import static app.morphe.extension.shared.StringRef.str;
import static app.morphe.extension.shared.Utils.submitOnBackgroundThread;
import static app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch.pageIDHeaderValue;
import static app.morphe.extension.shared.spoof.js.JavaScriptEngineSupport.supportsJavaScriptEngine;
import static app.morphe.extension.shared.spoof.js.JavaScriptManager.getDeobfuscatedStreamingData;
import static app.morphe.extension.shared.spoof.requests.PlayerRoutes.GET_PLAYER_STREAMING_DATA;
import static app.morphe.extension.shared.spoof.requests.PlayerRoutes.GET_REEL_STREAMING_DATA;

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

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayerResponse;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.StreamingData;
import app.morphe.extension.shared.innertube.ReelItemWatchResponseOuterClass.ReelItemWatchResponse;
import app.morphe.extension.shared.oauth2.requests.OAuth2Requester;
import app.morphe.extension.shared.requests.Route;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.spoof.ClientType;

public class StreamOrDetailsDataRequest {

    private static volatile ClientType[] clientStreamOrderToUse =
            Arrays.stream(ClientType.values())
                    .filter(client -> client.endpoint == GET_PLAYER_STREAMING_DATA || client.endpoint == GET_REEL_STREAMING_DATA)
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

    private static final String[] REQUEST_HEADER_KEYS = {
            AUTHORIZATION_HEADER,
            "X-GOOG-API-FORMAT-VERSION",
            "X-Goog-Visitor-Id"
    };

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

        final boolean isStream = clientType.endpoint == GET_PLAYER_STREAMING_DATA || clientType.endpoint == GET_REEL_STREAMING_DATA;

        try {
            HttpURLConnection connection = PlayerRoutes.getPlayerResponseConnectionFromRoute(clientType);
            connection.setConnectTimeout(HTTP_TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(HTTP_TIMEOUT_MILLISECONDS);

            boolean authHeadersIncludes = false;
            authHeadersOverrides = false;

            if (playerHeaders != null) {
                for (String key : REQUEST_HEADER_KEYS) {
                    String value = playerHeaders.get(key);

                    if (value != null) {
                        if (key.equals(AUTHORIZATION_HEADER)) {
                            if (clientType.supportsOAuth2) {
                                String authorization = OAuth2Requester.getAndUpdateAccessTokenIfNeeded();
                                if (authorization.isEmpty()) {
                                    continue;
                                } else {
                                    value = authorization;
                                    authHeadersOverrides = true;
                                }
                            } else if (!clientType.canLogin) {
                                continue;
                            }
                            authHeadersIncludes = true;
                        }
                        connection.setRequestProperty(key, value);
                    }
                }
            }

            if (authHeadersIncludes) {
                if (!pageIDHeaderValue.isEmpty()) {
                    connection.setRequestProperty(PAGE_ID_HEADER, pageIDHeaderValue);
                }
            } else {
                if (clientType.requireLogin) {
                    return null;
                }
            }

            String innerTubeBody = PlayerRoutes.createInnertubeBody(clientType, videoId);
            byte[] requestBody = innerTubeBody.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(requestBody.length);
            connection.getOutputStream().write(requestBody);

            final int responseCode = connection.getResponseCode();
            if (responseCode == 200) return connection;

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
        final boolean returnStreamObject = clientType.endpoint == GET_PLAYER_STREAMING_DATA
                || clientType.endpoint == GET_REEL_STREAMING_DATA;

        if (connection.getContentLength() == 0) {
            handleDebugToast(String.format("Debug: Ignoring empty %s client (%%s)", returnStreamObject ? "spoof stream" : "get details"), clientType);
            return null;
        }

        try (InputStream inputStream = connection.getInputStream()) {
            if (returnStreamObject) {
                PlayerResponse playerResponse = clientType.endpoint == GET_PLAYER_STREAMING_DATA
                        ? PlayerResponse.parseFrom(inputStream)
                        : ReelItemWatchResponse.parseFrom(inputStream).getPlayerResponse();
                var playabilityStatus = playerResponse.getPlayabilityStatus();
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
                    var deobfuscatedStreamingData = getDeobfuscatedStreamingData(streamingData);
                    if (deobfuscatedStreamingData == null) {
                        return null;
                    }
                    responseBuilder.setStreamingData(deobfuscatedStreamingData);
                }

                return responseBuilder.build().toByteArray();
            } else {
                String response = new BufferedReader(new InputStreamReader(inputStream))
                        .lines()
                        .collect(Collectors.joining("\n"));

                JSONObject jsonResponse = new JSONObject(response);

                if (clientType.endpoint.equals(PlayerRoutes.GET_CHANNEL_FROM_ID)) {
                    return jsonResponse
                            .getJSONObject("videoDetails")
                            .getString("channelId");
                } else if (clientType.endpoint.equals(PlayerRoutes.SEND_SAVE_VIDEO_TO_WATCH_LATER)) {
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
            for (ClientType clientTypeDetails : ClientType.values()) {
                if (clientTypeDetails.endpoint == videoDetailsEndpoint) {
                    HttpURLConnection connection = send(clientTypeDetails, videoId, playerHeaders, false);
                    if (connection != null) {
                        return buildPlayerStreamOrDetailsResponse(clientTypeDetails, connection);
                    }
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
