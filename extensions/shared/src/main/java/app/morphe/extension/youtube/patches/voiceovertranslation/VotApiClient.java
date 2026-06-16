/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Jav1x (https://github.com/Jav1x)
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;

public class VotApiClient {

    private static final String DEFAULT_WORKER_HOST = "vot-worker.toil.cc";
    private static final String VOT_USER_SCRIPT_URL =
            "https://raw.githubusercontent.com/ilyhalight/voice-over-translation/master/dist/vot.user.js";
    private static final Pattern PROXY_WORKER_HOST_PATTERN =
            Pattern.compile("\\bproxyWorkerHost\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern PROXY_WORKER_HOST_MODE_1_PATTERN =
            Pattern.compile("\\bproxyWorkerHostMode1\\s*=\\s*[\"']([^\"']+)[\"']");

    private static final String HMAC_KEY = "bt8xH3VOlb4mqf0nqAibnDOoiPlXsisf";
    private static final String COMPONENT_VERSION = "25.6.0.2259";
    private static final double DEFAULT_DURATION = 343.0;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/134.0.0.0 YaBrowser/25.4.0.0 Safari/537.36";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;

    private static String sessionUuid = null;
    private static String sessionSecretKey = null;
    private static long sessionExpires = 0;
    private static final ReentrantLock sessionLock = new ReentrantLock();

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

    private record ProxyWorkerRetryResult(TranslationResult result, boolean workerHostsFound) {
    }

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
         * @return true if the handler took recovery action and polling should continue
         *         (e.g. disabled live voices), false to stop polling.
         */
        boolean onFailed();

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
        int waitSeconds = Math.max(1, initialWaitSeconds);
        boolean isFirstWait = true;

        for (int retry = 0; retry < DEFAULT_POLL_MAX_RETRIES; retry++) {
            if (handler.isCancelled()) return null;

            try {
                Thread.sleep(waitSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }

            if (handler.isCancelled()) return null;

            TranslationResult result;
            try {
                result = requestTranslation(videoUrl, duration, sourceLang, targetLang, videoTitle);
            } catch (Exception e) {
                Logger.printException(() -> "pollUntilReady: requestTranslation failure", e);
                continue;
            }

            if (result == null) {
                waitSeconds = 5;
                continue;
            }

            int status = result.status();

            if (status == STATUS_FINISHED || status == STATUS_PART_CONTENT) {
                if (result.audioUrl() != null && !result.audioUrl().isEmpty()) {
                    handler.onAudioReady(result);
                    return result;
                }
                return null;
            }

            if (status == STATUS_FAILED) {
                if (handler.onFailed()) {
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
                waitSeconds = waitSeconds1;
                handler.onWaiting(waitSeconds, isFirstWait);
                isFirstWait = false;
                continue;
            }

            if (status == STATUS_AUDIO_REQUESTED) {
                waitSeconds = waitSeconds1;
                handler.onWaiting(waitSeconds, isFirstWait);
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
        // Read OAuth token once (may be null if not configured)
        boolean useLiveVoices = Settings.VOT_USE_LIVE_VOICES.get();
        String oauthToken = useLiveVoices ? Settings.VOT_OAUTH_TOKEN.get() : null;
        if (oauthToken != null && oauthToken.isEmpty()) {
            oauthToken = null;
        }

        // Validate OAuth token before using it (lightweight API call, cached per process)
        if (oauthToken != null && !isValidOAuthToken(oauthToken)) {
            Logger.printDebug(() -> "VOT OAuth token is invalid, clearing and falling back");
            Settings.VOT_OAUTH_TOKEN.save("");
            return new TranslationResult(STATUS_SESSION_REQUIRED, null, 0, null, null);
        }

        final String finalOauthToken = oauthToken;

        try {
            TranslationResult result = requestTranslationInternal(videoUrl, duration, sourceLang, targetLang, videoTitle, finalOauthToken);
            if (result != null) {
                return result;
            }

            return requestTranslationUsingFreshProxyWorkers(
                    videoUrl, duration, sourceLang, targetLang, videoTitle, finalOauthToken, null).result();
        } catch (Exception e) {
            ProxyWorkerRetryResult retryResult = requestTranslationUsingFreshProxyWorkers(
                    videoUrl, duration, sourceLang, targetLang, videoTitle, finalOauthToken, e);

            if (retryResult.result() != null || retryResult.workerHostsFound()) {
                return retryResult.result();
            }

            Logger.printException(() -> "VotApiClient.requestTranslation failed for " + videoUrl, e);
            return null;
        }
    }

    private static TranslationResult requestTranslationInternal(
            String videoUrl, double duration,
            String sourceLang, String targetLang,
            String videoTitle, String oauthToken
    ) throws Exception {
        ensureSession();

        if (duration <= 0) {
            duration = DEFAULT_DURATION;
        }

        String apiSourceLang = (sourceLang == null || sourceLang.isEmpty() || "auto".equalsIgnoreCase(sourceLang))
                ? "" : sourceLang;

        byte[] body = VotProtobuf.encodeTranslationRequest(
                videoUrl, true, duration,
                apiSourceLang, targetLang, videoTitle,
                Settings.VOT_USE_LIVE_VOICES.get()
        );

        String path = "/video-translation/translate";
        String bodySignature = computeHmacHex(body);

        String token = sessionUuid + ":" + path + ":" + COMPONENT_VERSION;
        String tokenSignature = computeHmacHex(token.getBytes(StandardCharsets.UTF_8));

        byte[] responseBytes = sendWorkerRequest(path, body, bodySignature,
                sessionSecretKey, tokenSignature + ":" + token, "POST", oauthToken);

        if (responseBytes == null || responseBytes.length == 0) {

            return null;
        }

        VotProtobuf.TranslationResponse response = VotProtobuf.decodeTranslationResponse(responseBytes);

        return new TranslationResult(
                response.status,
                response.url,
                response.remainingTime,
                response.translationId,
                response.message
        );
    }

    private static ProxyWorkerRetryResult requestTranslationUsingFreshProxyWorkers(
            String videoUrl, double duration,
            String sourceLang, String targetLang,
            String videoTitle, String oauthToken,
            Exception originalException
    ) {
        String[] workerHosts = fetchProxyWorkerHosts();
        if (workerHosts.length == 0) {
            if (originalException != null) {
                Logger.printDebug(() -> "VOT proxy worker refresh found no workers");
            }
            return new ProxyWorkerRetryResult(null, false);
        }

        Exception retryException = originalException;
        for (String workerHost : workerHosts) {
            Settings.VOT_PROXY_URL.save(workerHost);
            resetSession();

            try {
                TranslationResult result = requestTranslationInternal(videoUrl, duration, sourceLang, targetLang, videoTitle, oauthToken);
                if (result != null) {
                    return new ProxyWorkerRetryResult(result, true);
                }
            } catch (Exception e) {
                retryException = e;
            }
        }

        if (retryException != null) {
            Exception exception = retryException;
            Logger.printDebug(() -> "VOT proxy worker retry failed: " + exception.getMessage());
        }
        return new ProxyWorkerRetryResult(null, true);
    }

    public static void sendFailedAudio(String videoUrl) {
        try {
            ensureSession();

            String path = "/video-translation/fail-audio-js";
            String jsonBody = "{\"video_url\":\"" + videoUrl + "\"}";

            sendWorkerJsonRequest(path, jsonBody);
        } catch (Exception e) {
            Logger.printException(() -> "VotApiClient.sendFailedAudio failed for " + videoUrl, e);
        }
    }

    public static void sendEmptyAudio(String videoUrl, String translationId, String oauthToken) {
        try {
            ensureSession();

            byte[] body = VotProtobuf.encodeEmptyAudioRequest(translationId, videoUrl);

            String path = "/video-translation/audio";
            String bodySignature = computeHmacHex(body);

            String token = sessionUuid + ":" + path + ":" + COMPONENT_VERSION;
            String tokenSignature = computeHmacHex(token.getBytes(StandardCharsets.UTF_8));

            sendWorkerRequest(path, body, bodySignature,
                    sessionSecretKey, tokenSignature + ":" + token, "PUT", oauthToken);

        } catch (Exception e) {
            Logger.printException(() -> "VotApiClient.sendEmptyAudio failed for " + videoUrl, e);
        }
    }

    public static boolean sendAudio(String videoUrl, String translationId, String fileId, byte[] audioData) {
        try {
            ensureSession();

            byte[] body = VotProtobuf.encodeAudioRequest(translationId, videoUrl, fileId, audioData);

            return sendAudioRequestBody(body);
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
            ensureSession();

            byte[] body = VotProtobuf.encodePartialAudioRequest(
                    translationId, videoUrl, fileId,
                    audioPartsLength, version, chunkId, audioData
            );

            return sendAudioRequestBody(body);
        } catch (Exception e) {
            Logger.printException(() -> "VotApiClient.sendPartialAudio failed for " + videoUrl, e);
            return false;
        }
    }

    private static boolean sendAudioRequestBody(byte[] body) throws IOException {
        String path = "/video-translation/audio";
        String bodySignature = computeHmacHex(body);

        String token = sessionUuid + ":" + path + ":" + COMPONENT_VERSION;
        String tokenSignature = computeHmacHex(token.getBytes(StandardCharsets.UTF_8));

        return sendWorkerRequest(path, body, bodySignature,
                sessionSecretKey, tokenSignature + ":" + token, "PUT", null) != null;
    }

    private static void ensureSession() throws Exception {
        sessionLock.lock();
        try {
            long now = System.currentTimeMillis() / 1000;
            if (sessionSecretKey != null && now < sessionExpires) {
                return;
            }

            sessionUuid = generateUuid();

            byte[] body = VotProtobuf.encodeSessionRequest(sessionUuid, "video-translation");
            String signature = computeHmacHex(body);

            byte[] responseBytes = sendWorkerRequest("/session/create", body, signature,
                    null, null, "POST", null);

            if (responseBytes == null || responseBytes.length == 0) {
                throw new IOException("Empty session response");
            }

            VotProtobuf.SessionResponse sessionResponse = VotProtobuf.decodeSessionResponse(responseBytes);

            sessionSecretKey = sessionResponse.secretKey;
            sessionExpires = now + sessionResponse.expires - 60;

        } finally {
            sessionLock.unlock();
        }
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

    private static byte[] sendWorkerRequest(
            String path, byte[] body,
            String vtransSignature, String secretKey, String vtransToken,
            String method, String oauthToken
    ) throws IOException {
        String workerHost = getWorkerHost();

        String workerUrl = "https://" + workerHost + path;

        StringBuilder headersJson = getStringBuilder(vtransSignature, secretKey, vtransToken, oauthToken);

        StringBuilder bodyArrayJson = new StringBuilder("[");
        for (int i = 0; i < body.length; i++) {
            if (i > 0) bodyArrayJson.append(",");
            bodyArrayJson.append(body[i] & 0xFF);
        }
        bodyArrayJson.append("]");
        String jsonPayload = "{\"headers\":" + headersJson + ",\"body\":" + bodyArrayJson + "}";

        HttpURLConnection connection = (HttpURLConnection) new URL(workerUrl).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);

            byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payloadBytes.length);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payloadBytes);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {

                return null;
            }

            return readBytes(connection.getInputStream());

        } finally {
            connection.disconnect();
        }
    }

    @NonNull
    private static StringBuilder getStringBuilder(String vtransSignature, String secretKey, String vtransToken, String oauthToken) {
        StringBuilder headersJson = new StringBuilder();
        headersJson.append("{");
        headersJson.append("\"User-Agent\":\"").append(USER_AGENT).append("\",");
        headersJson.append("\"Accept\":\"application/x-protobuf\",");
        headersJson.append("\"Accept-Language\":\"en\",");
        headersJson.append("\"Content-Type\":\"application/x-protobuf\",");
        headersJson.append("\"Pragma\":\"no-cache\",");
        headersJson.append("\"Cache-Control\":\"no-cache\"");

        if (vtransSignature != null) {
            headersJson.append(",\"Vtrans-Signature\":\"").append(vtransSignature).append("\"");
        }
        if (secretKey != null) {
            headersJson.append(",\"Sec-Vtrans-Sk\":\"").append(secretKey).append("\"");
        }
        if (vtransToken != null) {
            headersJson.append(",\"Sec-Vtrans-Token\":\"").append(vtransToken).append("\"");
        }
        if (oauthToken != null && !oauthToken.isEmpty()) {
            headersJson.append(",\"Authorization\":\"OAuth ").append(oauthToken).append("\"");
        }

        headersJson.append("}");
        return headersJson;
    }

    private static void sendWorkerJsonRequest(String path, String jsonBody) throws IOException {
        String workerHost = getWorkerHost();

        String workerUrl = "https://" + workerHost + path;

        String headersJson = "{" +
                "\"User-Agent\":\"" + USER_AGENT + "\"," +
                "\"Content-Type\":\"application/json\"," +
                "\"Accept\":\"application/json\"" +
                "}";

        String payload = "{\"headers\":" + headersJson + ",\"body\":" + jsonBody + "}";

        HttpURLConnection connection = (HttpURLConnection) new URL(workerUrl).openConnection();
        try {
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);

            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payloadBytes.length);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payloadBytes);
            }
        } finally {
            connection.disconnect();
        }
    }

    @NonNull
    private static String getWorkerHost() {
        String workerHost = Settings.VOT_PROXY_URL.get();
        if (workerHost.isEmpty()) {
            workerHost = DEFAULT_WORKER_HOST;
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

    @NonNull
    private static String[] fetchProxyWorkerHosts() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(VOT_USER_SCRIPT_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new String[0];
            }

            String script = new String(readBytes(connection.getInputStream()), StandardCharsets.UTF_8);
            return parseProxyWorkerHosts(script);
        } catch (Exception e) {
            Logger.printDebug(() -> "VOT proxy worker refresh failed: " + e.getMessage());
            return new String[0];
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    private static String[] parseProxyWorkerHosts(@NonNull String script) {
        List<String> workerHosts = new ArrayList<>(2);
        addProxyWorkerHost(workerHosts, script, PROXY_WORKER_HOST_PATTERN);
        addProxyWorkerHost(workerHosts, script, PROXY_WORKER_HOST_MODE_1_PATTERN);

        return workerHosts.toArray(new String[0]);
    }

    private static void addProxyWorkerHost(List<String> workerHosts, String script, Pattern pattern) {
        Matcher matcher = pattern.matcher(script);
        if (matcher.find()) {
            String workerHost = normalizeWorkerHost(Objects.requireNonNull(matcher.group(1)));
            if (!workerHost.isEmpty() && !workerHosts.contains(workerHost)) {
                workerHosts.add(workerHost);
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
