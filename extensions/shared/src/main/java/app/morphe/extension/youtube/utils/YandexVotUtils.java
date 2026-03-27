/*
 * Copyright (C) 2025 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
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

package app.morphe.extension.youtube.utils;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.morphe.extension.shared.utils.Logger;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.youtube.shared.VideoInformation.getVideoTitle;

/**
 * Utility class for interacting with Yandex API to fetch and process video subtitles.
 */
public class YandexVotUtils {
    // --- Constants ---
    private static final String YANDEX_HOST = "api.browser.yandex.ru";
    private static final String BASE_URL = "https://" + YANDEX_HOST;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 YaBrowser/25.4.0.0 Safari/537.36";
    private static final String HMAC_KEY = "bt8xH3VOlb4mqf0nqAibnDOoiPlXsisf";
    private static final String COMPONENT_VERSION = "25.4.3.870";
    private static final String SESSION_MODULE = "video-translation";
    private static final String SEC_CH_UA = "\"Chromium\";v=\"134\", \"YaBrowser\";v=\"" + COMPONENT_VERSION.substring(0, COMPONENT_VERSION.indexOf('.')) + "\", \"Not?A_Brand\";v=\"24\", \"Yowser\";v=\"2.5\"";
    private static final String SEC_CH_UA_FULL = "\"Chromium\";v=\"134.0.6998.1973\", \"YaBrowser\";v=\"" + COMPONENT_VERSION + "\", \"Not?A_Brand\";v=\"24.0.0.0\", \"Yowser\";v=\"2.5\"";

    // API Paths
    private static final String PATH_SESSION_CREATE = "/session/create";
    private static final String PATH_TRANSLATE = "/video-translation/translate";
    private static final String PATH_GET_SUBTITLES = "/video-subtitles/get-subtitles";
    private static final String PATH_FAIL_AUDIO_JS = "/video-translation/fail-audio-js";
    private static final String PATH_SEND_AUDIO = "/video-translation/audio";

    // Header Prefixes
    private static final String VSUBS_PREFIX = "Vsubs";
    private static final String VTRANS_PREFIX = "Vtrans";

    // API Status Codes (from ManualVideoTranslationResponse)
    private static final int STATUS_FAILED = 0;             // Translation failed or cannot be translated
    private static final int STATUS_SUCCESS = 1;            // Translation completed successfully or already exists
    private static final int STATUS_PROCESSING = 2;         // Translation is in progress (short wait expected)
    private static final int STATUS_LONG_PROCESSING = 3;    // Translation is in progress (long wait expected)
    private static final int STATUS_PART_CONTENT = 5;       // Often means finished audio/subs available
    private static final int STATUS_AUDIO_REQUESTED = 6;    // YouTube specific, treat as processing

    // File ID for YouTube Status 6 audio request
    private static final String FILE_ID_YOUTUBE_STATUS_6 = "web_api_get_all_generating_urls_data_from_iframe";

    // Configuration
    private static final long DEFAULT_VIDEO_DURATION_SECONDS = 343; // Default, but request uses actual if available
    private static final int MIN_POLLING_INTERVAL_MS = 5000;  // Poll at least every 5 seconds
    private static final int MAX_POLLING_INTERVAL_MS = 60000; // Poll at most every 60 seconds
    private static final int POLLING_TIME_BUFFER_MS = 2000; // Add 2-second buffer to remainingTime
    private static final long WORKFLOW_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes total timeout

    private static final int MAX_STUCK_POLLS = 3;
    private static final String YANDEX_ERROR_SERVER_TRY_AGAIN = "Возникла ошибка при переводе, попробуйте позже";

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    // --- State & Utilities ---
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();
    private static final ReentrantLock sessionLock = new ReentrantLock();
    private static final Map<String, AtomicBoolean> urlCancellationFlags = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> urlWorkflowLocks = new ConcurrentHashMap<>();
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private static volatile SessionInfo currentSession = null;

    private static final ExecutorService workflowExecutor = Executors.newCachedThreadPool();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // region Session Management

