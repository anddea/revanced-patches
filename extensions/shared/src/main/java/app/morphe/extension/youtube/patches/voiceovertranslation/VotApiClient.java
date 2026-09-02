/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Jav1x (https://github.com/Jav1x)
 * - sashade8-ship-it (https://github.com/sashade8-ship-it)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;

public class VotApiClient {

    private static final String VOT_USER_SCRIPT_URL =
            "https://raw.githubusercontent.com/ilyhalight/voice-over-translation/master/dist/vot.user.js";
    private static final Pattern PROXY_WORKER_HOST_PATTERN =
            Pattern.compile("\\bproxyWorkerHost\\s*=\\s*[\"']([^\"']+)[\"']");

    private static final String HMAC_KEY = "bt8xH3VOlb4mqf0nqAibnDOoiPlXsisf";
    private static final String COMPONENT_VERSION = "26.6.4.760";
    private static final String VOT_MODULE = "video-translation";
    private static final double DEFAULT_DURATION = 310.0;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/148.0.0.0 YaBrowser/26.6.0.0 Safari/537.36";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;

    private static String sessionUuid = null;
    private static String sessionSecretKey = null;
    private static long sessionExpires = 0;
    private static final ReentrantLock sessionLock = new ReentrantLock();

    /** Translation results are reusable while the worker's generated audio remains fresh. */
    private static final long CACHE_TTL_MS = 30 * 60_000L;
    private static final Map<String, CachedResult> translationCache = new ConcurrentHashMap<>();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private record CachedResult(TranslationResult result, long createdAt) {
        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
    }

    /** OAuth token validation cache — skip re-checking once passed during process lifetime. */
    private static volatile String lastValidatedToken = null;
    private static volatile boolean tokenIsValid = false;

    public record TranslationResult(int status, String audioUrl, int remainingTime,
                                    String translationId, String message) {
    }

    public static final int STATUS_FAILED = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_WAITING = 2;
    public static final int STATUS_LONG_WAITING = 3;
    public static final int STATUS_PART_CONTENT = 5;
    public static final int STATUS_AUDIO_REQUESTED = 6;
    public static final int STATUS_SESSION_REQUIRED = 7;

    /**
     * Callback interface for {@link #pollUntilReady}.
     * Implementations handle the per-caller differences in polling behavior.
     */
    public interface PollHandler {
        /** @return true if polling should be canceled (e.g. video changed, deadline passed) */
        boolean isCancelled();

        /**
         * Called when translation audio is ready (STATUS_FINISHED or STATUS_PART_CONTENT
         * with a non-empty audioUrl).
         */
        void onAudioReady(TranslationResult result);

        /**
         * Called on STATUS_AUDIO_REQUESTED to allow sending audio data to the server.
         * It may be a no-op if the caller does not support audio upload.
         */
        void onAudioRequested(String videoUrl, String translationId);

        /**
         * Called on STATUS_FAILED.
         *
         * @param result failed response, including the server's diagnostic message
         * @return true if the handler took recovery action and polling should continue
         *         (e.g. disabled live voices), false to stop polling.
         */
        boolean onFailed(TranslationResult result);

        /** Called on STATUS_SESSION_REQUIRED. */
        void onSessionRequired();

        /**
         * Called when status indicates waiting. Allows the handler to observe or react
         * to the wait (e.g. show a toast).
         *
         * @param waitSeconds suggested wait time from the API
         * @param isFirstWait true if this is the first waiting response in this poll session
         */
        void onWaiting(int waitSeconds, boolean isFirstWait);
    }

    private static final int DEFAULT_POLL_MAX_RETRIES = 30;
    private static final int DEFAULT_POLL_DELAY_SECONDS = 10;
    private static final int MAX_POLL_DELAY_SECONDS = 15;
    private static final int AUDIO_REQUESTED_RETRY_DELAY_SECONDS = 3;

    /**
     * Polls the translation API until the result is ready, failed, or canceled.
     * Centralizes the polling loop shared by VoiceOverTranslationPatch and VotStreamReplacer.
     *
     * @param videoUrl         the YouTube video URL
     * @param duration         video duration in seconds
     * @param sourceLang       source language code (or "auto"/"")
     * @param targetLang       target language code
     * @param videoTitle       video title for the API request
     * @param initialWaitSeconds seconds to sleep before the first API call (0 to skip)
     * @param handler          callback for status-specific handling
     * @return the final TranslationResult, or null if polling was canceled/failed
     */
    public static TranslationResult pollUntilReady(
            String videoUrl, double duration,
            String sourceLang, String targetLang,
            String videoTitle,
            int initialWaitSeconds,
            PollHandler handler
    ) {
        // A non-zero initial delay is used by the in-app flow after it already received a
        // waiting response. The stream replacer uses zero and therefore starts a new request.
        return pollUntilReady(
                videoUrl, duration, sourceLang, targetLang, videoTitle,
                initialWaitSeconds, initialWaitSeconds <= 0, handler
        );
    }

    /**
     * Variant that explicitly declares whether the first API call should enqueue a translation
     * or only poll an existing request.
     */
    public static TranslationResult pollUntilReady(
            String videoUrl, double duration,
            String sourceLang, String targetLang,
            String videoTitle,
            int initialWaitSeconds,
            boolean firstRequest,
            PollHandler handler
    ) {
        int waitSeconds = initialWaitSeconds > 0
                ? pollDelaySeconds(initialWaitSeconds)
                : 1;
        boolean isFirstWait = true;

        for (int retry = 0; retry < DEFAULT_POLL_MAX_RETRIES; retry++) {
            if (handler.isCancelled()) return null;

            final int pollNumber = retry + 1;
            final int requestedWaitSeconds = waitSeconds;
            final boolean isFirstRequest = firstRequest;
            Logger.printDebug(() -> "VOT poll #" + pollNumber
                    + ": sending " + (isFirstRequest ? "initial" : "status")
                    + " request after " + requestedWaitSeconds + "s");

            try {
                Thread.sleep(waitSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }

            if (handler.isCancelled()) return null;

            TranslationResult result;
            try {
                result = requestTranslation(
                        videoUrl, duration, sourceLang, targetLang, videoTitle, firstRequest);
                // Even a null response may have reached the worker. Do not send another
                // firstRequest and accidentally enqueue a duplicate translation.
                firstRequest = false;
            } catch (Exception e) {
                Logger.printException(() -> "pollUntilReady: requestTranslation failure", e);
                continue;
            }

            if (result == null) {
                Logger.printDebug(() -> "VOT poll #" + pollNumber
                        + ": no decoded response; retrying in 5s");
                waitSeconds = 5;
                continue;
            }

            Logger.printDebug(() -> "VOT poll #" + pollNumber
                    + " response: " + describeTranslationResult(result));

            int status = result.status();

            if (status == STATUS_FINISHED || status == STATUS_PART_CONTENT) {
                if (result.audioUrl() != null && !result.audioUrl().isEmpty()) {
                    handler.onAudioReady(result);
                    return result;
                }
                return null;
            }

            if (status == STATUS_FAILED) {
                if (handler.onFailed(result)) {
                    waitSeconds = 3;
                    continue;
                }
                return null;
            }

            if (status == STATUS_SESSION_REQUIRED) {
                handler.onSessionRequired();
                return null;
            }

            int waitSeconds1 = result.remainingTime() > 0 ? result.remainingTime() : 5;
            if (status == STATUS_WAITING || status == STATUS_LONG_WAITING) {
                waitSeconds = pollDelaySeconds(waitSeconds1);
                final int nextPollDelaySeconds = waitSeconds;
                Logger.printDebug(() -> "VOT poll #" + pollNumber
                        + ": server remainingTime=" + result.remainingTime()
                        + "s, next poll in " + nextPollDelaySeconds + "s");
                handler.onWaiting(waitSeconds1, isFirstWait);
                isFirstWait = false;
                continue;
            }

            if (status == STATUS_AUDIO_REQUESTED) {
                waitSeconds = pollDelaySeconds(waitSeconds1);
                final int nextPollDelaySeconds = Math.min(
                        waitSeconds, AUDIO_REQUESTED_RETRY_DELAY_SECONDS);
                Logger.printDebug(() -> "VOT poll #" + pollNumber
                        + ": server remainingTime=" + result.remainingTime()
                        + "s, audio upload requested, next poll in "
                        + nextPollDelaySeconds + "s");
                handler.onWaiting(waitSeconds1, isFirstWait);
                isFirstWait = false;
                handler.onAudioRequested(videoUrl, result.translationId());
                waitSeconds = Math.min(waitSeconds, AUDIO_REQUESTED_RETRY_DELAY_SECONDS);
                continue;
            }

            waitSeconds = 5;
        }

        return null;
    }

    /**
     * Keeps readiness checks frequent enough to notice completed audio while leaving the
     * server-provided estimate available to the UI as the user-facing countdown.
     */
    private static int pollDelaySeconds(int serverRemainingSeconds) {
        if (serverRemainingSeconds <= 0) return DEFAULT_POLL_DELAY_SECONDS;
        return Math.min(serverRemainingSeconds, MAX_POLL_DELAY_SECONDS);
    }

    /**
     * Streams the worker envelope so audio upload requests do not require a second, larger
     * in-memory JSON copy of the protobuf body.
     */
    private static void writeBinaryWorkerRequest(
            @NonNull OutputStream output,
            @NonNull byte[] body,
            @NonNull Map<String, String> headers
    ) throws IOException {
        StringBuilder json = new StringBuilder(16_384);
        json.append("{\"headers\":");
        appendJsonHeaders(json, headers);
        json.append(",\"body\":[");
        for (int i = 0; i < body.length; i++) {
            if (i > 0) json.append(',');
            json.append(body[i] & 0xFF);
            if (json.length() >= 16_384) {
                writeUtf8(output, json);
                json.setLength(0);
            }
        }
        json.append("]}");
        writeUtf8(output, json);
    }

    private static void writeUtf8(
            @NonNull OutputStream output,
            @NonNull StringBuilder text
    ) throws IOException {
        output.write(text.toString().getBytes(StandardCharsets.UTF_8));
    }

    @NonNull
    private static byte[] wrapJsonWorkerRequest(
            @NonNull String jsonBody,
            @NonNull Map<String, String> headers
    ) {
        StringBuilder json = new StringBuilder(jsonBody.length() + 256);
        json.append("{\"headers\":");
        appendJsonHeaders(json, headers);
        json.append(",\"body\":").append(jsonBody).append('}');
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendJsonHeaders(
            @NonNull StringBuilder json,
            @NonNull Map<String, String> headers
    ) {
        json.append('{');
        boolean first = true;
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (!first) json.append(',');
            first = false;
            appendJsonString(json, header.getKey());
            json.append(':');
            appendJsonString(json, header.getValue());
        }
        json.append('}');
    }

    private static void appendJsonString(
            @NonNull StringBuilder json,
            @NonNull String value
    ) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"':
                    json.append("\\\"");
                    break;
                case '\\':
                    json.append("\\\\");
                    break;
                case '\b':
                    json.append("\\b");
                    break;
                case '\f':
                    json.append("\\f");
                    break;
                case '\n':
                    json.append("\\n");
                    break;
                case '\r':
                    json.append("\\r");
                    break;
                case '\t':
                    json.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        json.append(String.format(Locale.US, "\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
            }
        }
        json.append('"');
    }

    /**
     * Converts a direct audio URL (S3/Yandex) to a proxied URL.
     * Format: https://{proxyHost}/video-translation/audio-proxy/{path}?{query}
     * Takes path and query from the original URL. The proxy fetches using its configured
     * base URL + path with the given query (AWS signature params).
     *
     * @param originalUrl the original audio URL
     * @return proxied URL, or originalUrl on error
     */
    @NonNull
    public static String toProxyAudioUrl(@NonNull String originalUrl) {
        if (originalUrl.isEmpty()) {
            return originalUrl;
        }
        String proxyHost = getWorkerHost();
        try {
            URI uri = new URI(originalUrl);
            String path = uri.getRawPath();
            String query = uri.getRawQuery();
            if (path == null || path.isEmpty()) {
                return originalUrl;
            }
            String result = getString(path, proxyHost, query);
            Logger.printDebug(() -> "toProxyAudioUrl: " + originalUrl + " -> " + result);
            return result;
        } catch (URISyntaxException e) {
            Logger.printDebug(() -> "toProxyAudioUrl: invalid URL " + originalUrl);
            return originalUrl;
        }
    }

    @NonNull
    private static String getString(String path, String proxyHost, String query) {
        String pathTrimmed = path.replaceFirst("^/+", "");
        int lastSlash = pathTrimmed.lastIndexOf('/');
        if (lastSlash >= 0) {
            pathTrimmed = pathTrimmed.substring(lastSlash + 1);
        }
        StringBuilder proxyUrl = new StringBuilder();
        proxyUrl.append("https://").append(proxyHost);
        proxyUrl.append("/video-translation/audio-proxy/");
        proxyUrl.append(pathTrimmed);
        if (query != null && !query.isEmpty()) {
            proxyUrl.append("?").append(query);
        }
        return proxyUrl.toString();
    }

    public static TranslationResult requestTranslation(
            String videoUrl, double duration,
            String sourceLang, String targetLang,
            String videoTitle
    ) {
        return requestTranslation(videoUrl, duration, sourceLang, targetLang, videoTitle, true);
    }

    /**
     * Requests translation while preserving whether this is the initial request for a video.
     * The Yandex API uses that bit to distinguish queueing a translation from checking its
     * existing state during polling.
     */
    public static TranslationResult requestTranslation(
            String videoUrl, double duration,
            String sourceLang, String targetLang,
            String videoTitle, boolean firstRequest
    ) {
        boolean useLiveVoices = Settings.VOT_USE_LIVE_VOICES.get();

        // Resolve OAuth token once (may be null if not configured).
        String oauthToken = useLiveVoices ? Settings.VOT_OAUTH_TOKEN.get() : null;
        if (oauthToken != null && oauthToken.isEmpty()) {
            oauthToken = null;
        }

        String cacheKey = videoUrl + "|" + sourceLang + "|" + targetLang + "|" + useLiveVoices;
        CachedResult cached = translationCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            Logger.printDebug(() -> "VOT cache hit: " + cacheKey);
            return cached.result();
        }
        if (cached != null) {
            translationCache.remove(cacheKey);
        }

        if (!ensureSession()) {
            Logger.printDebug(() -> "VOT: unable to establish session, network may be unavailable");
            return null;
        }

        // Validate OAuth token before using it (lightweight API call, cached per process)
        if (oauthToken != null && !isValidOAuthToken(oauthToken)) {
            Logger.printDebug(() -> "VOT OAuth token is invalid, clearing and falling back");
            Settings.VOT_OAUTH_TOKEN.save("");
            return new TranslationResult(STATUS_SESSION_REQUIRED, null, 0, null, null);
        }

        final String finalOauthToken = oauthToken;

        // A stale session can be returned by the worker even before its local expiry. Recreate it
        // once when the API explicitly asks for a session, then retry the same request.
        for (int attempt = 0; true; attempt++) {
            try {
                TranslationResult result = requestTranslationInternal(
                        videoUrl, duration, sourceLang, targetLang, videoTitle,
                        finalOauthToken, useLiveVoices, firstRequest
                );
                if (result == null) return null;

                if (result.status() == STATUS_SESSION_REQUIRED && attempt == 0) {
                    Logger.printDebug(() -> "VOT: session required, recreating session and retrying");
                    resetSession();
                    if (ensureSession()) continue;
                }

                if (result.status() == STATUS_FINISHED || result.status() == STATUS_PART_CONTENT) {
                    translationCache.put(cacheKey, new CachedResult(result, System.currentTimeMillis()));
                }
                return result;
            } catch (Exception e) {
                Logger.printException(() -> "VotApiClient.requestTranslation failed for " + videoUrl, e);
                return null;
            }
        }
    }

    private static TranslationResult requestTranslationInternal(
            String videoUrl, double duration,
            String sourceLang, String targetLang,
            String videoTitle, String oauthToken,
            boolean useLiveVoices, boolean firstRequest
    ) throws Exception {
        if (!ensureSession()) return null;

        if (duration <= 0) {
            duration = DEFAULT_DURATION;
        }

        String apiSourceLang = (sourceLang == null || sourceLang.isEmpty() || "auto".equalsIgnoreCase(sourceLang))
                ? "" : sourceLang;

        byte[] body = VotProtobuf.encodeTranslationRequest(
                videoUrl, firstRequest, duration,
                apiSourceLang, targetLang, videoTitle,
                useLiveVoices
        );

        String path = "/video-translation/translate";
        byte[] responseBytes = sendWorkerRequest(path, body, getVtransHeaders(path, body, oauthToken), "POST");

        if (responseBytes == null || responseBytes.length == 0) {

            return null;
        }

        Logger.printDebug(() -> "VOT translation response body: " + Arrays.toString(responseBytes));
        VotProtobuf.TranslationResponse response = VotProtobuf.decodeTranslationResponse(responseBytes);
        Logger.printDebug(() -> "VOT server response (decoded from " + responseBytes.length
                + " protobuf bytes): " + describeTranslationResponse(response));

        return new TranslationResult(
                response.status,
                response.url,
                response.remainingTime,
                response.translationId,
                response.message
        );
    }

    /**
     * Formats the fields returned by the VOT server for polling diagnostics. Signed audio URLs
     * are intentionally represented by their length so debug logs do not expose a usable URL.
     */
    private static String describeTranslationResponse(
            @NonNull VotProtobuf.TranslationResponse response
    ) {
        String audioUrl = response.url == null
                ? "<null>"
                : "present(" + response.url.length() + " chars)";
        return "status=" + response.status + " (" + statusName(response.status) + ")"
                + ", remainingTime=" + response.remainingTime + "s"
                + ", duration=" + response.duration
                + ", translationId=" + response.translationId
                + ", language=" + response.language
                + ", message=" + response.message
                + ", audioUrl=" + audioUrl;
    }

    /**
     * Formats a response after it has passed through the public request API, preserving the
     * server-provided remaining time for comparison with the polling decision.
     */
    private static String describeTranslationResult(@Nullable TranslationResult result) {
        if (result == null) return "<null>";
        String audioUrl = result.audioUrl() == null
                ? "<null>"
                : "present(" + result.audioUrl().length() + " chars)";
        return "status=" + result.status() + " (" + statusName(result.status()) + ")"
                + ", remainingTime=" + result.remainingTime() + "s"
                + ", translationId=" + result.translationId()
                + ", message=" + result.message()
                + ", audioUrl=" + audioUrl;
    }

    private static String statusName(int status) {
        return switch (status) {
            case STATUS_FAILED -> "FAILED";
            case STATUS_FINISHED -> "FINISHED";
            case STATUS_WAITING -> "WAITING";
            case STATUS_LONG_WAITING -> "LONG_WAITING";
            case STATUS_PART_CONTENT -> "PART_CONTENT";
            case STATUS_AUDIO_REQUESTED -> "AUDIO_REQUESTED";
            case STATUS_SESSION_REQUIRED -> "SESSION_REQUIRED";
            default -> "UNKNOWN";
        };
    }

    public static void sendFailedAudio(String videoUrl) {
        try {
            String path = "/video-translation/fail-audio-js";
            StringBuilder jsonBody = new StringBuilder("{\"video_url\":");
            appendJsonString(jsonBody, videoUrl);
            jsonBody.append('}');
            sendWorkerJsonRequest(path, jsonBody.toString());
        } catch (Exception e) {
            Logger.printException(() -> "VotApiClient.sendFailedAudio failed for " + videoUrl, e);
        }
    }

    public static void sendEmptyAudio(String videoUrl, String translationId, String oauthToken) {
        try {
            byte[] body = VotProtobuf.encodeEmptyAudioRequest(translationId, videoUrl);
            sendAudioRequestBody(body, oauthToken);

        } catch (Exception e) {
            Logger.printException(() -> "VotApiClient.sendEmptyAudio failed for " + videoUrl, e);
        }
    }

    public static boolean sendAudio(String videoUrl, String translationId, String fileId, byte[] audioData) {
        try {
            byte[] body = VotProtobuf.encodeAudioRequest(translationId, videoUrl, fileId, audioData);

            return sendAudioRequestBody(body, null);
        } catch (Exception e) {
            Logger.printException(() -> "VotApiClient.sendAudio failed for " + videoUrl, e);
            return false;
        }
    }

    public static boolean sendPartialAudio(
            String videoUrl, String translationId, String fileId,
            int audioPartsLength, int version, int chunkId, byte[] audioData
    ) {
        try {
            byte[] body = VotProtobuf.encodePartialAudioRequest(
                    translationId, videoUrl, fileId,
                    audioPartsLength, version, chunkId, audioData
            );

            return sendAudioRequestBody(body, null);
        } catch (Exception e) {
            Logger.printException(() -> "VotApiClient.sendPartialAudio failed for " + videoUrl, e);
            return false;
        }
    }

    private static boolean sendAudioRequestBody(
            @NonNull byte[] body,
            @Nullable String oauthToken
    ) throws IOException {
        if (!ensureSession()) return false;

        String path = "/video-translation/audio";
        return sendWorkerRequest(path, body, getVtransHeaders(path, body, oauthToken), "PUT") != null;
    }

    /**
     * Creates a Yandex session through the configured worker. Newer workers forward the
     * Ya-summary session headers, while older workers expect the body-signature form.
     * Accepting both keeps the manually selected worker host compatible across protocol revisions.
     */
    private static boolean createSession() {
        String uuid = generateUuid();
        String path = "/session/create";
        byte[] body = VotProtobuf.encodeSessionRequest(uuid, VOT_MODULE);

        try {
            Map<String, String> summaryHeaders = getSessionHeaders(uuid, path);
            byte[] responseBytes = sendWorkerRequest(path, body, summaryHeaders, "POST");
            VotProtobuf.SessionResponse sessionResponse = decodeValidSessionResponse(responseBytes);

            if (sessionResponse == null) {
                Logger.printDebug(() -> "VOT createSession: summary session request was not accepted; retrying legacy headers");
                Map<String, String> legacyHeaders = getVtransHeaders(
                        path, body, uuid, null, null);
                responseBytes = sendWorkerRequest(path, body, legacyHeaders, "POST");
                sessionResponse = decodeValidSessionResponse(responseBytes);
            }

            if (sessionResponse == null) {
                Logger.printDebug(() -> "VOT createSession: empty or invalid session response");
                return false;
            }

            long now = System.currentTimeMillis() / 1000L;
            long expires = sessionResponse.expires > 0 ? sessionResponse.expires : 3600L;
            sessionUuid = uuid;
            sessionSecretKey = sessionResponse.secretKey;
            sessionExpires = now + Math.max(1L, expires - 60L);
            Logger.printDebug(() -> "VOT createSession: success, expires in " + expires + "s");
            return true;
        } catch (UnknownHostException e) {
            Logger.printException(() -> "VOT createSession failed: DNS resolution error", e);
            return false;
        } catch (SocketTimeoutException e) {
            Logger.printException(() -> "VOT createSession failed: connection timeout", e);
            return false;
        } catch (ConnectException e) {
            Logger.printException(() -> "VOT createSession failed: connection refused", e);
            return false;
        } catch (Exception e) {
            Logger.printException(() -> "VOT createSession failed", e);
            return false;
        }
    }

    @NonNull
    private static Map<String, String> getSessionHeaders(
            @NonNull String uuid,
            @NonNull String path
    ) {
        String tokenData = uuid + ":" + path + ":" + COMPONENT_VERSION;
        String tokenSignature = computeHmacHex(tokenData.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/x-protobuf");
        headers.put("Content-Type", "application/x-protobuf");
        headers.put("User-Agent", USER_AGENT);
        headers.put("X-Ya-Summary-Token", tokenSignature + ":" + tokenData);
        headers.put("X-Ya-Summary-Sk", "");
        return headers;
    }

    @NonNull
    private static Map<String, String> getVtransHeaders(
            @NonNull String path,
            @NonNull byte[] body,
            @Nullable String oauthToken
    ) {
        String uuid = sessionUuid != null ? sessionUuid : generateUuid();
        return getVtransHeaders(path, body, uuid, sessionSecretKey, oauthToken);
    }

    @NonNull
    private static Map<String, String> getVtransHeaders(
            @NonNull String path,
            @NonNull byte[] body,
            @NonNull String uuid,
            @Nullable String secretKey,
            @Nullable String oauthToken
    ) {
        String tokenData = uuid + ":" + path + ":" + COMPONENT_VERSION;
        String tokenSignature = computeHmacHex(tokenData.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/x-protobuf");
        headers.put("Accept-Language", "en");
        headers.put("Content-Type", "application/x-protobuf");
        headers.put("User-Agent", USER_AGENT);
        headers.put("Pragma", "no-cache");
        headers.put("Cache-Control", "no-cache");
        headers.put("Vtrans-Signature", computeHmacHex(body));
        headers.put("Sec-Vtrans-Token", tokenSignature + ":" + tokenData);
        if (secretKey != null && !secretKey.isEmpty()) {
            headers.put("Sec-Vtrans-Sk", secretKey);
        }
        if (oauthToken != null && !oauthToken.isEmpty()) {
            headers.put("Authorization", "OAuth " + oauthToken);
        }
        return headers;
    }

    @Nullable
    private static VotProtobuf.SessionResponse decodeValidSessionResponse(@Nullable byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            VotProtobuf.SessionResponse response = VotProtobuf.decodeSessionResponse(data);
            return response.secretKey == null || response.secretKey.isEmpty() ? null : response;
        } catch (RuntimeException e) {
            Logger.printDebug(() -> "VOT createSession: could not decode response: " + e.getMessage());
            return null;
        }
    }

    private static boolean ensureSession() {
        sessionLock.lock();
        try {
            long now = System.currentTimeMillis() / 1000;
            if (sessionSecretKey != null && !sessionSecretKey.isEmpty() && now < sessionExpires) {
                return true;
            }
            sessionUuid = null;
            sessionSecretKey = null;
            sessionExpires = 0;
            return createSession();
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Returns true when the server specifically reports that live voices are unavailable for the
     * requested language pair. Generic failures must not silently disable live voices.
     */
    public static boolean isLivelyVoiceUnavailableError(@Nullable String message) {
        if (message == null || message.isEmpty()) return false;
        String lower = message.toLowerCase(Locale.US);
        return lower.contains("обычная озвучка") || lower.contains("standard voice");
    }

    private static void resetSession() {
        sessionLock.lock();
        try {
            sessionUuid = null;
            sessionSecretKey = null;
            sessionExpires = 0;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Saves a worker host selected by the user and invalidates state from the previous worker.
     *
     * @param workerHost the worker host to use for future VOT requests
     */
    public static void saveProxyWorkerHost(@NonNull String workerHost) {
        String normalizedWorkerHost = normalizeWorkerHost(workerHost);
        if (normalizedWorkerHost.isEmpty()) {
            return;
        }

        String previousWorkerHost = getWorkerHost();
        Settings.VOT_PROXY_URL.save(normalizedWorkerHost);
        if (!normalizedWorkerHost.equals(previousWorkerHost)) {
            Logger.printDebug(() -> "VOT worker host changed to " + normalizedWorkerHost);
        }
        resetSession();
        clearTranslationCache();
    }

    /** Clears completed translation results, for example after changing the worker host. */
    public static void clearTranslationCache() {
        translationCache.clear();
    }

    private static byte[] sendWorkerRequest(
            @NonNull String path,
            @NonNull byte[] body,
            @NonNull Map<String, String> headers,
            @NonNull String method
    ) throws IOException {
        String workerHost = getWorkerHost();
        String workerUrl = "https://" + workerHost + path;
        Logger.printDebug(() -> "VOT sendWorkerRequest: " + method + " " + workerUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(workerUrl).openConnection();
        try {
            connection.setRequestMethod(method);
            // These are the headers for the outer worker request. The Yandex headers are
            // serialized inside the JSON envelope by writeBinaryWorkerRequest().
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/x-protobuf");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(32 * 1024);

            try (OutputStream os = connection.getOutputStream()) {
                writeBinaryWorkerRequest(os, body, headers);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Logger.printDebug(() -> "VOT sendWorkerRequest: " + workerUrl
                        + " returned " + responseCode);
                return null;
            }

            return readBytes(connection.getInputStream());

        } finally {
            connection.disconnect();
        }
    }

    private static void sendWorkerJsonRequest(String path, String jsonBody) throws IOException {
        String workerHost = getWorkerHost();
        String workerUrl = "https://" + workerHost + path;
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        byte[] payloadBytes = wrapJsonWorkerRequest(jsonBody, headers);

        HttpURLConnection connection = (HttpURLConnection) new URL(workerUrl).openConnection();
        try {
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(payloadBytes.length);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payloadBytes);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                Logger.printDebug(() -> "VOT sendWorkerJsonRequest: " + workerUrl
                        + " returned " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    @NonNull
    private static String getWorkerHost() {
        String workerHost = Settings.VOT_PROXY_URL.get();
        if (workerHost.isEmpty()) {
            workerHost = Settings.VOT_PROXY_URL.defaultValue;
        }

        return normalizeWorkerHost(workerHost);
    }

    @NonNull
    private static String normalizeWorkerHost(@NonNull String workerHost) {
        workerHost = workerHost.trim()
                .replaceFirst("^https?://", "")
                .replaceAll("/+$", "");

        int slashIndex = workerHost.indexOf('/');
        if (slashIndex >= 0) {
            workerHost = workerHost.substring(0, slashIndex);
        }

        return workerHost;
    }

    /**
     * Fetches the current {@code proxyWorkerHost} from the upstream userscript when explicitly
     * requested from the VOT settings. Translation requests never call this method automatically.
     *
     * @return the fetched worker host, or {@code null} if it could not be fetched or parsed
     */
    @Nullable
    public static String fetchLatestProxyWorkerHost() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(VOT_USER_SCRIPT_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            String script;
            try (InputStream inputStream = connection.getInputStream()) {
                script = new String(readBytes(inputStream), StandardCharsets.UTF_8);
            }
            Matcher matcher = PROXY_WORKER_HOST_PATTERN.matcher(script);
            if (!matcher.find()) {
                return null;
            }

            String workerHost = normalizeWorkerHost(Objects.requireNonNull(matcher.group(1)));
            if (workerHost.isEmpty()) {
                return null;
            }

            return workerHost;
        } catch (Exception e) {
            Logger.printDebug(() -> "VOT proxy worker fetch failed: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String computeHmacHex(byte[] data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    HMAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] result = hmac.doFinal(data);

            StringBuilder hex = new StringBuilder();
            for (byte b : result) {
                hex.append(String.format(Locale.US, "%02x", b & 0xFF));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {

            return "";
        }
    }

    private static String generateUuid() {
        String hexDigits = "0123456789ABCDEF";
        Random random = new Random();
        StringBuilder uuid = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            uuid.append(hexDigits.charAt(random.nextInt(16)));
        }
        return uuid.toString();
    }

    private static byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    /**
     * Validates a Yandex OAuth token by calling login.yandex.ru/info.
     * Caches the result so we only call it once per token per process lifetime.
     *
     * @param token the OAuth token to validate
     * @return true if the token is valid, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static synchronized boolean isValidOAuthToken(String token) {
        if (token == null || token.isEmpty()) return false;

        long expiresAt = Settings.VOT_OAUTH_TOKEN_EXPIRES_AT.get();
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            Logger.printDebug(() -> "VOT OAuth token has expired (expiresAt=" + expiresAt + ")");
            lastValidatedToken = null;
            tokenIsValid = false;
            return false;
        }

        // Return cached result if we already validated this exact token.
        if (token.equals(lastValidatedToken)) return tokenIsValid;
        try {
            String url = "https://login.yandex.ru/info?format=json";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            try {
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "OAuth " + token);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                int code = conn.getResponseCode();
                lastValidatedToken = token;
                tokenIsValid = (code == 200);
                Logger.printDebug(() -> "VOT OAuth token validation: HTTP " + code
                        + " -> " + (tokenIsValid ? "valid" : "invalid"));
                return tokenIsValid;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            Logger.printDebug(() -> "VOT OAuth token validation failed: " + e.getMessage());
            // On network error, assume valid so we don't block the user.
            // Do NOT update the cache — the next request will re-validate properly.
            return true;
        }
    }

    /**
     * Clears the OAuth token validation cache.
     * Call when the user signs out so that a new token can be re-validated.
     */
    public static synchronized void clearTokenValidationCache() {
        lastValidatedToken = null;
        tokenIsValid = false;
    }
}
