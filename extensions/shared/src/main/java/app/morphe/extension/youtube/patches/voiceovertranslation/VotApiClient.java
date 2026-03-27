/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s) (based on contributions):
 * - Jav1x (https://github.com/Jav1x)
 * - anddea (https://github.com/anddea)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Attribution Notice
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Attribution (Section 7(b)): This specific copyright notice and the
 *    list of original authors above must be preserved in any copy or
 *    derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin (Section 7(c)): Modified versions must be clearly marked as
 *    such (e.g., by adding a "Modified by" line or a new copyright notice).
 *    They must not be misrepresented as the original work.
 *
 * ------------------------------------------------------------------------
 * Version Control Acknowledgement (Non-binding Request)
 * ------------------------------------------------------------------------
 *
 * While not a legal requirement of the GPLv3, the original author(s)
 * respectfully request that ports or substantial modifications retain
 * historical authorship credit in version control systems (e.g., Git),
 * listing original author(s) appropriately and modifiers as committers
 * or co-authors.
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
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;

public class VotApiClient {

    private static final String DEFAULT_WORKER_HOST = "vot-worker.toil.cc";

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

    public record TranslationResult(int status, String audioUrl, int remainingTime,
                                    String translationId, String message) {
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
        String proxyHost = Settings.VOT_PROXY_URL.get();
        if (proxyHost.isEmpty()) {
            proxyHost = DEFAULT_WORKER_HOST;
        }
        proxyHost = proxyHost.replaceFirst("^https?://", "").replaceAll("/+$", "");
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
        try {
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
                    sessionSecretKey, tokenSignature + ":" + token, "POST");

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

        } catch (Exception e) {
            Logger.printException(() -> "VotApiClient.requestTranslation failed for " + videoUrl, e);
            return null;
        }
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

    public static void sendEmptyAudio(String videoUrl, String translationId) {
        try {
            ensureSession();

            byte[] body = VotProtobuf.encodeEmptyAudioRequest(translationId, videoUrl);

            String path = "/video-translation/audio";
            String bodySignature = computeHmacHex(body);

            String token = sessionUuid + ":" + path + ":" + COMPONENT_VERSION;
            String tokenSignature = computeHmacHex(token.getBytes(StandardCharsets.UTF_8));

            sendWorkerRequest(path, body, bodySignature,
                    sessionSecretKey, tokenSignature + ":" + token, "PUT");

        } catch (Exception e) {
            Logger.printException(() -> "VotApiClient.sendEmptyAudio failed for " + videoUrl, e);
        }
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
                    null, null, "POST");

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

    private static byte[] sendWorkerRequest(
            String path, byte[] body,
            String vtransSignature, String secretKey, String vtransToken,
            String method
    ) throws IOException {
        String workerHost = Settings.VOT_PROXY_URL.get();
        if (workerHost.isEmpty()) {
            workerHost = DEFAULT_WORKER_HOST;
        }

        String workerUrl = "https://" + workerHost + path;

        StringBuilder headersJson = getStringBuilder(vtransSignature, secretKey, vtransToken);

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
    private static StringBuilder getStringBuilder(String vtransSignature, String secretKey, String vtransToken) {
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

        headersJson.append("}");
        return headersJson;
    }

    private static void sendWorkerJsonRequest(String path, String jsonBody) throws IOException {
        String workerHost = Settings.VOT_PROXY_URL.get();
        if (workerHost.isEmpty()) {
            workerHost = DEFAULT_WORKER_HOST;
        }

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
}