    /**
     * Ensures a valid Yandex API session exists, creating a new one if necessary.
     * Thread-safe.
     *
     * @return A valid {@link SessionInfo} object.
     * @throws IOException              If a network error occurs during session creation.
     * @throws GeneralSecurityException If a security error occurs during signature calculation.
     */
    private static SessionInfo ensureSession() throws IOException, GeneralSecurityException {
        SessionInfo session = currentSession;
        if (session != null && session.isValid()) {
            Logger.printDebug(() -> "VOT: Using existing valid session.");
            return session;
        }
        sessionLock.lock();
        try {
            session = currentSession;
            if (session != null && session.isValid()) {
                Logger.printDebug(() -> "VOT: Using existing valid session after lock.");
                return session;
            }
            Logger.printInfo(() -> "VOT: Creating new Yandex session...");
            currentSession = createNewSession();
            Logger.printInfo(() -> "VOT: New Yandex session created successfully.");
            return currentSession;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Creates a new Yandex API session.
     * <p>
     * HTTP Method: POST
     * <br>
     * Endpoint: /session/create (PATH_SESSION_CREATE)
     *
     * @return The newly created {@link SessionInfo}.
     * @throws IOException              If a network error occurs or the response is invalid.
     * @throws GeneralSecurityException If a security error occurs during signature calculation.
     */
    private static SessionInfo createNewSession() throws IOException, GeneralSecurityException {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        Logger.printDebug(() -> "VOT: Creating session with module: " + SESSION_MODULE);

        ManualYandexSessionRequest requestProto = new ManualYandexSessionRequest();
        requestProto.uuid = uuid;
        requestProto.module = SESSION_MODULE;

        byte[] requestBodyBytes = requestProto.toByteArray();
        String signature = calculateSignature(requestBodyBytes);
        Headers headers = new Headers.Builder()
                .add("User-Agent", USER_AGENT)
                .add("Accept", "application/x-protobuf")
                .add("Content-Type", "application/x-protobuf")
                .add("Pragma", "no-cache")
                .add("Cache-Control", "no-cache")
                .add("Vtrans-Signature", signature)
                .add("sec-ch-ua", SEC_CH_UA)
                .add("sec-ch-ua-full-version-list", SEC_CH_UA_FULL)
                .add("Sec-Fetch-Mode", "no-cors")
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + PATH_SESSION_CREATE)
                .headers(headers)
                .post(RequestBody.create(requestBodyBytes, MediaType.parse("application/x-protobuf")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                assert response.body() != null;
                String bodyString = response.body().string();
                Logger.printException(() -> "VOT: Failed to create session: " + response.code() + " " + response.message() + ", Body: " + bodyString);
                throw new IOException("Failed to create session: " + response.code());
            }
            assert response.body() != null;
            byte[] responseBytes = response.body().bytes();
            ManualYandexSessionResponse sessionResponse = ManualYandexSessionResponse.parseFrom(responseBytes);
            if (TextUtils.isEmpty(sessionResponse.secretKey)) {
                Logger.printException(() -> "VOT: Invalid session response (missing key)");
                throw new IOException("Invalid session response (missing key)");
            }
            Logger.printDebug(() -> "VOT: Parsed session - SecretKey: " + sessionResponse.secretKey.substring(0, Math.min(sessionResponse.secretKey.length(), 10)) + "... , Expires: " + sessionResponse.expires + "s");
            return new SessionInfo(uuid, sessionResponse.secretKey, sessionResponse.expires);
        }
    }

    // endregion Session Management

    // region API Calls

    /**
     * Initiates an asynchronous workflow to fetch Yandex translated subtitles for a video URL.
     * Manages session creation, subtitle checking, translation requests, polling, and subtitle fetching.
     * Ensures only one workflow runs per URL at a time.
     *
     * @param videoUrl           The YouTube video URL.
     * @param durationSeconds    The video duration in seconds.
     * @param originalTargetLang The desired target language code (e.g., "en", "es", "de").
     * @param callback           The {@link SubtitleWorkflowCallback} for progress and results.
     */
    public static void getYandexSubtitlesWorkflowAsync(
            String videoUrl,
            double durationSeconds,
            String originalTargetLang,
            SubtitleWorkflowCallback callback
    ) {
        Logger.printInfo(() -> "VOT: Starting workflow for URL: " + videoUrl + ", TargetLang: " + originalTargetLang);

        String yandexTargetLang = "en".equals(originalTargetLang) || "ru".equals(originalTargetLang) || "kk".equals(originalTargetLang)
                ? originalTargetLang
                : "en";
        if (!originalTargetLang.equals(yandexTargetLang)) {
            Logger.printInfo(() -> "VOT: Unsupported language " + originalTargetLang + ". Using intermediate language: " + yandexTargetLang);
        }

        AtomicBoolean lock = urlWorkflowLocks.computeIfAbsent(videoUrl, k -> new AtomicBoolean(false));
        if (!lock.compareAndSet(false, true)) {
            Logger.printInfo(() -> "VOT: Workflow already running for " + videoUrl);
            return;
        }

        AtomicBoolean isCancelled = urlCancellationFlags.computeIfAbsent(videoUrl, k -> new AtomicBoolean(false));

        SubtitleWorkflowCallback wrappedCallback = new SubtitleWorkflowCallback() {
            private final AtomicBoolean finalCalled = new AtomicBoolean(false);

            @Override
            public void onFinalSuccess(TreeMap<Long, Pair<Long, String>> parsedSubtitles) {
                if (finalCalled.compareAndSet(false, true)) {
                    Logger.printDebug(() -> "VOT: Final success for " + videoUrl + ". Releasing lock.");
                    try {
                        callback.onFinalSuccess(parsedSubtitles);
                    } finally {
                        cleanupWorkflow(videoUrl);
                    }
                }
            }

            @Override
            public void onIntermediateSuccess(String rawIntermediateJson, String intermediateLang) {
                if (!finalCalled.get() && !isCancelled.get()) {
                    Logger.printDebug(() -> "VOT: Intermediate success for " + videoUrl + " (Lang: " + intermediateLang + ")");
                    callback.onIntermediateSuccess(rawIntermediateJson, intermediateLang);
                }
            }

            @Override
            public void onFinalFailure(String errorMessage) {
                if (finalCalled.compareAndSet(false, true)) {
                    Logger.printDebug(() -> "VOT: Final failure for " + videoUrl + ". Releasing lock.");
                    try {
                        callback.onFinalFailure(errorMessage);
                    } finally {
                        cleanupWorkflow(videoUrl);
                    }
                }
            }

            @Override
            public void onProcessingStarted(String statusMessage) {
                if (!finalCalled.get() && !isCancelled.get()) {
                    callback.onProcessingStarted(statusMessage);
                }
            }
        };

        WorkflowState state = new WorkflowState(videoUrl, durationSeconds, originalTargetLang, yandexTargetLang, wrappedCallback, isCancelled);
        workflowExecutor.submit(() -> startWorkflow(state));
    }

    /**
     * The entry point for the background workflow thread.
     *
     * @param state The current workflow state.
     */
    private static void startWorkflow(WorkflowState state) {
        try {
            Logger.printInfo(() -> "VOT: Step 1/3 - Ensuring session...");
            if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled before start");
            SessionInfo session = ensureSession();
            postToMainThread(() -> state.callback.onProcessingStarted(str("revanced_yandex_status_session_ok")));

            Logger.printInfo(() -> "VOT: Step 2/3 - Checking existing subtitles for lang: " + state.yandexTargetLang);
            if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled during subtitle check");
            ManualSubtitlesResponse subsResponse = getFinalSubtitleTracks(state.videoUrl, session);
            if (subsResponse != null && !subsResponse.waiting) {
                ManualSubtitlesObject chosenSub = findBestSubtitleForLanguage(subsResponse.subtitles, state.yandexTargetLang);
                String subtitleUrl = chosenSub != null ? determineSubtitleUrl(chosenSub, state.yandexTargetLang) : null;
                if (chosenSub != null && !TextUtils.isEmpty(subtitleUrl)) {
                    Logger.printInfo(() -> "VOT: Found existing subtitles for " + state.yandexTargetLang + ". Skipping translation.");
                    postToMainThread(() -> state.callback.onProcessingStarted(str("revanced_yandex_status_subs_found")));
                    processAndFetchFinalSubtitles(subsResponse, state.originalTargetLang, state.yandexTargetLang, state.callback);
                    return;
                }
            }

            Logger.printInfo(() -> "VOT: Step 3/3 - Requesting translation for " + state.yandexTargetLang);
            postToMainThread(() -> state.callback.onProcessingStarted(str("revanced_yandex_status_requesting_translation")));

            // Start the first poll immediately
            pollForTranslation(state);
        } catch (InterruptedException e) {
            Logger.printInfo(() -> "VOT: Workflow cancelled for " + state.videoUrl);
            postToMainThread(() -> state.callback.onFinalFailure(str("revanced_gemini_cancelled")));
        } catch (Exception e) {
            Logger.printException(() -> "VOT: Workflow failed during initialization: " + e.getMessage(), e);
            String userMessage = (e instanceof IOException || e instanceof GeneralSecurityException)
                    ? (e.getMessage() != null ? e.getMessage() : str("revanced_yandex_error_network_generic"))
                    : str("revanced_yandex_error_unknown") + (e.getMessage() != null ? ": " + e.getMessage() : "");
            postToMainThread(() -> state.callback.onFinalFailure(userMessage));
        }
    }

    /**
     * Performs a single poll for translation status and schedules the next one if needed.
     *
     * @param state The current workflow state.
     */
    private static void pollForTranslation(WorkflowState state) {
        workflowExecutor.submit(() -> {
            try {
                if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled");
                if (System.currentTimeMillis() - state.startTime >= WORKFLOW_TIMEOUT_MS) {
                    throw new IOException("Workflow timeout after " + (WORKFLOW_TIMEOUT_MS / 1000) + "s");
                }

                SessionInfo session = ensureSession();
                String videoTitle = getVideoTitle();
                ManualVideoTranslationResponse transResponse = requestTranslation(state.videoUrl, state.yandexTargetLang, session, state.durationSeconds, videoTitle);

                Logger.printInfo(() -> "VOT: Poll - Status: " + transResponse.status +
                        ", RemainingTime: " + transResponse.remainingTime + "s, Message: " + transResponse.message);

                switch (transResponse.status) {
                    case STATUS_SUCCESS:
                    case STATUS_PART_CONTENT:
                        Logger.printInfo(() -> "VOT: Translation completed for " + state.yandexTargetLang);
                        if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled");
                        ManualSubtitlesResponse subsResponse = getFinalSubtitleTracks(state.videoUrl, session);
                        processAndFetchFinalSubtitles(subsResponse, state.originalTargetLang, state.yandexTargetLang, state.callback);
                        return; // Workflow complete

                    case STATUS_AUDIO_REQUESTED:
                        Logger.printInfo(() -> "VOT: Handling STATUS_AUDIO_REQUESTED for YouTube");
                        postToMainThread(() -> state.callback.onProcessingStarted(str("revanced_yandex_status_youtube_specific")));
                        if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled");
                        sendFailAudioJsRequest(state.videoUrl);
                        if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled");
                        sendAudioRequest(state.videoUrl, transResponse.translationId, session);
                        // Schedule next poll after a short delay
                        scheduler.schedule(() -> pollForTranslation(state), 1, TimeUnit.SECONDS);
                        break;

                    case STATUS_PROCESSING:
                    case STATUS_LONG_PROCESSING:
                        if (!state.isStuck) {
                            if (transResponse.remainingTime > 0 && transResponse.remainingTime == state.lastRemainingTime) {
                                state.stuckPollCount++;
                            } else {
                                state.stuckPollCount = 0; // Reset counter if time changes
                            }
                            state.lastRemainingTime = transResponse.remainingTime;

                            if (state.stuckPollCount >= MAX_STUCK_POLLS) {
                                Logger.printInfo(() -> "VOT: Poll is now considered stuck. Setting persistent delayed state.");
                                state.isStuck = true;
                            }
                        }

                        if (state.isStuck) {
                            postToMainThread(() -> state.callback.onProcessingStarted(str("revanced_yandex_status_transcription_delayed")));
                        } else {
                            String waitMsg = secsToStrTime(transResponse.remainingTime);
                            postToMainThread(() -> state.callback.onProcessingStarted(waitMsg));
                        }

                        long delayMs = calculateSleepTime(transResponse.remainingTime);
                        Logger.printDebug(() -> "VOT: Scheduling next poll in " + (delayMs / 1000.0) + "s");
                        scheduler.schedule(() -> pollForTranslation(state), delayMs, TimeUnit.MILLISECONDS);
                        break;

                    case STATUS_FAILED:
                    default:
                        String errMsg = str("revanced_yandex_error_translation_failed") +
                                (TextUtils.isEmpty(transResponse.message) ? "" : ": " + translateServerMessage(transResponse.message));
                        throw new IOException(errMsg);
                }
            } catch (InterruptedException e) {
                Logger.printInfo(() -> "VOT: Polling cancelled for " + state.videoUrl);
                postToMainThread(() -> state.callback.onFinalFailure(str("revanced_gemini_cancelled")));
            } catch (Exception e) {
                Logger.printException(() -> "VOT: Polling failed: " + e.getMessage(), e);
                String userMessage = (e instanceof IOException || e instanceof GeneralSecurityException)
                        ? (e.getMessage() != null ? e.getMessage() : str("revanced_yandex_error_network_generic"))
                        : str("revanced_yandex_error_unknown") + (e.getMessage() != null ? ": " + translateServerMessage(e.getMessage()) : "");
                postToMainThread(() -> state.callback.onFinalFailure(userMessage));
            }
        });
    }

    /**
     * Internal state holder for a single translation workflow.
     */
    private static class WorkflowState {
        final String videoUrl;
        final double durationSeconds;
        final String originalTargetLang;
        final String yandexTargetLang;
        final SubtitleWorkflowCallback callback;
        final AtomicBoolean isCancelled;
        final long startTime = System.currentTimeMillis();

        int lastRemainingTime = -1;
        int stuckPollCount = 0;
        boolean isStuck = false;

        WorkflowState(String videoUrl, double durationSeconds, String originalTargetLang, String yandexTargetLang, SubtitleWorkflowCallback callback, AtomicBoolean isCancelled) {
            this.videoUrl = videoUrl;
            this.durationSeconds = durationSeconds;
            this.originalTargetLang = originalTargetLang;
            this.yandexTargetLang = yandexTargetLang;
            this.callback = callback;
            this.isCancelled = isCancelled;
        }
    }

    /**
     * Cleans up workflow resources (locks and cancellation flags) for a video URL.
     */
    private static void cleanupWorkflow(@Nullable String videoUrl) {
        if (videoUrl == null) return;
        urlWorkflowLocks.remove(videoUrl);
        urlCancellationFlags.remove(videoUrl);
        Logger.printDebug(() -> "VOT: Cleaned up workflow resources for " + videoUrl);
    }

    /**
     * Requests video translation from Yandex API, either initiating or polling status.
     *
     * @param videoUrl         The video URL.
     * @param yandexTargetLang The Yandex target language ('en', 'ru', 'kk').
     * @param session          The current session information.
     * @param durationSeconds  The video duration in seconds.
     * @return The parsed {@link ManualVideoTranslationResponse}.
     * @throws IOException              If a network error occurs.
     * @throws GeneralSecurityException If a crypto error occurs.
     */
    private static ManualVideoTranslationResponse requestTranslation(
            String videoUrl, String yandexTargetLang, SessionInfo session, double durationSeconds, String videoTitle
    ) throws IOException, GeneralSecurityException {
        ManualVideoTranslationRequest requestProto = new ManualVideoTranslationRequest();
        requestProto.url = videoUrl;
        requestProto.duration = durationSeconds > 0 ? durationSeconds : DEFAULT_VIDEO_DURATION_SECONDS;
        requestProto.language = "auto";
        requestProto.responseLanguage = yandexTargetLang;
        requestProto.firstRequest = true;
        requestProto.videoTitle = videoTitle;

        byte[] requestBodyBytes = requestProto.toByteArray();
        Logger.printDebug(() -> "VOT: Translation request body: " + Arrays.toString(requestBodyBytes));
        Headers headers = buildRequestHeaders(session, requestBodyBytes, PATH_TRANSLATE, VTRANS_PREFIX);

        StringBuilder headersLog = new StringBuilder("VOT: HTTP Headers for translation request:\n");
        headers.toMultimap().forEach((key, values) -> {
            String value = String.join(", ", values);
            headersLog.append("  ").append(key).append(": ").append(value).append("\n");
        });
        Logger.printInfo(headersLog::toString);

        Request request = new Request.Builder()
                .url(BASE_URL + PATH_TRANSLATE)
                .headers(headers)
                .post(RequestBody.create(requestBodyBytes, MediaType.parse("application/x-protobuf")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                assert response.body() != null;
                String bodyString = response.body().string();
                Logger.printException(() -> "VOT: Translation request failed: " + response.code() + " " + response.message() + ", Body: " + bodyString);
                throw new IOException("API Error: " + response.code());
            }
            assert response.body() != null;
            byte[] responseBytes = response.body().bytes();
            Logger.printDebug(() -> "VOT: Translation response body: " + Arrays.toString(responseBytes));
            return ManualVideoTranslationResponse.parseFrom(responseBytes);
        }
    }

    /**
     * Fetches subtitle tracks after translation or for initial check.
     *
     * @param videoUrl The video URL.
     * @param session  The current valid {@link SessionInfo}.
     * @return A {@link ManualSubtitlesResponse} or null if the request fails.
     * @throws IOException              If a network error occurs or the response cannot be parsed.
     * @throws GeneralSecurityException If a security error occurs during header generation.
     */
    @Nullable
    private static ManualSubtitlesResponse getFinalSubtitleTracks(String videoUrl, SessionInfo session) throws IOException, GeneralSecurityException {
        ManualSubtitlesRequest requestProto = new ManualSubtitlesRequest();
        requestProto.url = videoUrl;
        requestProto.language = "auto";

        byte[] requestBodyBytes = requestProto.toByteArray();
        Headers headers = buildRequestHeaders(session, requestBodyBytes, PATH_GET_SUBTITLES, VSUBS_PREFIX);
        Request request = new Request.Builder()
                .url(BASE_URL + PATH_GET_SUBTITLES)
                .headers(headers)
                .post(RequestBody.create(requestBodyBytes, MediaType.parse("application/x-protobuf")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                assert response.body() != null;
                String bodyString = response.body().string();
                Logger.printException(() -> "VOT: Failed to get subtitle tracks: " + response.code() + " " + response.message() + ", Body: " + bodyString);
                return null;
            }
            assert response.body() != null;
            ManualSubtitlesResponse subResponse = ManualSubtitlesResponse.parseFrom(response.body().bytes());
            String availableSubsLog = subResponse.subtitles != null && !subResponse.subtitles.isEmpty()
                    ? subResponse.subtitles.stream()
                    .map(s -> (s.translatedLanguage != null ? s.language + "->" + s.translatedLanguage : s.language)
                            + (TextUtils.isEmpty(s.url) && TextUtils.isEmpty(s.translatedUrl) ? "(X)" : ""))
                    .collect(Collectors.joining(", "))
                    : "None";
            Logger.printInfo(() -> "VOT: Subtitle tracks response - Waiting: " + subResponse.waiting + ", Subtitles: [" + availableSubsLog + "]");
            return subResponse;
        }
    }

    /**
     * Sends a PUT request to /video-translation/fail-audio-js for YouTube status 6 handling.
     *
     * @param videoUrl The video URL.
     */
    private static void sendFailAudioJsRequest(String videoUrl) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("video_url", videoUrl);
        } catch (JSONException e) {
            Logger.printException(() -> "VOT: Failed to create JSON body for fail-audio-js", e);
            return;
        }

        Headers headers = new Headers.Builder()
                .add("User-Agent", USER_AGENT)
                .add("Accept", "*/*")
                .add("Content-Type", "application/json")
                .add("Origin", BASE_URL)
                .add("Referer", BASE_URL + "/")
                .add("sec-ch-ua", SEC_CH_UA)
                .add("sec-ch-ua-mobile", "?0")
                .add("sec-ch-ua-platform", "\"Windows\"")
                .add("Sec-Fetch-Mode", "cors")
                .add("Sec-Fetch-Dest", "empty")
                .add("Sec-Fetch-Site", "cross-site")
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + PATH_FAIL_AUDIO_JS)
                .headers(headers)
                .put(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                assert response.body() != null;
                String bodyString = response.body().string();
                Logger.printException(() -> "VOT: Failed fail-audio-js request: " + response.code() + " " + response.message() + ", Body: " + bodyString);
            } else {
                Logger.printDebug(() -> "VOT: Successfully sent fail-audio-js request: " + response.code());
            }
        } catch (IOException e) {
            Logger.printException(() -> "VOT: Network error in fail-audio-js request", e);
        }
    }

    /**
     * Sends a PUT request to /video-translation/audio for YouTube status 6 handling.
     *
     * @param videoUrl      The video URL.
     * @param translationId The translation ID from the status 6 response.
     * @param session       The current valid {@link SessionInfo}.
     */
    private static void sendAudioRequest(String videoUrl, String translationId, SessionInfo session) {
        ManualVideoTranslationAudioRequest requestProto = new ManualVideoTranslationAudioRequest();
        requestProto.url = videoUrl;
        requestProto.translationId = translationId != null ? translationId : "";
        requestProto.audioInfo = new ManualAudioBufferObject();
        requestProto.audioInfo.fileId = FILE_ID_YOUTUBE_STATUS_6;
        requestProto.audioInfo.audioFile = new byte[0];

        try {
            byte[] requestBodyBytes = requestProto.toByteArray();
            Headers headers = buildRequestHeaders(session, requestBodyBytes, PATH_SEND_AUDIO, VTRANS_PREFIX);
            Request request = new Request.Builder()
                    .url(BASE_URL + PATH_SEND_AUDIO)
                    .headers(headers)
                    .put(RequestBody.create(requestBodyBytes, MediaType.parse("application/x-protobuf")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    assert response.body() != null;
                    String bodyString = response.body().string();
                    Logger.printException(() -> "VOT: Failed /audio request: " + response.code() + " " + response.message() + ", Body: " + bodyString);
                } else {
                    Logger.printDebug(() -> "VOT: Successfully sent /audio request: " + response.code());
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            Logger.printException(() -> "VOT: Error in /audio request", e);
        }
    }

    // endregion API Calls

    // region Utils

    /**
     * Calculates the polling delay based on remaining time.
     *
     * @param remainingTimeSecs The estimated remaining time from the API response (in seconds).
     * @return The sleep time in milliseconds.
     */
    private static long calculateSleepTime(int remainingTimeSecs) {
        long remainingMs = (long) remainingTimeSecs * 1000;
        return Math.max(MIN_POLLING_INTERVAL_MS,
                Math.min(MAX_POLLING_INTERVAL_MS,
                        (remainingMs >= 0 ? remainingMs : MIN_POLLING_INTERVAL_MS) + POLLING_TIME_BUFFER_MS));
    }

    /**
     * Processes and fetches final subtitle content, selecting the best track and downloading it.
     *
     * @param response           The {@link ManualSubtitlesResponse} from the API.
     * @param originalTargetLang The user's desired language.
     * @param yandexTargetLang   The Yandex requested language.
     * @param callback           The callback for results or errors.
     */
    private static void processAndFetchFinalSubtitles(
            @Nullable ManualSubtitlesResponse response,
            String originalTargetLang,
            String yandexTargetLang,
            SubtitleWorkflowCallback callback
    ) {
        if (response == null) {
            Logger.printException(() -> "VOT: Null subtitle response");
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_internal_null_subs_response")));
            return;
        }

        if (response.waiting) {
            Logger.printInfo(() -> "VOT: Subtitles still processing after polling success");
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_subs_stuck_processing")));
            return;
        }

        List<ManualSubtitlesObject> availableSubs = response.subtitles;
        if (availableSubs == null || availableSubs.isEmpty()) {
            Logger.printInfo(() -> "VOT: No subtitle tracks returned");
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_no_subs_returned")));
            return;
        }

        String log = availableSubs.stream()
                .map(s -> "{Orig:" + s.language + (TextUtils.isEmpty(s.url) ? "(X)" : "")
                        + (s.translatedLanguage != null ? ",Trans:" + s.translatedLanguage + (TextUtils.isEmpty(s.translatedUrl) ? "(X)" : "") : "") + "}")
                .collect(Collectors.joining(", "));
        Logger.printInfo(() -> "VOT: Processing tracks - OriginalTarget: " + originalTargetLang + ", YandexTarget: " + yandexTargetLang + ", Available: [" + log + "]");

        ManualSubtitlesObject chosenSub = findBestSubtitleForLanguage(availableSubs, yandexTargetLang);
        if (chosenSub == null) {
            Logger.printInfo(() -> "VOT: No suitable track for " + yandexTargetLang);
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_no_subs_for_language", yandexTargetLang)));
            return;
        }

        String subtitleUrl = determineSubtitleUrl(chosenSub, yandexTargetLang);
        if (TextUtils.isEmpty(subtitleUrl)) {
            Logger.printException(() -> "VOT: Chosen track for " + yandexTargetLang + " has no valid URL");
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_internal_no_chosen_url")));
            return;
        }

        Logger.printInfo(() -> "VOT: Fetching subtitle from: " + subtitleUrl);
        fetchSubtitleContent(subtitleUrl, originalTargetLang, yandexTargetLang, callback);
    }

    /**
     * Selects the best subtitle track based on the target language.
     *
     * @param subs       The list of available subtitle tracks.
     * @param targetLang The desired target language.
     * @return The best matching {@link ManualSubtitlesObject} or null.
     */
    @Nullable
    private static ManualSubtitlesObject findBestSubtitleForLanguage(List<ManualSubtitlesObject> subs, String targetLang) {
        if (subs == null || subs.isEmpty()) return null;

        ManualSubtitlesObject translatedMatch = null;
        for (ManualSubtitlesObject sub : subs) {
            if (targetLang.equals(sub.translatedLanguage) && !TextUtils.isEmpty(sub.translatedUrl)) {
                translatedMatch = sub;
                break;
            }
        }

        if (translatedMatch != null) {
            Logger.printInfo(() -> "VOT: Selected translated track for " + targetLang);
            return translatedMatch;
        }

        for (ManualSubtitlesObject sub : subs) {
            if (targetLang.equals(sub.language) && !TextUtils.isEmpty(sub.url)) {
                Logger.printInfo(() -> "VOT: Selected original track for " + targetLang);
                return sub;
            }
        }

        Logger.printInfo(() -> "VOT: No suitable subtitle track for " + targetLang);
        return null;
    }

    /**
     * Determines the appropriate subtitle URL from a chosen subtitle object.
     *
     * @param chosenSub  The selected subtitle track.
     * @param targetLang The desired target language.
     * @return The subtitle URL or null if invalid.
     */
    @Nullable
    private static String determineSubtitleUrl(@NonNull ManualSubtitlesObject chosenSub, @NonNull String targetLang) {
        if (targetLang.equals(chosenSub.translatedLanguage) && !TextUtils.isEmpty(chosenSub.translatedUrl)) {
            Logger.printDebug(() -> "VOT: Using translated URL: " + chosenSub.translatedUrl);
            return chosenSub.translatedUrl;
        }
        if (targetLang.equals(chosenSub.language) && !TextUtils.isEmpty(chosenSub.url)) {
            Logger.printDebug(() -> "VOT: Using original URL: " + chosenSub.url);
            return chosenSub.url;
        }
        if (!TextUtils.isEmpty(chosenSub.translatedUrl)) {
            Logger.printDebug(() -> "VOT: Fallback to translated URL: " + chosenSub.translatedUrl);
            return chosenSub.translatedUrl;
        }
        Logger.printException(() -> "VOT: No valid URL for track: Orig=" + chosenSub.language + ", Trans=" + chosenSub.translatedLanguage + ", Target=" + targetLang);
        return null;
    }

    /**
     * Fetches subtitle content asynchronously and processes it based on language requirements.
     *
     * @param url                The subtitle file URL.
     * @param originalTargetLang The user's desired language.
     * @param yandexTargetLang   The Yandex requested language.
     * @param callback           The callback for results or errors.
     */
    private static void fetchSubtitleContent(String url, String originalTargetLang, String yandexTargetLang, SubtitleWorkflowCallback callback) {
        Request request = new Request.Builder().url(url).get().build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.printException(() -> "VOT: Failed to fetch subtitle content: " + url, e);
                postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_network_subs_fetch")));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        assert body != null;
                        String bodyPreview = body.source().peek().readString(1024, StandardCharsets.UTF_8);
                        Logger.printException(() -> "VOT: Failed to download subtitle: " + response.code() + " " + response.message() + ", Preview: " + bodyPreview);
                        postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_download_subs_failed", response.code())));
                        return;
                    }

                    assert body != null;
                    String subtitleText = body.string();

                    Logger.printInfo(() -> "VOT: Fetched subtitle content (" + yandexTargetLang + ", " + subtitleText.length() + " chars)");
                    if (!originalTargetLang.equals(yandexTargetLang)) {
                        Logger.printInfo(() -> "VOT: Passing raw JSON for secondary translation to " + originalTargetLang);
                        postToMainThread(() -> callback.onIntermediateSuccess(subtitleText, yandexTargetLang));
                    } else {
                        TreeMap<Long, Pair<Long, String>> parsedData = parseYandexJsonSubtitles(subtitleText);
                        if (parsedData == null) {
                            Logger.printException(() -> "VOT: Failed to parse subtitle JSON");
                            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_subs_parsing_failed")));
                            return;
                        }
                        Logger.printInfo(() -> "VOT: Parsed " + parsedData.size() + " subtitle entries for " + originalTargetLang);
                        postToMainThread(() -> callback.onFinalSuccess(parsedData));
                    }
                } catch (Exception e) {
                    Logger.printException(() -> "VOT: Error processing subtitle content: " + url, e);
                    postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_subs_processing_failed") + ": " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Parses Yandex subtitle JSON into a structured format.
     *
     * @param jsonContent The raw JSON subtitle content.
     * @return A {@link TreeMap} of subtitle entries or null if parsing fails.
     * @throws JSONException If the JSON is invalid.
     */
    @Nullable
    static TreeMap<Long, Pair<Long, String>> parseYandexJsonSubtitles(String jsonContent) throws JSONException {
        if (TextUtils.isEmpty(jsonContent)) {
            Logger.printInfo(() -> "VOT: Empty subtitle content");
            return null;
        }

        JSONArray subsArray = null;
        try {
            JSONObject root = new JSONObject(jsonContent);
            if (root.has("subtitles")) {
                subsArray = root.getJSONArray("subtitles");
            }
        } catch (JSONException e) {
            if (jsonContent.trim().startsWith("[")) {
                subsArray = new JSONArray(jsonContent);
            }
        }

        if (subsArray == null) {
            Logger.printException(() -> "VOT: Invalid subtitle JSON structure: " + jsonContent.substring(0, Math.min(jsonContent.length(), 100)));
            return null;
        }

        TreeMap<Long, Pair<Long, String>> map = new TreeMap<>();
        for (int i = 0; i < subsArray.length(); i++) {
            try {
                JSONObject subObj = subsArray.getJSONObject(i);
                double startMsDouble = subObj.optDouble("startMs", -1.0);
                double endMsDouble = subObj.optDouble("endMs", -1.0);
                double durationMsDouble = endMsDouble < 0 ? subObj.optDouble("durationMs", -1.0) : -1.0;

                if (startMsDouble < 0) {
                    int finalI = i;
                    Logger.printInfo(() -> "VOT: Skipping subtitle entry #" + finalI + ": Invalid startMs");
                    continue;
                }

                long startMs = Math.round(startMsDouble);
                long endMs = endMsDouble >= 0 ? Math.round(endMsDouble) : startMs + Math.round(durationMsDouble);
                if (endMs <= startMs) {
                    int finalI1 = i;
                    Logger.printInfo(() -> "VOT: Skipping subtitle entry #" + finalI1 + ": Invalid timing (end <= start)");
                    continue;
                }

                String text = subObj.optString("text", "").trim();
                map.put(startMs, new Pair<>(endMs, text));
            } catch (JSONException e) {
                int finalI2 = i;
                Logger.printException(() -> "VOT: Error parsing subtitle entry #" + finalI2, e);
            }
        }

        Logger.printDebug(() -> "VOT: Parsed " + map.size() + " subtitle entries");
        return map.isEmpty() && subsArray.length() > 0 ? null : map;
    }

    /**
     * Builds HTTP headers for Yandex API requests with authentication.
     *
     * @param session      The current session.
     * @param bodyBytes    The request body bytes for signature.
     * @param path         The API endpoint path.
     * @param modulePrefix The header prefix ("Vtrans" or "Vsubs").
     * @return The constructed {@link Headers}.
     * @throws GeneralSecurityException If a cryptographic error occurs.
     */
    private static Headers buildRequestHeaders(SessionInfo session, byte[] bodyBytes, String path, String modulePrefix) throws GeneralSecurityException {
        String bodySignature = calculateSignature(bodyBytes != null ? bodyBytes : new byte[0]);
        String tokenPart = session.uuid + ":" + path + ":" + COMPONENT_VERSION;
        String tokenSignature = calculateSignature(tokenPart.getBytes(StandardCharsets.UTF_8));
        if (TextUtils.isEmpty(tokenSignature)) {
            throw new GeneralSecurityException("Empty token signature");
        }
        String secToken = tokenSignature + ":" + tokenPart;

        return new Headers.Builder()
                .add("User-Agent", USER_AGENT)
                .add("Accept", "application/x-protobuf")
                .add("Content-Type", "application/x-protobuf")
                .add("Pragma", "no-cache")
                .add("Cache-Control", "no-cache")
                .add(modulePrefix + "-Signature", bodySignature)
                .add("Sec-" + modulePrefix + "-Sk", session.secretKey)
                .add("Sec-" + modulePrefix + "-Token", secToken)
                .add("sec-ch-ua", SEC_CH_UA)
                .add("sec-ch-ua-full-version-list", SEC_CH_UA_FULL)
                .add("Sec-Fetch-Mode", "no-cors")
                .build();
    }

    /**
     * Calculates HMAC-SHA256 signature for data.
     *
     * @param data The data to sign.
     * @return The hexadecimal signature.
     * @throws GeneralSecurityException If a cryptographic error occurs.
     */
    private static String calculateSignature(byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(HMAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return bytesToHex(mac.doFinal(data != null ? data : new byte[0]));
    }

    /**
     * Converts bytes to a hexadecimal string.
     *
     * @param bytes The byte array.
     * @return The hex string.
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_ARRAY[v >>> 4];
            hex[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hex);
    }

    /**
     * Converts seconds to a user-friendly time string.
     *
     * @param secs The duration in seconds.
     * @return A localized time string.
     */
    private static String secsToStrTime(int secs) {
        if (secs < 0) return str("revanced_yandex_status_processing_short");
        if (secs < 90) return str("revanced_yandex_status_processing_minute");
        int minutes = Math.round(secs / 60.0f);
        if (minutes <= 0) return str("revanced_yandex_status_processing_minute");
        if (minutes >= 60) return str("revanced_yandex_status_processing_hour");
        return String.format(str("revanced_yandex_status_processing_minutes_plural"), minutes);
    }

    /**
     * Forces release of a workflow lock for a video URL.
     *
     * @param videoUrl The video URL.
     */
    static void forceReleaseWorkflowLock(@Nullable String videoUrl) {
        if (videoUrl == null) return;
        AtomicBoolean lock = urlWorkflowLocks.remove(videoUrl);
        AtomicBoolean cancelFlag = urlCancellationFlags.remove(videoUrl);
        if (cancelFlag != null) cancelFlag.set(true);

        if (lock != null || cancelFlag != null) {
            Logger.printInfo(() -> "VOT: Force-released lock and cleaned up cancellation state for " + videoUrl);
        } else {
            Logger.printDebug(() -> "VOT: No lock or cancellation flag found to cleanup for " + videoUrl);
        }
    }

    /**
     * Posts a runnable to the main thread.
     *
     * @param runnable The runnable to execute.
     */
    private static void postToMainThread(Runnable runnable) {
        mainThreadHandler.post(runnable);
    }

    /**
     * Translates known, hardcoded server error messages into localizable string resources.
     *
     * @param serverMessage The raw error message from the Yandex API.
     * @return A localizable string resource ID if a known error is detected, otherwise the original message.
     */
    private static String translateServerMessage(String serverMessage) {
        if (TextUtils.isEmpty(serverMessage)) return "";

        if (serverMessage.contains(YANDEX_ERROR_SERVER_TRY_AGAIN))
            return str("revanced_yandex_error_server_try_again");

        return serverMessage;
    }

    /**
     * Helper method to strip the "tokens" array from Yandex JSON.
     * This drastically reduces the size of the JSON sent to Gemini,
     * preventing output truncation and saving tokens.
     *
     * @param jsonContent The raw JSON from Yandex.
     * @return The JSON string with 'tokens' arrays removed from each subtitle entry.
     */
    public static String stripTokensFromYandexJson(String jsonContent) {
        if (TextUtils.isEmpty(jsonContent)) return jsonContent;
        try {
            // Handle case where JSON is just an array (non-standard Yandex output)
            if (jsonContent.trim().startsWith("[")) {
                JSONArray root = new JSONArray(jsonContent);
                for (int i = 0; i < root.length(); i++) {
                    JSONObject item = root.getJSONObject(i);
                    if (item.has("tokens")) item.remove("tokens");
                }
                return root.toString();
            } else {
                // Handle standard object with "subtitles" array (standard Yandex output)
                JSONObject root = getJsonObject(jsonContent);
                return root.toString();
            }
        } catch (JSONException e) {
            Logger.printException(() -> "Failed to strip tokens from JSON", e);
            return jsonContent; // Return original if modification fails
        }
    }

    /**
     * Parses the provided JSON string into a JSONObject and removes the verbose "tokens"
     * arrays from within the "subtitles" list. It also updates metadata flags to reflect
     * the removal of tokens.
     *
     * @param jsonContent The raw JSON string containing the subtitle object.
     * @return The modified {@link JSONObject} with tokens removed.
     * @throws JSONException If the string cannot be parsed or the structure is invalid.
     */
    @NotNull
    private static JSONObject getJsonObject(String jsonContent) throws JSONException {
        JSONObject root = new JSONObject(jsonContent);
        if (root.has("subtitles")) {
            JSONArray subs = root.getJSONArray("subtitles");
            for (int i = 0; i < subs.length(); i++) {
                JSONObject item = subs.getJSONObject(i);
                if (item.has("tokens")) item.remove("tokens");
            }
        }
        // Also remove top-level containsTokens flag if present
        if (root.has("containsTokens")) root.put("containsTokens", false);
        return root;
    }

    // endregion Utils

    // region Interfaces and Inner Classes

    /**
     * Callback interface for the Yandex subtitle workflow.
     */
    public interface SubtitleWorkflowCallback {
        void onFinalSuccess(TreeMap<Long, Pair<Long, String>> parsedSubtitles);

        void onIntermediateSuccess(String rawIntermediateJson, String intermediateLang);

        void onFinalFailure(String errorMessage);

        void onProcessingStarted(String statusMessage);
    }

    /**
     * Represents an active Yandex API session.
     */
    private static class SessionInfo {
        final String uuid;
        final String secretKey;
        final long expiresAtMillis;

        SessionInfo(String uuid, String secretKey, int expires) {
            this.uuid = uuid;
            this.secretKey = secretKey;
            long durationMillis = expires > 0 ? TimeUnit.SECONDS.toMillis(expires) : TimeUnit.HOURS.toMillis(1);
            this.expiresAtMillis = System.currentTimeMillis() + durationMillis - TimeUnit.MINUTES.toMillis(1);
            Logger.printDebug(() -> "VOT: Session created, expires in " + (expiresAtMillis - System.currentTimeMillis()) + "ms");
        }

        boolean isValid() {
            return System.currentTimeMillis() < (expiresAtMillis - 30000);
        }
    }

    // endregion Interfaces and Inner Classes

    // region Protobuf Manual Classes

    public static class ManualYandexSessionRequest {
        String uuid;
        String module;

        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(out)) {
                if (!TextUtils.isEmpty(uuid)) ProtoWriter.writeString(data, 1, uuid);
                if (!TextUtils.isEmpty(module)) ProtoWriter.writeString(data, 2, module);
            }
            return out.toByteArray();
        }
    }

    public static class ManualYandexSessionResponse {
        String secretKey;
        int expires;

        static ManualYandexSessionResponse parseFrom(byte[] data) throws IOException {
            ManualYandexSessionResponse response = new ManualYandexSessionResponse();
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
                while (input.available() > 0) {
                    int tag = ProtoReader.readVarint32(input);
                    int field = tag >>> 3;
                    int wireType = tag & 7;
                    switch (field) {
                        case 1:
                            if (wireType != 2) throw new IOException("Invalid wire type for secretKey: " + wireType);
                            response.secretKey = ProtoReader.readString(input);
                            break;
                        case 2:
                            if (wireType != 0) throw new IOException("Invalid wire type for expires: " + wireType);
                            response.expires = ProtoReader.readVarint32(input);
                            break;
                        default:
                            ProtoReader.skipField(input, tag);
                            break;
                    }
                }
            }
            return response;
        }
    }

    public static class ManualSubtitlesRequest {
        String url;
        String language;

        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(out)) {
                if (!TextUtils.isEmpty(url)) ProtoWriter.writeString(data, 1, url);
                if (!TextUtils.isEmpty(language)) ProtoWriter.writeString(data, 2, language);
            }
            return out.toByteArray();
        }
    }

    public static class ManualSubtitlesObject {
        String language;
        String url;
        String translatedLanguage;
        String translatedUrl;

        static ManualSubtitlesObject parseFrom(DataInputStream input) throws IOException {
            ManualSubtitlesObject obj = new ManualSubtitlesObject();
            int length = ProtoReader.readVarint32(input);
            if (length < 0 || length > 1024 * 1024) {
                Logger.printException(() -> "VOT: Invalid subtitle object length: " + length);
                input.skipBytes(input.available());
                return obj;
            }
            if (length == 0) return obj;

            byte[] message = new byte[length];
            input.readFully(message);
            try (DataInputStream nested = new DataInputStream(new ByteArrayInputStream(message))) {
                while (nested.available() > 0) {
                    int tag = ProtoReader.readVarint32(nested);
                    int field = tag >>> 3;
                    int wireType = tag & 7;
                    switch (field) {
                        case 1:
                            if (wireType != 2) throw new IOException("Invalid wire type for language: " + wireType);
                            obj.language = ProtoReader.readString(nested);
                            break;
                        case 2:
                            if (wireType != 2) throw new IOException("Invalid wire type for url: " + wireType);
                            obj.url = ProtoReader.readString(nested);
                            break;
                        case 4:
                            if (wireType != 2) throw new IOException("Invalid wire type for translatedLanguage: " + wireType);
                            obj.translatedLanguage = ProtoReader.readString(nested);
                            break;
                        case 5:
                            if (wireType != 2) throw new IOException("Invalid wire type for translatedUrl: " + wireType);
                            obj.translatedUrl = ProtoReader.readString(nested);
                            break;
                        default:
                            ProtoReader.skipField(nested, tag);
                            break;
                    }
                }
            }
            return obj;
        }
    }

    public static class ManualSubtitlesResponse {
        boolean waiting;
        List<ManualSubtitlesObject> subtitles = new ArrayList<>();

        static ManualSubtitlesResponse parseFrom(byte[] data) throws IOException {
            ManualSubtitlesResponse response = new ManualSubtitlesResponse();
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
                while (input.available() > 0) {
                    int tag = ProtoReader.readVarint32(input);
                    int field = tag >>> 3;
                    int wireType = tag & 7;
                    switch (field) {
                        case 1:
                            if (wireType != 0) throw new IOException("Invalid wire type for waiting: " + wireType);
                            response.waiting = ProtoReader.readBool(input);
                            break;
                        case 2:
                            if (wireType != 2) throw new IOException("Invalid wire type for subtitles: " + wireType);
                            response.subtitles.add(ManualSubtitlesObject.parseFrom(input));
                            break;
                        default:
                            ProtoReader.skipField(input, tag);
                            break;
                    }
                }
            }
            return response;
        }
    }

    public static class ManualVideoTranslationRequest {
        String url;                     // 3
        boolean firstRequest = true;    // 5
        double duration;                // 6
        int unknown7 = 1;               // 7
        String language;                // 8
        String responseLanguage;        // 14
        int unknown15 = 1;              // 15
        int unknown16 = 2;              // 16
        String videoTitle;              // 19

        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(out)) {
                if (!TextUtils.isEmpty(url)) ProtoWriter.writeString(data, 3, url);
                ProtoWriter.writeBool(data, 5, firstRequest);
                if (duration > 0) ProtoWriter.writeDouble(data, 6, duration);
                ProtoWriter.writeInt32(data, 7, unknown7);
                if (!TextUtils.isEmpty(language)) ProtoWriter.writeString(data, 8, language);
                if (!TextUtils.isEmpty(responseLanguage)) ProtoWriter.writeString(data, 14, responseLanguage);
                ProtoWriter.writeInt32(data, 15, unknown15);
                ProtoWriter.writeInt32(data, 16, unknown16);
                if (!TextUtils.isEmpty(videoTitle)) ProtoWriter.writeString(data, 19, videoTitle);
            }
            return out.toByteArray();
        }
    }

    public static class ManualVideoTranslationResponse {
        String url;
        double duration;
        int status;
        int remainingTime;
        String translationId;
        String language;
        String message;
        int unknown0;
        boolean isLivelyVoice;
        int unknown2;

        static ManualVideoTranslationResponse parseFrom(byte[] data) throws IOException {
            ManualVideoTranslationResponse response = new ManualVideoTranslationResponse();
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
                while (input.available() > 0) {
                    int tag = ProtoReader.readVarint32(input);
                    int field = tag >>> 3;
                    int wireType = tag & 7;
                    switch (field) {
                        case 1:
                            if (wireType != 2) throw new IOException("Invalid wire type for url: " + wireType);
                            response.url = ProtoReader.readString(input);
                            break;
                        case 2:
                            if (wireType != 1) throw new IOException("Invalid wire type for duration: " + wireType);
                            response.duration = ProtoReader.readDouble(input);
                            break;
                        case 4:
                            if (wireType != 0) throw new IOException("Invalid wire type for status: " + wireType);
                            response.status = ProtoReader.readVarint32(input);
                            break;
                        case 5:
                            if (wireType != 0) throw new IOException("Invalid wire type for remainingTime: " + wireType);
                            response.remainingTime = ProtoReader.readVarint32(input);
                            break;
                        case 6:
                            if (wireType != 0) throw new IOException("Invalid wire type for unknown0: " + wireType);
                            response.unknown0 = ProtoReader.readVarint32(input);
                            break;
                        case 7:
                            if (wireType != 2) throw new IOException("Invalid wire type for translationId: " + wireType);
                            response.translationId = ProtoReader.readString(input);
                            break;
                        case 8:
                            if (wireType != 2) throw new IOException("Invalid wire type for language: " + wireType);
                            response.language = ProtoReader.readString(input);
                            break;
                        case 9:
                            if (wireType != 2) throw new IOException("Invalid wire type for message: " + wireType);
                            response.message = ProtoReader.readString(input);
                            break;
                        case 10:
                            if (wireType != 0) throw new IOException("Invalid wire type for isLivelyVoice: " + wireType);
                            response.isLivelyVoice = ProtoReader.readBool(input);
                            break;
                        case 11:
                            if (wireType != 0) throw new IOException("Invalid wire type for unknown2: " + wireType);
                            response.unknown2 = ProtoReader.readVarint32(input);
                            break;
                        default:
                            ProtoReader.skipField(input, tag);
                            break;
                    }
                }
            }
            return response;
        }
    }

    public static class ManualAudioBufferObject {
        String fileId;
        byte[] audioFile;

        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(out)) {
                if (!TextUtils.isEmpty(fileId)) ProtoWriter.writeString(data, 1, fileId);
                if (audioFile != null && audioFile.length > 0) ProtoWriter.writeBytes(data, 2, audioFile);
            }
            return out.toByteArray();
        }
    }

    public static class ManualChunkAudioObject {
        int audioPartsLength;
        ManualAudioBufferObject audioBuffer;
        String fileId;
        int unknown0;

        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(out)) {
                if (audioBuffer != null) {
                    byte[] msgBytes = audioBuffer.toByteArray();
                    ProtoWriter.writeTag(data, 1, 2);
                    ProtoWriter.writeVarint32(data, msgBytes.length);
                    data.write(msgBytes);
                }
                if (audioPartsLength != 0) ProtoWriter.writeInt32(data, 2, audioPartsLength);
                if (!TextUtils.isEmpty(fileId)) ProtoWriter.writeString(data, 3, fileId);
                if (unknown0 != 0) ProtoWriter.writeInt32(data, 4, unknown0);
            }
            return out.toByteArray();
        }
    }

    public static class ManualVideoTranslationAudioRequest {
        String translationId;
        String url;
        ManualChunkAudioObject partialAudioInfo;
        ManualAudioBufferObject audioInfo;

        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DataOutputStream data = new DataOutputStream(out)) {
                if (!TextUtils.isEmpty(translationId)) ProtoWriter.writeString(data, 1, translationId);
                if (!TextUtils.isEmpty(url)) ProtoWriter.writeString(data, 2, url);
                if (partialAudioInfo != null) {
                    byte[] msgBytes = partialAudioInfo.toByteArray();
                    ProtoWriter.writeTag(data, 4, 2);
                    ProtoWriter.writeVarint32(data, msgBytes.length);
                    data.write(msgBytes);
                }
                if (audioInfo != null) {
                    byte[] msgBytes = audioInfo.toByteArray();
                    ProtoWriter.writeTag(data, 6, 2);
                    ProtoWriter.writeVarint32(data, msgBytes.length);
                    data.write(msgBytes);
                }
            }
            return out.toByteArray();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static class ProtoWriter {
        static void writeTag(DataOutputStream data, int field, int wireType) throws IOException {
            ProtoWriter.writeVarint32(data, (field << 3) | wireType);
        }

        static void writeVarint32(DataOutputStream data, int value) throws IOException {
            while (true) {
                if ((value & ~0x7F) == 0) {
                    data.writeByte(value);
                    return;
                }
                data.writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }

        static void writeBytes(DataOutputStream data, int field, byte[] value) throws IOException {
            writeTag(data, field, 2);
            writeVarint32(data, value.length);
            data.write(value);
        }

        static void writeString(DataOutputStream data, int field, String value) throws IOException {
            writeBytes(data, field, value.getBytes(StandardCharsets.UTF_8));
        }

        static void writeDouble(DataOutputStream data, int field, double value) throws IOException {
            writeTag(data, field, 1);
            data.writeLong(Long.reverseBytes(Double.doubleToRawLongBits(value)));
        }

        static void writeInt32(DataOutputStream data, int field, int value) throws IOException {
            writeTag(data, field, 0);
            writeVarint32(data, value);
        }

        static void writeBool(DataOutputStream data, int field, boolean value) throws IOException {
            writeTag(data, field, 0);
            data.writeByte(value ? 1 : 0);
        }
    }

    private static class ProtoReader {
        private static final int MAX_VARINT_BYTES = 10;
        private static final int MAX_LENGTH_LIMIT = 50 * 1024 * 1024;

        static int readVarint32(DataInputStream input) throws IOException {
            int result = 0;
            int shift = 0;
            for (int i = 0; i < MAX_VARINT_BYTES; i++) {
                byte b = input.readByte();
                result |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) return result;
                shift += 7;
                if (shift >= 32 && (b & 0x80) != 0) {
                    while (i + 1 < MAX_VARINT_BYTES && (input.readByte() & 0x80) != 0) {
                        i++;
                    }
                    throw new IOException("Varint32 too large");
                }
            }
            throw new IOException("Varint too long");
        }

        static byte[] readBytes(DataInputStream input) throws IOException {
            int length = readVarint32(input);
            if (length < 0) throw new IOException("Negative length: " + length);
            if (length > MAX_LENGTH_LIMIT) throw new IOException("Length too large: " + length);
            if (length > input.available()) throw new IOException("Length exceeds available: " + length);
            byte[] data = new byte[length];
            input.readFully(data);
            return data;
        }

        static String readString(DataInputStream input) throws IOException {
            return new String(readBytes(input), StandardCharsets.UTF_8);
        }

        static double readDouble(DataInputStream input) throws IOException {
            return Double.longBitsToDouble(Long.reverseBytes(input.readLong()));
        }

        static boolean readBool(DataInputStream input) throws IOException {
            return readVarint32(input) != 0;
        }

        static void skipField(DataInputStream input, int tag) throws IOException {
            int wireType = tag & 7;
            switch (wireType) {
                case 0:
                    readVarint32(input);
                    break;
                case 1:
                    if (input.available() < 8) throw new EOFException("Insufficient data for 64-bit");
                    input.skipBytes(8);
                    break;
                case 2:
                    int length = readVarint32(input);
                    if (length < 0 || length > MAX_LENGTH_LIMIT || length > input.available()) {
                        throw new IOException("Invalid length for wire type 2: " + length);
                    }
                    input.skipBytes(length);
                    break;
                case 5:
                    if (input.available() < 4) throw new EOFException("Insufficient data for 32-bit");
                    input.skipBytes(4);
                    break;
                case 3:
                    int fieldNo = tag >>> 3;
                    int depth = 1;
                    while (depth > 0) {
                        if (input.available() <= 0) throw new EOFException("Unexpected end in group");
                        int nextTag = readVarint32(input);
                        int nextWireType = nextTag & 7;
                        int nextFieldNo = nextTag >>> 3;
                        if (nextWireType == 4) {
                            if (nextFieldNo != fieldNo) throw new IOException("Mismatched EndGroup: " + nextFieldNo);
                            depth--;
                        } else if (nextWireType == 3) {
                            depth++;
                        } else {
                            skipField(input, nextTag);
                        }
                    }
                    break;
                default:
                    throw new IOException("Unknown wire type: " + wireType);
            }
        }
    }

    // endregion Protobuf Manual Classes
}
