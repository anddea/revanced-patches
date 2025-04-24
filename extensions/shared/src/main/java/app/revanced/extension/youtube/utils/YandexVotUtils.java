package app.revanced.extension.youtube.utils;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.revanced.extension.shared.utils.Logger;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static app.revanced.extension.shared.utils.StringRef.str;

public class YandexVotUtils {
    // --- Constants ---
    private static final String YANDEX_HOST = "api.browser.yandex.ru";
    private static final String BASE_URL = "https://" + YANDEX_HOST;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 YaBrowser/25.2.0.0 Safari/537.36";
    private static final String HMAC_KEY = "bt8xH3VOlb4mqf0nqAibnDOoiPlXsisf";
    private static final String COMPONENT_VERSION = "25.2.3.808";
    private static final String SESSION_MODULE = "video-translation";

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

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    // --- State & Utilities ---
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();
    private static final ReentrantLock sessionLock = new ReentrantLock();
    private static final Map<String, AtomicBoolean> urlWorkflowLocks = new ConcurrentHashMap<>();
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private static volatile SessionInfo currentSession = null;

    // region Session Management

    /**
     * Ensures a valid Yandex API session exists, creating a new one if necessary.
     * This method is thread-safe.
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
                Logger.printDebug(() -> "VOT: Using existing valid session (checked after lock).");
                return session;
            }
            Logger.printInfo(() -> "VOT: Creating new Yandex session...");
            currentSession = createNewSessionInternal();
            Logger.printInfo(() -> "VOT: New Yandex session created successfully.");
            return currentSession;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Performs the actual network request to create a new Yandex API session.
     * <p>
     * HTTP Method: POST
     * <br>
     * Endpoint: /session/create (PATH_SESSION_CREATE)
     * <br>
     * Purpose: Creates a new authenticated session with the Yandex API.
     *
     * @return The newly created {@link SessionInfo}.
     * @throws IOException              If a network error occurs or the response is invalid.
     * @throws GeneralSecurityException If a security error occurs during signature calculation.
     */
    private static SessionInfo createNewSessionInternal() throws IOException, GeneralSecurityException {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        Logger.printDebug(() -> "VOT: Calling " + PATH_SESSION_CREATE + " with module: " + YandexVotUtils.SESSION_MODULE);

        ManualYandexSessionRequest requestProto = new ManualYandexSessionRequest();
        requestProto.uuid = uuid;
        requestProto.module = YandexVotUtils.SESSION_MODULE;

        byte[] requestBodyBytes = requestProto.toByteArray();

        String signature = calculateSignature(requestBodyBytes);
        Headers headers = new Headers.Builder()
                .add("User-Agent", USER_AGENT)
                .add("Accept", "application/x-protobuf")
                .add("Content-Type", "application/x-protobuf")
                .add("Vtrans-Signature", signature)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + PATH_SESSION_CREATE)
                .headers(headers)
                .post(RequestBody.create(requestBodyBytes, MediaType.parse("application/x-protobuf")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String bodyString = "<failed>";
                try {
                    if (response.body() != null) bodyString = response.body().string();
                } catch (Exception ignored) {
                }
                final String finalBodyString = bodyString;
                Logger.printException(() -> "VOT: Failed to create session: " + response.code() + " " + response.message() + "\nBody: " + finalBodyString);
                throw new IOException("Failed to create session: " + response.code());
            }
            byte[] responseBytes = response.body().bytes();
            try {
                ManualYandexSessionResponse sessionResponse = ManualYandexSessionResponse.parseFrom(responseBytes);
                if (TextUtils.isEmpty(sessionResponse.secretKey)) {
                    Logger.printException(() -> "VOT: Session response invalid (missing key)");
                    throw new IOException("Invalid session response (missing key)");
                }
                return new SessionInfo(uuid, sessionResponse.secretKey, sessionResponse.expires);
            } catch (IOException e) {
                Logger.printException(() -> "VOT: Failed to parse session response", e);
                throw e;
            }
        } catch (IOException e) {
            Logger.printException(() -> "VOT: Network error during session creation", e);
            throw e;
        }
    }

    // endregion Session Management

    // region API Calls

    /**
     * Get Subtitle Tracks (Called AFTER translation is confirmed done OR for initial check).
     * Used to check for existing subtitles or fetch final translated subtitles.
     * <p>
     * HTTP Method: POST
     * <br>
     * Endpoint: /video-subtitles/get-subtitles (PATH_GET_SUBTITLES)
     * <br>
     * Purpose: Fetches the list of available original and translated subtitle tracks for a video, usually after the translation process is confirmed complete or to check for pre-existing subtitles.
     *
     * @param videoUrl The URL of the video.
     * @param session  The current valid {@link SessionInfo}.
     * @return A {@link ManualSubtitlesResponse} containing the list of available subtitles, or null if the request failed.
     * @throws IOException              If a network error occurs or the response cannot be parsed.
     * @throws GeneralSecurityException If a security error occurs during header generation.
     */
    @Nullable
    private static ManualSubtitlesResponse getFinalSubtitleTracks(String videoUrl, SessionInfo session) throws IOException, GeneralSecurityException {
        String requestLang = "auto"; // Request all available subtitles regardless of original language
        Logger.printInfo(() -> "VOT: Calling " + PATH_GET_SUBTITLES + " URL: " + videoUrl);
        ManualSubtitlesRequest requestProto = new ManualSubtitlesRequest();
        requestProto.url = videoUrl;
        requestProto.language = requestLang;
        byte[] requestBodyBytes = requestProto.toByteArray();
        Headers headers = buildRequestHeaders(session, requestBodyBytes, PATH_GET_SUBTITLES, VSUBS_PREFIX);
        Request request = new Request.Builder()
                .url(BASE_URL + PATH_GET_SUBTITLES)
                .headers(headers)
                .post(RequestBody.create(requestBodyBytes, MediaType.parse("application/x-protobuf")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String bodyString = "<failed>";
                try {
                    if (response.body() != null) bodyString = response.body().string();
                } catch (Exception ignored) {}
                final String finalBodyString = bodyString;
                Logger.printException(() -> "VOT: Failed to get subtitle tracks: " + response.code() + " " + response.message() + "\nBody: " + finalBodyString);
                return null;
            }
            byte[] responseBytes = response.body().bytes();
            try {
                ManualSubtitlesResponse subResponse = ManualSubtitlesResponse.parseFrom(responseBytes);
                String availableSubsLog;
                if (subResponse.subtitles != null && !subResponse.subtitles.isEmpty()) {
                    availableSubsLog = subResponse.subtitles.stream()
                            .map(s -> (s.translatedLanguage != null ? s.language + "->" + s.translatedLanguage : s.language)
                                    + (TextUtils.isEmpty(s.url) && TextUtils.isEmpty(s.translatedUrl) ? "(X)" : "")) // Indicate if URL is missing
                            .collect(Collectors.joining(", "));
                } else {
                    availableSubsLog = "None";
                }
                Logger.printInfo(() -> "VOT: Subtitle tracks response. Waiting: " + subResponse.waiting + ", Subtitles: [" + availableSubsLog + "]");
                return subResponse;
            } catch (IOException e) {
                Logger.printException(() -> "VOT: Failed to parse subtitle tracks response", e);
                throw e;
            }
        } catch (IOException e) {
            Logger.printException(() -> "VOT: Network/Security error getting subtitle tracks", e);
            throw e;
        }
    }

    /**
     * Initiates the asynchronous workflow to get Yandex translated subtitles for a given video URL.
     * This method handles session management, checking for existing subtitles, requesting translation if needed,
     * polling for status, and finally fetching and parsing the subtitles OR returning raw intermediate JSON.
     * Ensures only one workflow runs per URL at a time.
     *
     * @param videoUrl           The URL of the YouTube video.
     * @param videoTitle         The title of the video (optional, used in translation request).
     * @param durationSeconds    The duration of the video in seconds (used in translation request).
     * @param originalTargetLang The user's desired final target language code (e.g., "en", "es", "de").
     * @param callback           The {@link SubtitleWorkflowCallback} to report progress, success (intermediate or final), or failure.
     */
    public static void getYandexSubtitlesWorkflowAsync(
            final String videoUrl,
            @Nullable final String videoTitle,
            final double durationSeconds,
            final String originalTargetLang,
            final SubtitleWorkflowCallback callback
    ) {
        Logger.printInfo(() -> "VOT: Starting Async Workflow for URL: " + videoUrl + ", OriginalTargetLang: " + originalTargetLang);

        final String yandexTargetLang;

        if ("en".equals(originalTargetLang) || "ru".equals(originalTargetLang) || "kk".equals(originalTargetLang)) {
            yandexTargetLang = originalTargetLang;
            Logger.printDebug(() -> "VOT: Yandex supports target language directly: " + yandexTargetLang);
        } else {
            yandexTargetLang = "en";
            Logger.printInfo(() -> "VOT: Yandex does not support " + originalTargetLang + ". Requesting intermediate '" + yandexTargetLang + "' for secondary translation.");
        }

        AtomicBoolean lock = urlWorkflowLocks.computeIfAbsent(videoUrl, k -> new AtomicBoolean(false));
        if (!lock.compareAndSet(false, true)) {
            Logger.printInfo(() -> "VOT: Workflow for " + videoUrl + " already running.");
            return;
        }

        final SubtitleWorkflowCallback finalCallback = new SubtitleWorkflowCallback() {
            private final AtomicBoolean finalCalled = new AtomicBoolean(false);

            @Override
            public void onFinalSuccess(TreeMap<Long, Pair<Long, String>> parsedSubtitles) {
                if (finalCalled.compareAndSet(false, true)) {
                    Logger.printDebug(() -> "VOT: Workflow final success for " + videoUrl + ". Releasing lock via finalCallback.");
                    try {
                        callback.onFinalSuccess(parsedSubtitles);
                    } finally {
                        urlWorkflowLocks.remove(videoUrl); // Release lock
                    }
                }
            }

            @Override
            public void onIntermediateSuccess(String rawIntermediateJson, String intermediateLang) {
                Logger.printDebug(() -> "VOT: Workflow intermediate success for " + videoUrl + " (Lang: " + intermediateLang + "). Caller handles next step.");
                callback.onIntermediateSuccess(rawIntermediateJson, intermediateLang);
                // DO NOT release lock here
            }

            @Override
            public void onFinalFailure(String errorMessage) {
                if (finalCalled.compareAndSet(false, true)) {
                    Logger.printDebug(() -> "VOT: Workflow final failure for " + videoUrl + ". Releasing lock via finalCallback.");
                    try {
                        callback.onFinalFailure(errorMessage);
                    } finally {
                        urlWorkflowLocks.remove(videoUrl); // Release lock
                    }
                }
            }

            @Override
            public void onProcessingStarted(String statusMessage) {
                if (!finalCalled.get()) {
                    callback.onProcessingStarted(statusMessage);
                }
            }
        };

        new Thread(() -> {
            try {
                SessionInfo session;
                long workflowStartTime = System.currentTimeMillis();

                try {
                    Logger.printInfo(() -> "VOT: Step 1/5 - Ensuring Session...");
                    session = ensureSession();
                    postToMainThread(() -> finalCallback.onProcessingStarted(str("revanced_yandex_status_session_ok")));
                    Logger.printDebug(() -> "VOT: Step 1/5 - Session OK.");

                    Logger.printInfo(() -> "VOT: Step 2/5 - Checking for existing subtitles (for Yandex target: " + yandexTargetLang + ")...");
                    ManualSubtitlesResponse initialSubsResponse = null;
                    try {
                        initialSubsResponse = getFinalSubtitleTracks(videoUrl, session);
                    } catch (Exception e) {
                        Logger.printException(() -> "VOT: Initial subtitle check failed, proceeding to request translation.", e);
                    }

                    if (initialSubsResponse != null && initialSubsResponse.subtitles != null && !initialSubsResponse.waiting) {
                        ManualSubtitlesObject chosenSub = findBestSubtitleForLanguage(initialSubsResponse.subtitles, yandexTargetLang);
                        String subtitleUrl = (chosenSub != null) ? determineSubtitleUrl(chosenSub, yandexTargetLang) : null;

                        if (chosenSub != null && !TextUtils.isEmpty(subtitleUrl)) {
                            Logger.printInfo(() -> "VOT: Step 2/5 - Found existing suitable subtitle track for Yandex target " + yandexTargetLang + ". Skipping translation request.");
                            postToMainThread(() -> finalCallback.onProcessingStarted(str("revanced_yandex_status_subs_found")));
                            processAndFetchFinalSubtitles(initialSubsResponse, originalTargetLang, yandexTargetLang, finalCallback);
                            // Note: execution continues in processAndFetchFinalSubtitles's async callback
                            return; // Exit this thread's main path after starting async fetch
                        }
                    }

                    Logger.printInfo(() -> "VOT: Step 3/5 - No suitable existing subtitles found for Yandex target " + yandexTargetLang + ". Requesting translation...");
                    postToMainThread(() -> finalCallback.onProcessingStarted(str("revanced_yandex_status_requesting_translation")));
                    pollTranslationStatusAsync(videoUrl, originalTargetLang, yandexTargetLang, session, true, videoTitle, durationSeconds, workflowStartTime, finalCallback, 1);
                    // Note: execution continues in pollTranslationStatusAsync's async callback

                } catch (Exception e) {
                    Logger.printException(() -> "VOT: Workflow FAILED during initial setup phase!", e);
                    String userMessage = (e instanceof IOException || e instanceof GeneralSecurityException)
                            ? (e.getMessage() != null ? e.getMessage() : str("revanced_yandex_error_network_generic"))
                            : str("revanced_yandex_error_unknown") + ": " + e.getMessage();
                    // Report failure through the wrapper, which will release the lock
                    postToMainThread(() -> finalCallback.onFinalFailure(userMessage));
                }
                // The background thread often finishes here, letting async callbacks complete the work.
                // The finally block below ensures cleanup even if an unexpected error occurs later
                // or if the thread is terminated/interrupted before finalCallback is called.
            } finally {
                // Ensure lock is released if thread exits unexpectedly ---
                // This acts as a safety net. If the finalCallback's success/failure methods
                // are called, they already remove the lock. This finally block handles cases
                // where the thread might terminate *without* calling those (e.g., uncaught RuntimeException
                // in async tasks, or external interruption if not handled properly).
                AtomicBoolean lockState = urlWorkflowLocks.get(videoUrl);
                if (lockState != null && lockState.get()) {
                    Logger.printInfo(() -> "VOT: Releasing workflow lock for " + videoUrl + " in finally block (thread terminated unexpectedly?).");
                    urlWorkflowLocks.remove(videoUrl);
                } else {
                    Logger.printDebug(() -> "VOT: Workflow lock for " + videoUrl + " already released or not present in finally block.");
                }
            }
        }).start();
    }

    /**
     * Performs an asynchronous request to the Yandex translate endpoint, either initiating the translation
     * or polling for its status. Handles the response and schedules the next poll if necessary,
     * or proceeds to fetch subtitles on success. Handles session expiry and refreshes if needed.
     * <p>
     * HTTP Method: POST
     * <br>
     * Endpoint: /video-translation/translate (PATH_TRANSLATE)
     * <br>
     * Purpose: Initiates the translation process (if firstRequest is true) or polls for the current status of an ongoing translation (if firstRequest is false).
     *
     * @param videoUrl           The URL of the video.
     * @param originalTargetLang The user's final desired language.
     * @param yandexTargetLang   The target language for the Yandex API request ('en', 'ru', 'kk').
     * @param session            The current session information (might be refreshed internally).
     * @param isFirstRequest     True if this is the initial request to start translation, false if polling.
     * @param videoTitle         The title of the video (optional).
     * @param durationSeconds    The duration of the video.
     * @param workflowStartTime  The timestamp when the overall workflow started (for timeout).
     * @param callback           The callback for reporting progress and final results.
     * @param pollCount          The current poll attempt number.
     */
    private static void pollTranslationStatusAsync(
            final String videoUrl,
            final String originalTargetLang,
            final String yandexTargetLang,
            final SessionInfo session,
            final boolean isFirstRequest,
            @Nullable final String videoTitle,
            final double durationSeconds,
            final long workflowStartTime,
            final SubtitleWorkflowCallback callback,
            final int pollCount
    ) {
        if (System.currentTimeMillis() - workflowStartTime >= WORKFLOW_TIMEOUT_MS) {
            Logger.printException(() -> "VOT: Workflow timeout exceeded before polling attempt #" + pollCount);
            long timeoutSec = WORKFLOW_TIMEOUT_MS / 1000;
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_timeout_poll", timeoutSec, pollCount)));
            return;
        }

        if (isFirstRequest) {
            Logger.printInfo(() -> "VOT: Calling " + PATH_TRANSLATE + " (Async Initial Request) URL: " + videoUrl + ", Yandex Target: " + yandexTargetLang);
        } else {
            Logger.printDebug(() -> "VOT: Calling " + PATH_TRANSLATE + " (Async Polling Status #" + pollCount + ") URL: " + videoUrl + ", Yandex Target: " + yandexTargetLang);
        }

        ManualVideoTranslationRequest requestProto = new ManualVideoTranslationRequest();
        requestProto.url = videoUrl;
        requestProto.firstRequest = isFirstRequest;
        requestProto.duration = (durationSeconds > 0) ? durationSeconds : DEFAULT_VIDEO_DURATION_SECONDS;
        requestProto.language = "auto"; // Source language
        requestProto.responseLanguage = yandexTargetLang; // Target for Yandex API
        requestProto.unknown0 = 1;
        requestProto.unknown1 = 0;
        requestProto.unknown2 = 0;
        requestProto.unknown3 = 1;
        requestProto.bypassCache = false;
        requestProto.useNewModel = true;
        requestProto.videoTitle = videoTitle != null ? videoTitle : "";

        byte[] requestBodyBytes;
        Headers headers;
        SessionInfo currentValidSession = session; // Use local variable for thread safety

        try {
            if (!currentValidSession.isValid()) {
                Logger.printInfo(() -> "VOT: Session potentially expired before poll #" + pollCount + ". Attempting refresh.");
                try {
                    currentValidSession = ensureSession();
                    if (!currentValidSession.isValid()) {
                        Logger.printException(() -> "VOT: Session refresh failed or still invalid before poll #" + pollCount);
                        postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_session_expired")));
                        return;
                    }
                    Logger.printInfo(() -> "VOT: Session refreshed successfully before poll #" + pollCount);
                } catch (Exception e) {
                    Logger.printException(() -> "VOT: Error refreshing session before poll #" + pollCount, e);
                    postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_session_refresh_failed")));
                    return;
                }
            }
            requestBodyBytes = requestProto.toByteArray();
            headers = buildRequestHeaders(currentValidSession, requestBodyBytes, PATH_TRANSLATE, VTRANS_PREFIX);
        } catch (IOException | GeneralSecurityException e) {
            Logger.printException(() -> "VOT: Failed to build request for poll #" + pollCount, e);
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_internal_request_build") + ": " + e.getMessage()));
            return;
        }

        final SessionInfo sessionForCallback = currentValidSession;

        Request request = new Request.Builder()
                .url(BASE_URL + PATH_TRANSLATE)
                .headers(headers)
                .post(RequestBody.create(requestBodyBytes, MediaType.parse("application/x-protobuf")))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.printException(() -> "VOT: Network error during poll #" + pollCount, e);
                postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_network_poll") + ": " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {

                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        String bodyString = "<failed>";
                        try {if (response.body() != null) bodyString = response.body().string();} catch (Exception ignored) {}
                        final String finalBodyString = bodyString;
                        Logger.printException(() -> "VOT: Failed translation poll #" + pollCount + " API response: " + response.code() + " " + response.message() + "\nBody: " + finalBodyString);
                        postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_api_poll", response.code())));
                        return;
                    }

                    byte[] responseBytes = responseBody.bytes();
                    ManualVideoTranslationResponse transResponse;
                    try {
                        transResponse = ManualVideoTranslationResponse.parseFrom(responseBytes);
                        Logger.printInfo(() -> "VOT: Poll #" + pollCount + " response Status: " + transResponse.status + ", RemainingTime: " + transResponse.remainingTime + ", Message: " + transResponse.message);
                    } catch (IOException e) {
                        Logger.printException(() -> "VOT: Failed to parse translation status response on poll #" + pollCount, e);
                        postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_parsing_poll")));
                        return;
                    }

                    switch (transResponse.status) {
                        case STATUS_SUCCESS:
                        case STATUS_PART_CONTENT:
                            Logger.printInfo(() -> "VOT: Step 4/5 - SUCCESS! Yandex translation finished after " + pollCount + " polls (for lang " + yandexTargetLang + ").");
                            getFinalSubtitleTracksAsync(videoUrl, sessionForCallback, originalTargetLang, yandexTargetLang, workflowStartTime, callback);
                            break;

                        case STATUS_AUDIO_REQUESTED:
                            Logger.printInfo(() -> "VOT: Received STATUS_AUDIO_REQUESTED (6) for YouTube URL. Sending specific PUT requests.");
                            postToMainThread(() -> callback.onProcessingStarted(str("revanced_yandex_status_youtube_specific")));

                            final String translationId = transResponse.translationId;
                            if (TextUtils.isEmpty(translationId)) {
                                Logger.printException(() -> "VOT: Cannot handle STATUS_AUDIO_REQUESTED, translationId is missing.");
                                postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_internal_missing_translation_id")));
                                return;
                            }

                            // Send PUTs asynchronously
                            sendFailAudioJsRequestAsync(videoUrl);
                            sendAudioRequestAsync(videoUrl, translationId, sessionForCallback);

                            // Continue polling
                            scheduleNextPoll(videoUrl, originalTargetLang, yandexTargetLang, sessionForCallback, videoTitle, durationSeconds,
                                    workflowStartTime, callback, pollCount, transResponse.remainingTime);
                            break;

                        case STATUS_PROCESSING:
                        case STATUS_LONG_PROCESSING:
                            scheduleNextPoll(videoUrl, originalTargetLang, yandexTargetLang, sessionForCallback, videoTitle, durationSeconds,
                                    workflowStartTime, callback, pollCount, transResponse.remainingTime);
                            break;

                        case STATUS_FAILED:
                        default:
                            String errMsg = str("revanced_yandex_error_translation_failed");
                            if (!TextUtils.isEmpty(transResponse.message)) {
                                errMsg += ": " + transResponse.message;
                            }
                            final String finalErrMsg = errMsg;
                            Logger.printException(() -> "VOT: Translation Failed (Status " + transResponse.status + " on poll #" + pollCount + "): " + finalErrMsg);
                            postToMainThread(() -> callback.onFinalFailure(finalErrMsg));
                            break;
                    }

                } catch (IOException e) {
                    Logger.printException(() -> "VOT: IOException reading response body on poll #" + pollCount, e);
                    postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_reading_response")));
                }
            }
        });
    }

    /**
     * Schedules the next poll request after a calculated delay based on the API response.
     * Posts the task to the main thread handler.
     *
     * @param videoUrl           The URL of the video.
     * @param originalTargetLang The user's final desired language.
     * @param yandexTargetLang   The target language for the Yandex API request.
     * @param session            The session info to use for the next poll.
     * @param videoTitle         The video title (optional).
     * @param durationSeconds    The video duration.
     * @param workflowStartTime  The start time of the workflow for timeout checking.
     * @param callback           The callback for reporting progress.
     * @param currentPollCount   The count of the poll just completed.
     * @param remainingTimeSecs  The estimated remaining time from the API response (in seconds).
     */
    private static void scheduleNextPoll(
            final String videoUrl, final String originalTargetLang, final String yandexTargetLang, final SessionInfo session,
            @Nullable final String videoTitle, final double durationSeconds,
            final long workflowStartTime, final SubtitleWorkflowCallback callback,
            final int currentPollCount, int remainingTimeSecs
    ) {
        if (System.currentTimeMillis() - workflowStartTime >= WORKFLOW_TIMEOUT_MS) {
            Logger.printException(() -> "VOT: Workflow timeout exceeded before scheduling poll #" + (currentPollCount + 1));
            long timeoutSec = WORKFLOW_TIMEOUT_MS / 1000;
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_timeout_processing", timeoutSec)));
            return;
        }

        long remainingMs = (long) remainingTimeSecs * 1000;
        long sleepTimeMs = Math.max(MIN_POLLING_INTERVAL_MS,
                Math.min(MAX_POLLING_INTERVAL_MS,
                        (remainingMs >= 0 ? remainingMs : MIN_POLLING_INTERVAL_MS) + POLLING_TIME_BUFFER_MS));

        String waitMsg = secsToStrTime(remainingTimeSecs);
        Logger.printDebug(() -> "VOT: Poll #" + currentPollCount + " - Still processing Yandex request for " + yandexTargetLang + ". Wait estimate: " + waitMsg + ". Scheduling next poll in " + (sleepTimeMs / 1000.0) + "s.");
        postToMainThread(() -> callback.onProcessingStarted(waitMsg));

        mainThreadHandler.postDelayed(() -> pollTranslationStatusAsync(
                videoUrl, originalTargetLang, yandexTargetLang, session, false,
                videoTitle, durationSeconds, workflowStartTime, callback, currentPollCount + 1
        ), sleepTimeMs);
    }

    /**
     * Fetches the final list of available subtitle tracks asynchronously after translation is confirmed complete.
     * Handles session expiry and refreshes if needed.
     * <p>
     * HTTP Method: POST
     * <br>
     * Endpoint: /video-subtitles/get-subtitles (PATH_GET_SUBTITLES)
     * <br>
     * Purpose: Fetches the list of available original and translated subtitle tracks for a video, usually after the translation process is confirmed complete or to check for pre-existing subtitles.
     *
     * @param videoUrl           The URL of the video.
     * @param session            The current session information (might be refreshed internally).
     * @param originalTargetLang The user's final desired language.
     * @param yandexTargetLang   The language that was requested from Yandex.
     * @param workflowStartTime  The start time of the overall workflow for timeout checks.
     * @param callback           The callback to report success or failure.
     */
    private static void getFinalSubtitleTracksAsync(
            final String videoUrl,
            final SessionInfo session,
            final String originalTargetLang,
            final String yandexTargetLang,
            final long workflowStartTime,
            final SubtitleWorkflowCallback callback
    ) {
        if (System.currentTimeMillis() - workflowStartTime >= WORKFLOW_TIMEOUT_MS) {
            Logger.printException(() -> "VOT: Workflow timeout exceeded before fetching final subtitle list.");
            long timeoutSec = WORKFLOW_TIMEOUT_MS / 1000;
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_timeout_final_fetch", timeoutSec)));
            return;
        }

        Logger.printInfo(() -> "VOT: Step 5/5 - Calling " + PATH_GET_SUBTITLES + " (Async) URL: " + videoUrl);
        postToMainThread(() -> callback.onProcessingStarted(str("revanced_yandex_status_fetching_subs")));

        ManualSubtitlesRequest requestProto = new ManualSubtitlesRequest();
        requestProto.url = videoUrl;
        requestProto.language = "auto"; // Request all available

        byte[] requestBodyBytes;
        Headers headers;
        SessionInfo currentValidSession = session;

        try {
            if (!currentValidSession.isValid()) {
                Logger.printInfo(() -> "VOT: Session expired before final subtitle fetch. Attempting refresh.");
                try {
                    currentValidSession = ensureSession();
                    if (!currentValidSession.isValid()) {
                        Logger.printException(() -> "VOT: Session refresh failed or still invalid before final subtitle fetch.");
                        postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_session_expired")));
                        return;
                    }
                    Logger.printInfo(() -> "VOT: Session refreshed successfully before final subtitle fetch.");
                } catch (Exception e) {
                    Logger.printException(() -> "VOT: Error refreshing session before final subtitle fetch.", e);
                    postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_session_refresh_failed")));
                    return;
                }
            }
            requestBodyBytes = requestProto.toByteArray();
            headers = buildRequestHeaders(currentValidSession, requestBodyBytes, PATH_GET_SUBTITLES, VSUBS_PREFIX);
        } catch (IOException | GeneralSecurityException e) {
            Logger.printException(() -> "VOT: Failed to build request for final subtitle fetch", e);
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_internal_request_build") + ": " + e.getMessage()));
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + PATH_GET_SUBTITLES)
                .headers(headers)
                .post(RequestBody.create(requestBodyBytes, MediaType.parse("application/x-protobuf")))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.printException(() -> "VOT: Network error fetching final subtitle list", e);
                postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_network_final_subs") + ": " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        String bodyString = "<failed>";
                        try {if (response.body() != null) bodyString = response.body().string();} catch (Exception ignored) {}
                        final String finalBodyString = bodyString;
                        Logger.printException(() -> "VOT: Failed final subtitle fetch API response: " + response.code() + " " + response.message() + "\nBody: " + finalBodyString);
                        postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_api_final_subs", response.code())));
                        return;
                    }

                    byte[] responseBytes = responseBody.bytes();
                    ManualSubtitlesResponse finalSubsResponse;
                    try {
                        finalSubsResponse = ManualSubtitlesResponse.parseFrom(responseBytes);
                        String availableSubsLog = (finalSubsResponse.subtitles != null && !finalSubsResponse.subtitles.isEmpty()) ?
                                finalSubsResponse.subtitles.stream().map(s -> (s.translatedLanguage != null ? s.language + "->" + s.translatedLanguage : s.language)).collect(Collectors.joining(", "))
                                : "None";
                        Logger.printInfo(() -> "VOT: Final Subtitle tracks response. Waiting: " + finalSubsResponse.waiting + ", Subtitles: [" + availableSubsLog + "]");

                    } catch (IOException e) {
                        Logger.printException(() -> "VOT: Failed to parse final subtitle list response", e);
                        postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_parsing_final_subs")));
                        return;
                    }

                    Logger.printInfo(() -> "VOT: Step 6/5 - Processing final subtitle list and fetching content...");
                    processAndFetchFinalSubtitles(finalSubsResponse, originalTargetLang, yandexTargetLang, callback);

                } catch (IOException e) {
                    Logger.printException(() -> "VOT: IOException reading response body on final sub fetch", e);
                    postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_reading_response")));
                }
            }
        });
    }

    /**
     * Sends an asynchronous PUT request to the /video-translation/fail-audio-js endpoint.
     * This is part of the handling specific to YouTube videos when receiving status code 6.
     * Failures in this request are logged but generally do not halt the main translation workflow.
     * <p>
     * HTTP Method: PUT
     * <br>
     * Endpoint: /video-translation/fail-audio-js (PATH_FAIL_AUDIO_JS)
     * <br>
     * Purpose: Part of the specific handling for YouTube videos when the API returns STATUS_AUDIO_REQUESTED (Status 6). Sends a specific signal related to audio processing failure.
     *
     * @param videoUrl The URL of the video.
     */
    private static void sendFailAudioJsRequestAsync(String videoUrl) {
        Logger.printDebug(() -> "VOT: Sending PUT request to " + PATH_FAIL_AUDIO_JS + " for YouTube status 6 handling.");

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("video_url", videoUrl);
        } catch (JSONException e) {
            Logger.printException(() -> "VOT: Failed to create JSON body for fail-audio-js request", e);
            // Don't fail the whole workflow, just log this specific failure.
            return;
        }

        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Headers headers = new Headers.Builder()
                .add("User-Agent", USER_AGENT)
                .add("Accept", "*/*")
                .add("Content-Type", "application/json")
                .add("Sec-Fetch-Mode", "no-cors")
                .add("Sec-Fetch-Dest", "empty")
                .add("Sec-Fetch-Site", "cross-site")
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + PATH_FAIL_AUDIO_JS)
                .headers(headers)
                .put(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.printException(() -> "VOT: Network error sending fail-audio-js request", e);
                // Don't fail the main workflow, just log.
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        String bodyString = "<failed>";
                        try {if (body != null) bodyString = body.string();} catch (Exception ignored) {}
                        final String finalBody = bodyString;
                        Logger.printException(() -> "VOT: Failed fail-audio-js request: " + response.code() + " " + response.message() + "\nBody: " + finalBody);
                    } else {
                        Logger.printDebug(() -> "VOT: Successfully sent fail-audio-js request.");
                    }
                }
            }
        });
    }

    /**
     * Sends an asynchronous PUT request to the /video-translation/audio endpoint.
     * This is part of the handling specific to YouTube videos when receiving status code 6.
     * This request uses the standard protobuf format and session authentication headers.
     * Failures in this request are logged but generally do not halt the main translation workflow.
     * <p>
     * HTTP Method: PUT
     * <br>
     * Endpoint: /video-translation/audio (PATH_SEND_AUDIO)
     * <br>
     * Purpose: Also part of the handling for YouTube STATUS_AUDIO_REQUESTED (Status 6). Sends a protobuf message containing video details and an empty audio buffer object with a specific file ID, signaling readiness or interaction related to audio processing.
     *
     * @param videoUrl      The URL of the video.
     * @param translationId The translation ID received in the status 6 response.
     * @param session       The current valid {@link SessionInfo}.
     */
    private static void sendAudioRequestAsync(String videoUrl, String translationId, SessionInfo session) {
        Logger.printDebug(() -> "VOT: Sending PUT request to " + PATH_SEND_AUDIO + " for YouTube status 6 handling.");

        ManualVideoTranslationAudioRequest requestProto = new ManualVideoTranslationAudioRequest();
        requestProto.url = videoUrl;
        requestProto.translationId = translationId;
        requestProto.audioInfo = new ManualAudioBufferObject();
        requestProto.audioInfo.fileId = FILE_ID_YOUTUBE_STATUS_6;
        requestProto.audioInfo.audioFile = new byte[0];

        byte[] requestBodyBytes;
        Headers headers;
        try {
            requestBodyBytes = requestProto.toByteArray();
            headers = buildRequestHeaders(session, requestBodyBytes, PATH_SEND_AUDIO, VTRANS_PREFIX);
        } catch (IOException | GeneralSecurityException e) {
            Logger.printException(() -> "VOT: Failed to build request for /audio PUT", e);
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + PATH_SEND_AUDIO)
                .headers(headers)
                .put(RequestBody.create(requestBodyBytes, MediaType.parse("application/x-protobuf")))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.printException(() -> "VOT: Network error sending /audio PUT request", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        String bodyString = "<failed>";
                        try {if (body != null) bodyString = body.string();} catch (Exception ignored) {}
                        final String finalBody = bodyString;
                        Logger.printException(() -> "VOT: Failed /audio PUT request: " + response.code() + " " + response.message() + "\nBody: " + finalBody);
                    } else {
                        Logger.printDebug(() -> "VOT: Successfully sent /audio PUT request.");
                    }
                }
            }
        });
    }

    // endregion API Calls

    // region Utils

    /**
     * Processes the response containing the list of final subtitle tracks.
     * It selects the best track for the *Yandex target language*, determines the correct URL,
     * and initiates the download. It then decides whether to parse the result or pass the raw JSON.
     *
     * @param response           The {@link ManualSubtitlesResponse} received from the API. Can be null.
     * @param originalTargetLang The user's final desired language code.
     * @param yandexTargetLang   The language code that was requested from Yandex ('en', 'ru', 'kk').
     * @param callback           The callback to report success (intermediate or final) or failure.
     */
    private static void processAndFetchFinalSubtitles(
            @Nullable ManualSubtitlesResponse response,
            String originalTargetLang,
            String yandexTargetLang,
            SubtitleWorkflowCallback callback) {
        if (response == null) {
            Logger.printException(() -> "VOT: processAndFetchFinalSubtitles received null response!");
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_internal_null_subs_response")));
            return;
        }

        if (response.waiting) {
            Logger.printInfo(() -> "VOT: Subtitle check unexpectedly still shows 'waiting' after polling success. Treating as error.");
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_subs_stuck_processing")));
            return;
        }

        final List<ManualSubtitlesObject> availableSubs = response.subtitles;
        if (availableSubs == null || availableSubs.isEmpty()) {
            Logger.printInfo(() -> "VOT: Yandex finished, but no subtitle tracks returned in the final list.");
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_no_subs_returned")));
            return;
        }

        String log = availableSubs.stream()
                .map(s -> "{Orig:" + s.language + (TextUtils.isEmpty(s.url) ? "(X)" : "")
                        + (!TextUtils.isEmpty(s.translatedLanguage)
                        ? (",Trans:" + s.translatedLanguage + (TextUtils.isEmpty(s.translatedUrl) ? "(X)" : "")) : "") + "}")
                .collect(Collectors.joining(", "));
        Logger.printInfo(() -> "VOT: Processing final tracks. Original Target: " + originalTargetLang + ", Yandex Target: " + yandexTargetLang + ". Available: [" + log + "]");

        ManualSubtitlesObject chosenSub = findBestSubtitleForLanguage(availableSubs, yandexTargetLang);

        if (chosenSub == null) {
            Logger.printInfo(() -> "VOT: No suitable Yandex track found matching Yandex target language: " + yandexTargetLang);
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_no_subs_for_language", yandexTargetLang)));
            return;
        }

        Logger.printInfo(() -> "VOT: Chosen track for Yandex target " + yandexTargetLang + " - Original: " + chosenSub.language
                + ", Translated: " + chosenSub.translatedLanguage
                + ", Has Orig URL: " + !TextUtils.isEmpty(chosenSub.url)
                + ", Has Trans URL: " + !TextUtils.isEmpty(chosenSub.translatedUrl));

        String subtitleUrl = determineSubtitleUrl(chosenSub, yandexTargetLang);

        if (TextUtils.isEmpty(subtitleUrl)) {
            Logger.printException(() -> "VOT: Chosen subtitle track for Yandex target " + yandexTargetLang + " has no valid URL!");
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_internal_no_chosen_url")));
            return;
        }

        Logger.printInfo(() -> "VOT: Final subtitle URL selected (for lang " + yandexTargetLang + "): " + subtitleUrl);
        fetchSubtitleContent(subtitleUrl, originalTargetLang, yandexTargetLang, callback);
    }

    /**
     * Selects the most suitable subtitle track from a list based on the target language.
     * Priority:
     * 1. A translated track matching the target language with a valid translated URL.
     * 2. An original track matching the target language with a valid original URL (only if no translation exists for it).
     *
     * @param subs       The list of available {@link ManualSubtitlesObject} tracks.
     * @param targetLang The desired target language code.
     * @return The best matching {@link ManualSubtitlesObject}, or null if no suitable track is found.
     */
    @Nullable
    private static ManualSubtitlesObject findBestSubtitleForLanguage(List<ManualSubtitlesObject> subs, String targetLang) {
        ManualSubtitlesObject translatedMatch = null;
        ManualSubtitlesObject originalMatch = null;

        Logger.printDebug(() -> "VOT: Searching for best subtitle track for target language: " + targetLang);

        for (ManualSubtitlesObject sub : subs) {
            String nO = sub.language;
            String nT = sub.translatedLanguage;

            boolean hasT = !TextUtils.isEmpty(sub.translatedUrl);
            boolean hasO = !TextUtils.isEmpty(sub.url);

            Logger.printDebug(() -> "  Checking track: Orig=" + sub.language + " (" + nO + ")" + (hasO ? "" : "(X)") + ", Trans=" + sub.translatedLanguage + " (" + nT + ")" + (hasT ? "" : "(X)"));

            // Priority 1: Translated track matching target language with a valid translated URL
            if (targetLang.equals(nT) && hasT) {
                Logger.printDebug(() -> "    -> Found priority 1: Translated match for " + targetLang);
                translatedMatch = sub;
                break;
            }

            // Priority 2: Original track matching target language (implies no translation was needed/available) with a valid original URL
            // AND this track is NOT also a translated track (to avoid picking an original if a translated one is also listed)
            if (originalMatch == null && targetLang.equals(nO) && hasO && TextUtils.isEmpty(nT)) {
                Logger.printDebug(() -> "    -> Found priority 2: Original match for " + targetLang + " (no translation offered).");
                originalMatch = sub;
                // Continue checking in case a translated match (Priority 1) appears later in the list
            }
        }

        if (translatedMatch != null) {
            Logger.printInfo(() -> "VOT: Selected translated track for " + targetLang);
            return translatedMatch;
        }

        if (originalMatch != null) {
            Logger.printInfo(() -> "VOT: Selected original track for " + targetLang);
            return originalMatch;
        }

        return null;
    }

    /**
     * Determines the correct subtitle URL (either original or translated) from a chosen subtitle object,
     * based on the target language.
     *
     * @param chosenSub  The non-null {@link ManualSubtitlesObject} selected as the best match.
     * @param targetLang The non-null desired target language code.
     * @return The appropriate URL string (either {@code chosenSub.url} or {@code chosenSub.translatedUrl}),
     * or null if a valid URL cannot be determined based on the criteria.
     */
    @Nullable
    private static String determineSubtitleUrl(@NonNull ManualSubtitlesObject chosenSub, @NonNull String targetLang) {
        String nT = chosenSub.translatedLanguage;
        String nO = chosenSub.language;

        boolean hasTranslatedUrl = !TextUtils.isEmpty(chosenSub.translatedUrl);
        boolean hasOriginalUrl = !TextUtils.isEmpty(chosenSub.url);

        // Case 1: The chosen track is a translated track that matches the target language, AND it has a translated URL.
        if (targetLang.equals(nT) && hasTranslatedUrl) {
            Logger.printDebug(() -> "VOT: Using URL from *translated* match: " + chosenSub.translatedUrl);
            return chosenSub.translatedUrl;
        }

        // Case 2: The chosen track is an original track that matches the target language AND it has an original URL.
        // This handles cases where translation wasn't needed or where only the original was found.
        // We prefer the original URL here if the target language is the original language.
        if (targetLang.equals(nO) && hasOriginalUrl) {
            Logger.printDebug(() -> "VOT: Using URL from *original* match: " + chosenSub.url);
            return chosenSub.url;
        }

        // Fallback: If the chosen sub is translated but the target *isn't* the translated lang (e.g. requesting 'en' but found 'ru->de'),
        // and the translated URL exists, return that. This is less ideal but might happen.
        if (!targetLang.equals(nT) && hasTranslatedUrl) {
            Logger.printDebug(() -> "VOT: Using URL from *translated* match (fallback, target != translated): " + chosenSub.translatedUrl);
            return chosenSub.translatedUrl;
        }

        Logger.printException(() -> "VOT: Internal Logic Error in determineSubtitleUrl. Chosen sub does not yield a URL matching target criteria. Chosen: "
                + chosenSub.language + " (" + nO + ")" + (hasOriginalUrl ? "" : "(X)")
                + " -> " + chosenSub.translatedLanguage + " (" + nT + ")" + "(X)"
                + ", Target: " + targetLang);
        return null;
    }

    /**
     * Fetches the actual subtitle content from the given URL asynchronously.
     * Parses the content upon successful download IF the language matches the original target,
     * otherwise calls the intermediate success callback with the raw JSON.
     * <p>
     * HTTP Method: GET
     * <br>
     * Endpoint: <subtitle_file_url> (Dynamic URL, e.g., from sub.url or sub.translatedUrl fields in the /video-subtitles/get-subtitles response)
     * <br>
     * Purpose: Downloads the actual subtitle content (in JSON format) from the URL provided by the API in the subtitle track list. This URL is typically not under the /api.browser.yandex.ru/ base path but points directly to the file, often on a CDN.
     *
     * @param url                The URL of the subtitle file (JSON format expected).
     * @param originalTargetLang The user's final desired language.
     * @param yandexTargetLang   The language of the content being fetched from the URL.
     * @param callback           The callback to report the final parsed subtitles, intermediate JSON, or an error.
     */
    private static void fetchSubtitleContent(String url, final String originalTargetLang, final String yandexTargetLang, final SubtitleWorkflowCallback callback) {
        Request request = new Request.Builder().url(url).get().build();
        Logger.printInfo(() -> "VOT: Fetching final subtitle content (Lang: " + yandexTargetLang + ") from: " + url);

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.printException(() -> "VOT: Failed fetch subtitle content: " + url, e);
                postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_network_subs_fetch")));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Logger.printDebug(() -> "VOT: Subtitle content response code: " + response.code());
                if (!response.isSuccessful() || response.body() == null) {
                    String bodyPreview = "<failed>";
                    int code = response.code();
                    String message = response.message();
                    try {
                        if (response.body() != null) {
                            bodyPreview = response.body().source().peek().readString(1024, StandardCharsets.UTF_8); // Read up to 1KB
                            if (bodyPreview.length() >= 1024) bodyPreview += "...";
                        }
                    } catch (Exception ignored) {}
                    response.close();
                    final String finalBodyPreview = bodyPreview;
                    Logger.printException(() -> "VOT: Failed download subtitle file: " + code + " Msg: " + message + " Preview: " + finalBodyPreview);
                    final String errorMsg = str("revanced_yandex_error_download_subs_failed", code);
                    postToMainThread(() -> callback.onFinalFailure(errorMsg));
                    return;
                }

                try (ResponseBody body = response.body()) {
                    String subtitleText = body.string(); // Read the full content
                    Logger.printInfo(() -> "VOT: Subtitle content fetched (Lang: " + yandexTargetLang + ", " + subtitleText.length() + " chars). Processing...");

                    boolean needsSecondaryTranslation = !originalTargetLang.equals(yandexTargetLang);

                    if (needsSecondaryTranslation) {
                        Logger.printInfo(() -> "VOT: Passing raw JSON (Lang: " + yandexTargetLang + ") for secondary translation to " + originalTargetLang);
                        postToMainThread(() -> callback.onIntermediateSuccess(subtitleText, yandexTargetLang));
                    } else {
                        Logger.printInfo(() -> "VOT: Parsing final subtitle content (Lang: " + originalTargetLang + ")");
                        TreeMap<Long, Pair<Long, String>> parsedData = parseYandexJsonSubtitles(subtitleText);
                        if (parsedData == null) {
                            Logger.printException(() -> "VOT: Failed to parse non-empty subtitle JSON for " + originalTargetLang + " from " + url);
                            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_subs_parsing_failed")));
                            return;
                        }
                        if (parsedData.isEmpty() && !subtitleText.trim().isEmpty()) {
                            Logger.printInfo(() -> "VOT: Parsed JSON for " + originalTargetLang + " resulted in zero entries from non-empty content.");
                            postToMainThread(() -> callback.onFinalSuccess(parsedData));
                            return;
                        }

                        if (parsedData.isEmpty() && subtitleText.trim().isEmpty()) {
                            Logger.printInfo(() -> "VOT: Fetched subtitle content for " + originalTargetLang + " was empty or whitespace.");
                            postToMainThread(() -> callback.onFinalSuccess(parsedData));
                            return;
                        }

                        Logger.printInfo(() -> "VOT: SUCCESS! Subtitle content parsed (Lang: " + originalTargetLang + ", " + parsedData.size() + " entries). Workflow complete.");
                        postToMainThread(() -> callback.onFinalSuccess(parsedData));
                    }

                } catch (Exception e) {
                    Logger.printException(() -> "VOT: Error processing/parsing subtitle content from " + url + " for lang " + yandexTargetLang, e);
                    postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_subs_processing_failed") + ": " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Parses subtitle data from a JSON string provided by the Yandex API.
     * Expects a JSON structure containing a "subtitles" array, or a root JSON array,
     * where each object has "startMs", ("endMs" or "durationMs"), and "text".
     *
     * @param jsonContent The raw JSON string content of the subtitles.
     * @return A {@link TreeMap} mapping start time (Long ms) to a Pair of end time (Long ms) and subtitle text (String).
     * Returns an empty map if the input is valid JSON but contains no valid subtitle entries.
     * Returns null if the input is empty, null, or cannot be parsed into the expected structure.
     * @throws JSONException If the input string is not valid JSON or doesn't conform to the expected structure.
     */
    @Nullable
    static TreeMap<Long, Pair<Long, String>> parseYandexJsonSubtitles(String jsonContent) throws JSONException {
        if (TextUtils.isEmpty(jsonContent)) {
            Logger.printInfo(() -> "VOT: parseYandexJsonSubtitles received empty content.");
            return null;
        }
        JSONArray subsArray = null;
        try {
            JSONObject root = new JSONObject(jsonContent);
            if (root.has("subtitles") && root.get("subtitles") instanceof JSONArray) {
                subsArray = root.getJSONArray("subtitles");
            }
        } catch (JSONException e) {
            // Ignore
        }

        if (subsArray == null && jsonContent.trim().startsWith("[")) {
            try {
                subsArray = new JSONArray(jsonContent);
            } catch (JSONException e) {
                Logger.printException(() -> "VOT: Content is not valid JSON object with 'subtitles' array or a root array.", e);
                throw e;
            }
        }

        if (subsArray == null) {
            Logger.printException(() -> "VOT: Cannot find 'subtitles' array in JSON content or content is not a root array.");
            return null;
        }

        if (subsArray.length() == 0) {
            Logger.printInfo(() -> "VOT: Parsed empty subtitles array.");
            return new TreeMap<>();
        }

        TreeMap<Long, Pair<Long, String>> map = new TreeMap<>();
        for (int i = 0; i < subsArray.length(); i++) {
            try {
                JSONObject subObj = subsArray.getJSONObject(i);
                double startMsDouble = subObj.optDouble("startMs", -1.0);
                double endMsDouble = subObj.optDouble("endMs", -1.0);
                double durationMsDouble = (endMsDouble < 0) ? subObj.optDouble("durationMs", -1.0) : -1.0;

                if (startMsDouble < 0) {
                    int finalI1 = i;
                    Logger.printInfo(() -> "VOT: Skipping subtitle entry #" + finalI1 + " due to missing or invalid startMs.");
                    continue;
                }

                long startMs = Math.round(startMsDouble);
                long endMs;

                if (endMsDouble >= 0) {
                    endMs = Math.round(endMsDouble);
                } else if (durationMsDouble >= 0) {
                    endMs = startMs + Math.round(durationMsDouble);
                } else {
                    int finalI2 = i;
                    Logger.printInfo(() -> "VOT: Skipping subtitle entry #" + finalI2 + " due to missing or invalid endMs/durationMs.");
                    continue;
                }

                if (endMs <= startMs) {
                    int finalI3 = i;
                    Logger.printInfo(() -> "VOT: Skipping subtitle entry #" + finalI3 + " due to invalid timing (end <= start). start=" + startMs + ", end=" + endMs);
                    continue;
                }

                String text = subObj.optString("text", "").trim();
                map.put(startMs, new Pair<>(endMs, text));
            } catch (JSONException e) {
                int finalI = i;
                Logger.printException(() -> "VOT: Error parsing subtitle object #" + finalI, e);
            }
        }

        return (map.isEmpty() && subsArray.length() > 0) ? null : map;
    }

    /**
     * Posts a {@link Runnable} to be executed on the main application thread.
     *
     * @param runnable The Runnable to execute on the main thread.
     */
    private static void postToMainThread(Runnable runnable) {
        mainThreadHandler.post(runnable);
    }

    /**
     * Builds the necessary HTTP headers for authenticated Yandex API requests,
     * including User-Agent, Content-Type, signatures, and security tokens.
     *
     * @param session      The current valid {@link SessionInfo}.
     * @param bodyBytes    The byte array of the request body (used for body signature). Can be null or empty if no body.
     * @param path         The API endpoint path (e.g., "/video-translation/translate").
     * @param modulePrefix The prefix for signature/token headers ("Vtrans" or "Vsubs").
     * @return The constructed {@link Headers} object.
     * @throws GeneralSecurityException If a cryptographic error occurs during signature calculation.
     */
    private static Headers buildRequestHeaders(SessionInfo session, byte[] bodyBytes, String path, String modulePrefix) throws GeneralSecurityException {
        try {
            byte[] dataToSign = (bodyBytes != null) ? bodyBytes : new byte[0];
            String bodySignature = calculateSignature(dataToSign);

            String tokenPart = session.uuid + ":" + path + ":" + COMPONENT_VERSION;
            byte[] tokenPartBytes = tokenPart.getBytes(StandardCharsets.UTF_8);
            String tokenSignature = calculateSignature(tokenPartBytes);

            if (TextUtils.isEmpty(tokenSignature)) {
                Logger.printException(() -> "VOT: Calculated Token Signature is empty!");
                throw new GeneralSecurityException("Token Signature cannot be empty");
            }

            String secToken = tokenSignature + ":" + tokenPart;

            String secChUa = "\"Chromium\";v=\"132\", \"YaBrowser\";v=\"" + COMPONENT_VERSION.substring(0, COMPONENT_VERSION.indexOf('.')) + "\", \"Not?A_Brand\";v=\"99\", \"Yowser\";v=\"2.5\"";
            String secChUaFull = "\"Chromium\";v=\"132.0.6834.685\", \"YaBrowser\";v=\"" + COMPONENT_VERSION + "\", \"Not?A_Brand\";v=\"99.0.0.0\", \"Yowser\";v=\"2.5\"";

            return new Headers.Builder()
                    .add("User-Agent", USER_AGENT)
                    .add("Accept", "application/x-protobuf")
                    .add("Content-Type", "application/x-protobuf")
                    .add("Pragma", "no-cache")
                    .add("Cache-Control", "no-cache")
                    .add(modulePrefix + "-Signature", bodySignature)
                    .add("Sec-" + modulePrefix + "-Sk", session.secretKey)
                    .add("Sec-" + modulePrefix + "-Token", secToken)
                    .add("sec-ch-ua", secChUa)
                    .add("sec-ch-ua-full-version-list", secChUaFull)
                    .add("Sec-Fetch-Mode", "no-cors")
                    .add("Sec-Fetch-Dest", "empty")
                    .add("Sec-Fetch-Site", "cross-site")
                    .build();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Logger.printException(() -> "VOT: Crypto error building headers", e);
            throw new GeneralSecurityException("Crypto error building headers", e);
        }
    }

    /**
     * Calculates the HMAC-SHA256 signature for the given data using the predefined {@link #HMAC_KEY}.
     *
     * @param data The byte array to sign. Can be null or empty.
     * @return The hexadecimal representation of the HMAC-SHA256 signature.
     * @throws NoSuchAlgorithmException If the HmacSHA256 algorithm is not available.
     * @throws InvalidKeyException      If the {@link #HMAC_KEY} is invalid.
     */
    private static String calculateSignature(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        final String ALGORITHM = "HmacSHA256";
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(HMAC_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(data != null ? data : new byte[0]);
        return bytesToHex(signatureBytes);
    }

    /**
     * Converts a byte array into its hexadecimal string representation.
     *
     * @param bytes The byte array to convert. Can be null.
     * @return The hex string, or an empty string if the input is null.
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        char[] h = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            h[j * 2] = HEX_ARRAY[v >>> 4];
            h[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(h);
    }

    /**
     * Converts a duration in seconds into a user-friendly relative time string (e.g., "in about a minute", "in 5 minutes").
     * Used for displaying estimated processing time.
     *
     * @param secs The duration in seconds. Can be negative to indicate a short wait.
     * @return A localized string representing the approximate time.
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
     * Forces the release of the workflow lock for a specific video URL.
     * This should ONLY be called externally when an operation associated with
     * this URL is explicitly cancelled by the managing component (e.g., GeminiManager).
     * Note: This does NOT stop the background thread if it's still running, but it
     * allows a new workflow for the same URL to start immediately.
     *
     * @param videoUrl The URL for which to release the lock.
     */
    public static void forceReleaseWorkflowLock(@Nullable String videoUrl) {
        if (videoUrl == null) return;

        AtomicBoolean lock = urlWorkflowLocks.remove(videoUrl);
        if (lock != null) {
            // Optionally, set the AtomicBoolean to false, though removing is usually sufficient
            // lock.set(false);
            Logger.printInfo(() -> "VOT: Force-released workflow lock for URL: " + videoUrl + " due to external cancellation request.");
        } else {
            Logger.printDebug(() -> "VOT: Attempted to force-release lock for URL: " + videoUrl + ", but no lock was found (already released or never existed?).");
        }
    }

    // endregion Utils

    // --- Interfaces and Inner Classes ---

    /**
     * Callback interface for the asynchronous Yandex subtitle workflow.
     * Provides methods to report the final result (success or failure)
     * and intermediate processing status updates. If the requested language
     * requires a secondary translation step (e.g., via Gemini),
     * onIntermediateSuccess is called with the raw JSON from Yandex.
     */
    public interface SubtitleWorkflowCallback {
        /**
         * Called when the workflow completes successfully AND the result is
         * in the final desired language (either directly from Yandex or after
         * a secondary translation step handled by the caller).
         *
         * @param parsedSubtitles The final parsed subtitle data.
         */
        void onFinalSuccess(TreeMap<Long, Pair<Long, String>> parsedSubtitles);

        /**
         * Called when the workflow successfully retrieves subtitles from Yandex,
         * but these subtitles are in an intermediate language (e.g., 'en') and
         * require a secondary translation step by the caller to reach the user's
         * originally requested language.
         *
         * @param rawIntermediateJson The raw JSON string content of the subtitles
         *                            in the intermediate language (e.g., 'en', 'ru', 'kk').
         * @param intermediateLang    The language code of the rawIntermediateJson (e.g., "en").
         */
        void onIntermediateSuccess(String rawIntermediateJson, String intermediateLang);

        void onFinalFailure(String errorMessage);

        void onProcessingStarted(String statusMessage);
    }

    /**
     * Represents information about an active Yandex API session, including
     * the unique session UUID, the secret key for signing requests, and the
     * calculated expiration time.
     */
    private static class SessionInfo {
        final String uuid;
        final String secretKey;
        final long expiresAtMillis;

        SessionInfo(String u, String s, int e) {
            this.uuid = u;
            this.secretKey = s;
            long durationMillis = (e > 0) ? TimeUnit.SECONDS.toMillis(e) : TimeUnit.HOURS.toMillis(1);
            long safetyMarginMillis = TimeUnit.MINUTES.toMillis(1);
            this.expiresAtMillis = System.currentTimeMillis() + durationMillis - safetyMarginMillis;
            Logger.printDebug(() -> "VOT: Session created. Expires in " + (expiresAtMillis - System.currentTimeMillis()) + " ms.");
        }

        /**
         * Checks if the session is still valid based on its expiration time,
         * applying a small safety margin.
         *
         * @return true if the session is considered valid, false otherwise.
         */
        boolean isValid() {
            // Add a 30-second buffer
            return System.currentTimeMillis() < (this.expiresAtMillis - 30000);
        }
    }

    // region Protobuf Manual Classes

    /**
     * Manual representation of the protobuf message for creating a Yandex session.
     * Contains the UUID and the requested module name.
     */
    public static class ManualYandexSessionRequest {
        String uuid;    // 1
        String module;  // 2

        /**
         * Serializes this request object into a protobuf byte array.
         *
         * @return The serialized byte array.
         * @throws IOException If an error occurs during serialization.
         */
        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            if (uuid != null && !uuid.isEmpty()) ProtoWriter.writeString(d, 1, uuid);
            if (module != null && !module.isEmpty()) ProtoWriter.writeString(d, 2, module);
            return b.toByteArray();
        }
    }

    /**
     * Manual representation of the protobuf message for the Yandex session creation response.
     * Contains the secret key and expiration time (in seconds) for the session.
     */
    public static class ManualYandexSessionResponse {
        String secretKey;   // 1
        int expires;        // 2

        /**
         * Parses a Yandex session response from a protobuf byte array.
         *
         * @param dt The byte array containing the serialized protobuf data.
         * @return A parsed {@link ManualYandexSessionResponse} object.
         * @throws IOException If an error occurs during parsing or the data is malformed.
         */
        static ManualYandexSessionResponse parseFrom(byte[] dt) throws IOException {
            ManualYandexSessionResponse r = new ManualYandexSessionResponse();
            DataInputStream d = new DataInputStream(new ByteArrayInputStream(dt));
            while (d.available() > 0) {
                int t = ProtoReader.readVarint32(d);
                int f = t >>> 3; // field number
                int w = t & 7; // wire type
                switch (f) {
                    case 1: // secretKey (string, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 1: " + w);
                        r.secretKey = ProtoReader.readString(d);
                        break;
                    case 2: // expires (int, wire type 0)
                        if (w != 0) throw new IOException("Unexpected wire type for field 2: " + w);
                        r.expires = ProtoReader.readVarint32(d);
                        break;
                    default:
                        ProtoReader.skipField(d, t);
                        break;
                }
            }
            return r;
        }
    }

    /**
     * Manual representation of the protobuf message for requesting subtitle tracks.
     * Contains the video URL and optionally the requested language ("auto" for all).
     */
    public static class ManualSubtitlesRequest {
        String url;         // 1
        String language;    // 2

        /**
         * Serializes this request object into a protobuf byte array.
         *
         * @return The serialized byte array.
         * @throws IOException If an error occurs during serialization.
         */
        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            if (url != null && !url.isEmpty()) ProtoWriter.writeString(d, 1, url);
            if (language != null && !language.isEmpty()) ProtoWriter.writeString(d, 2, language);
            return b.toByteArray();
        }
    }

    /**
     * Manual representation of a single subtitle track object within a subtitles' response.
     * Contains information about the original language/URL and the translated language/URL, if available.
     */
    public static class ManualSubtitlesObject {
        String language;            // 1
        String url;                 // 2
        String translatedLanguage;  // 4
        String translatedUrl;       // 5

        /**
         * Parses a single subtitle object from a DataInputStream, assuming it's an embedded message
         * prefixed with its length.
         *
         * @param dis The DataInputStream positioned at the start of the length prefix.
         * @return A parsed {@link ManualSubtitlesObject}.
         * @throws IOException If an error occurs during reading or parsing.
         */
        static ManualSubtitlesObject parseFrom(DataInputStream dis) throws IOException {
            ManualSubtitlesObject o = new ManualSubtitlesObject();
            // Read message length prefix first
            int l = ProtoReader.readVarint32(dis);
            if (l < 0) throw new IOException("Invalid negative message length: " + l);
            // Limit length to prevent OOM, e.g., 1MB max per sub-object message
            final int MAX_SUB_OBJ_LEN = 1024 * 1024; // 1MB
            if (l > MAX_SUB_OBJ_LEN) throw new IOException("Message length too large: " + l);
            if (l == 0) return o; // Handle empty message gracefully

            // Read the sub-message bytes and parse from a new stream
            byte[] m = new byte[l];
            dis.readFully(m);
            DataInputStream n = new DataInputStream(new ByteArrayInputStream(m));
            while (n.available() > 0) {
                int t = ProtoReader.readVarint32(n);
                int f = t >>> 3; // field number
                int w = t & 7; // wire type
                switch (f) {
                    case 1: // language (string, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 1: " + w);
                        o.language = ProtoReader.readString(n);
                        break;
                    case 2: // url (string, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 2: " + w);
                        o.url = ProtoReader.readString(n);
                        break;
                    case 4: // translatedLanguage (string, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 4: " + w);
                        o.translatedLanguage = ProtoReader.readString(n);
                        break;
                    case 5: // translatedUrl (string, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 5: " + w);
                        o.translatedUrl = ProtoReader.readString(n);
                        break;
                    default:
                        ProtoReader.skipField(n, t);
                        break;
                }
            }
            return o;
        }
    }

    /**
     * Manual representation of the protobuf message for the subtitle tracks response.
     * Contains a flag indicating if processing is still ongoing (`waiting`) and a list
     * of available {@link ManualSubtitlesObject} tracks.
     */
    public static class ManualSubtitlesResponse {
        boolean waiting;                                            // 1
        List<ManualSubtitlesObject> subtitles = new ArrayList<>();  // 2

        /**
         * Parses a subtitle tracks response from a protobuf byte array.
         *
         * @param dt The byte array containing the serialized protobuf data.
         * @return A parsed {@link ManualSubtitlesResponse} object.
         * @throws IOException If an error occurs during parsing or the data is malformed.
         */
        static ManualSubtitlesResponse parseFrom(byte[] dt) throws IOException {
            ManualSubtitlesResponse r = new ManualSubtitlesResponse();
            DataInputStream d = new DataInputStream(new ByteArrayInputStream(dt));
            while (d.available() > 0) {
                int t = ProtoReader.readVarint32(d);
                int f = t >>> 3; // field number
                int w = t & 7; // wire type
                switch (f) {
                    case 1: // waiting (bool, wire type 0)
                        if (w != 0) throw new IOException("Unexpected wire type for field 1: " + w);
                        r.waiting = ProtoReader.readBool(d);
                        break;
                    case 2: // subtitles (repeated message, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 2: " + w);
                        try {
                            r.subtitles.add(ManualSubtitlesObject.parseFrom(d)); // Message parsing reads its own length
                        } catch (IOException e) {
                            Logger.printException(() -> "VOT Error parsing ManualSubtitlesObject", e);
                            throw e;
                        }
                        break;
                    default:
                        ProtoReader.skipField(d, t);
                        break;
                }
            }
            return r;
        }
    }

    /**
     * Manual representation of the protobuf message for requesting or polling video translation.
     * Contains details like the video URL, duration, target language, and flags controlling the request type.
     */
    public static class ManualVideoTranslationRequest {
        String url;             // 3, string
        boolean firstRequest;   // 5, bool (true to initiate, false to poll)
        double duration;        // 6, fixed64 (double)
        int unknown0;           // 7, varint (int32) - value 1 seems constant
        String language;        // 8, string (source language, "auto" works)
        int unknown1;           // 10, varint (int32) - value 0 seems constant
        String responseLanguage;// 14, string (target language)
        int unknown2;           // 15, varint (int32) - value 0 seems constant
        int unknown3;           // 16, varint (int32) - value 1 seems constant
        boolean bypassCache;    // 17, bool (usually false)
        boolean useNewModel;    // 18, bool (usually true)
        String videoTitle;      // 19, string  <--- ADDED FIELD based on userscript

        /**
         * Serializes this request object into a protobuf byte array.
         *
         * @return The serialized byte array.
         * @throws IOException If an error occurs during serialization.
         */
        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            if (url != null && !url.isEmpty()) ProtoWriter.writeString(d, 3, url);
            ProtoWriter.writeBool(d, 5, firstRequest);
            if (duration > 0) ProtoWriter.writeDouble(d, duration);
            if (unknown0 != 0) ProtoWriter.writeInt32(d, 7, unknown0);
            if (language != null && !language.isEmpty()) ProtoWriter.writeString(d, 8, language);
            if (unknown1 != 0) ProtoWriter.writeInt32(d, 10, unknown1);
            if (responseLanguage != null && !responseLanguage.isEmpty()) ProtoWriter.writeString(d, 14, responseLanguage);
            if (unknown2 != 0) ProtoWriter.writeInt32(d, 15, unknown2);
            if (unknown3 != 0) ProtoWriter.writeInt32(d, 16, unknown3);
            ProtoWriter.writeBool(d, 17, bypassCache);
            ProtoWriter.writeBool(d, 18, useNewModel);
            if (videoTitle != null && !videoTitle.isEmpty()) ProtoWriter.writeString(d, 19, videoTitle);
            return b.toByteArray();
        }
    }

    /**
     * Manual representation of the protobuf message for the video translation status response.
     * Contains the current status code, estimated remaining time, detected language,
     * translation ID, and potentially an error message.
     */
    public static class ManualVideoTranslationResponse {
        String url;             // 1
        double duration;        // 2
        int status;             // 4
        int remainingTime;      // 5
        String translationId;   // 7
        String language;        // 8
        String message;         // 9

        /**
         * Parses a video translation status response from a protobuf byte array.
         *
         * @param dt The byte array containing the serialized protobuf data.
         * @return A parsed {@link ManualVideoTranslationResponse} object.
         * @throws IOException If an error occurs during parsing or the data is malformed.
         */
        static ManualVideoTranslationResponse parseFrom(byte[] dt) throws IOException {
            ManualVideoTranslationResponse r = new ManualVideoTranslationResponse();
            DataInputStream d = new DataInputStream(new ByteArrayInputStream(dt));
            while (d.available() > 0) {
                int t = ProtoReader.readVarint32(d);
                int f = t >>> 3; // field number
                int w = t & 7; // wire type
                switch (f) {
                    case 1: // url (string, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 1: " + w);
                        r.url = ProtoReader.readString(d);
                        break;
                    case 2: // duration (double, wire type 1)
                        if (w != 1) throw new IOException("Unexpected wire type for field 2: " + w);
                        r.duration = ProtoReader.readDouble(d);
                        break;
                    case 4: // status (int, wire type 0)
                        if (w != 0) throw new IOException("Unexpected wire type for field 4: " + w);
                        r.status = ProtoReader.readVarint32(d);
                        break;
                    case 5: // remainingTime (int, wire type 0)
                        if (w != 0) throw new IOException("Unexpected wire type for field 5: " + w);
                        r.remainingTime = ProtoReader.readVarint32(d);
                        break;
                    case 7: // translationId (string, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 7: " + w);
                        r.translationId = ProtoReader.readString(d);
                        break;
                    case 8: // language (string, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 8: " + w);
                        r.language = ProtoReader.readString(d);
                        break;
                    case 9: // message (string, wire type 2)
                        if (w != 2) throw new IOException("Unexpected wire type for field 9: " + w);
                        r.message = ProtoReader.readString(d);
                        break;
                    default:
                        ProtoReader.skipField(d, t);
                        break;
                }
            }
            return r;
        }
    }

    /**
     * Manual representation of a protobuf message containing audio data, typically used
     * within other request messages. Contains a file ID and the raw audio bytes.
     * Used specifically for the YouTube status 6 workaround.
     */
    public static class ManualAudioBufferObject {
        String fileId;      // 1, string
        byte[] audioFile;   // 2, bytes

        /**
         * Serializes this object into a protobuf byte array.
         *
         * @return The serialized byte array.
         * @throws IOException If an error occurs during serialization.
         */
        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            if (fileId != null && !fileId.isEmpty()) ProtoWriter.writeString(d, 1, fileId);
            if (audioFile != null && audioFile.length > 0) ProtoWriter.writeBytes(d, 2, audioFile);
            return b.toByteArray();
        }
    }

    /**
     * Manual representation of a protobuf message potentially related to chunked audio uploads (speculative).
     * Contains an audio buffer object, file ID, and other fields.
     * Currently used only as part of {@link ManualVideoTranslationAudioRequest}.
     */
    public static class ManualChunkAudioObject {
        int audioPartsLength;                   // 2, int32
        ManualAudioBufferObject audioBuffer;    // 1, message
        String fileId;                          // 3, string
        int unknown0;                           // 4, int32

        /**
         * Serializes this object into a protobuf byte array, including embedding the audioBuffer message.
         *
         * @return The serialized byte array.
         * @throws IOException If an error occurs during serialization.
         */
        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            if (audioBuffer != null) {
                byte[] msgBytes = audioBuffer.toByteArray();
                ProtoWriter.writeTag(d, 1, 2); // Tag for field 1, wire type 2
                ProtoWriter.writeVarint32(d, msgBytes.length);
                d.write(msgBytes);
            }
            if (audioPartsLength != 0) ProtoWriter.writeInt32(d, 2, audioPartsLength);
            if (fileId != null && !fileId.isEmpty()) ProtoWriter.writeString(d, 3, fileId);
            if (unknown0 != 0) ProtoWriter.writeInt32(d, 4, unknown0);
            return b.toByteArray();
        }
    }

    /**
     * Manual representation of the protobuf message used for the PUT request to the
     * `/video-translation/audio` endpoint, specifically for the YouTube status 6 workaround.
     * Contains the video URL, translation ID, and an audio buffer object with a specific file ID.
     */
    public static class ManualVideoTranslationAudioRequest {
        String translationId;                       // 1, string
        String url;                                 // 2, string
        ManualChunkAudioObject partialAudioInfo;    // 4, message
        ManualAudioBufferObject audioInfo;          // 6, message

        /**
         * Serializes this request object into a protobuf byte array, embedding nested messages.
         *
         * @return The serialized byte array.
         * @throws IOException If an error occurs during serialization.
         */
        byte[] toByteArray() throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            if (translationId != null && !translationId.isEmpty()) ProtoWriter.writeString(d, 1, translationId);
            if (url != null && !url.isEmpty()) ProtoWriter.writeString(d, 2, url);
            if (partialAudioInfo != null) {
                byte[] msgBytes = partialAudioInfo.toByteArray();
                ProtoWriter.writeTag(d, 4, 2); // Tag for field 4, wire type 2
                ProtoWriter.writeVarint32(d, msgBytes.length);
                d.write(msgBytes);
            }
            if (audioInfo != null) {
                byte[] msgBytes = audioInfo.toByteArray();
                ProtoWriter.writeTag(d, 6, 2); // Tag for field 6, wire type 2
                ProtoWriter.writeVarint32(d, msgBytes.length);
                d.write(msgBytes);
            }
            return b.toByteArray();
        }
    }

    /**
     * Utility class containing static methods for writing protobuf data types
     * (varint, string, bytes, double, bool, tags) to a DataOutputStream according
     * to protobuf encoding rules.
     */
    private static class ProtoWriter {

        /**
         * Writes a protobuf field tag (field number and wire type) to the stream as a varint.
         *
         * @param d The DataOutputStream to write to.
         * @param f The field number.
         * @param w The wire type (0-5).
         * @throws IOException If an I/O error occurs.
         */
        static void writeTag(DataOutputStream d, int f, int w) throws IOException {
            writeVarint32(d, (f << 3) | w);
        }

        /**
         * Writes a 32-bit integer to the stream using variable-length encoding (varint).
         * Handles negative numbers correctly by treating the input as unsigned during shifting.
         *
         * @param d The DataOutputStream to write to.
         * @param v The integer value to write.
         * @throws IOException If an I/O error occurs.
         */
        static void writeVarint32(DataOutputStream d, int v) throws IOException {
            // Ensure v is treated as unsigned for encoding, but the loop logic handles signed correctly too
            while (true) {
                if ((v & ~0x7F) == 0) {
                    d.writeByte(v);
                    return;
                } else {
                    d.writeByte((v & 0x7F) | 0x80);
                    v >>>= 7; // Logical right shift to handle negative numbers correctly if they appear
                }
            }
        }

        /**
         * Writes a byte array field to the stream (wire type 2).
         * This includes writing the tag, the length prefix (as varint), and the actual bytes.
         *
         * @param d The DataOutputStream to write to.
         * @param f The field number.
         * @param v The byte array value to write.
         * @throws IOException If an I/O error occurs.
         */
        static void writeBytes(DataOutputStream d, int f, byte[] v) throws IOException {
            writeTag(d, f, 2); // Wire type 2 for length-delimited (bytes, string, embedded messages)
            writeVarint32(d, v.length);
            d.write(v);
        }

        /**
         * Writes a String field to the stream (wire type 2).
         * Converts the String to UTF-8 bytes and calls {@link #writeBytes}.
         *
         * @param d The DataOutputStream to write to.
         * @param f The field number.
         * @param v The String value to write.
         * @throws IOException If an I/O error occurs.
         */
        static void writeString(DataOutputStream d, int f, String v) throws IOException {
            writeBytes(d, f, v.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * Writes a double field to the stream (wire type 1, fixed 64-bit).
         * Writes the tag and the 8-byte little-endian representation of the double.
         *
         * @param d The DataOutputStream to write to.
         * @param v The double value to write.
         * @throws IOException If an I/O error occurs.
         */
        static void writeDouble(DataOutputStream d, double v) throws IOException {
            writeTag(d, 6, 1); // Wire type 1 for 64-bit fixed size (double, fixed64)
            d.writeLong(Long.reverseBytes(Double.doubleToRawLongBits(v)));
        }

        /**
         * Writes an int32 field to the stream (wire type 0, varint).
         * Writes the tag and the integer value using varint encoding.
         *
         * @param d The DataOutputStream to write to.
         * @param f The field number.
         * @param v The integer value to write.
         * @throws IOException If an I/O error occurs.
         */
        static void writeInt32(DataOutputStream d, int f, int v) throws IOException {
            writeTag(d, f, 0); // Wire type 0 for varint (int32, int64, uint32, uint64, sint32, sint64, bool, enum)
            writeVarint32(d, v);
        }

        /**
         * Writes a boolean field to the stream (wire type 0, varint).
         * Writes the tag and a single byte (1 for true, 0 for false).
         *
         * @param d The DataOutputStream to write to.
         * @param f The field number.
         * @param v The boolean value to write.
         * @throws IOException If an I/O error occurs.
         */
        static void writeBool(DataOutputStream d, int f, boolean v) throws IOException {
            writeTag(d, f, 0); // Wire type 0 for bool
            d.writeByte(v ? 1 : 0);
        }
    }

    /**
     * Utility class containing static methods for reading protobuf data types
     * (varint, string, bytes, double, bool) from a DataInputStream and skipping fields
     * according to protobuf encoding rules and wire types. Includes basic safety checks.
     */
    private static class ProtoReader {
        private static final int MAX_VARINT_BYTES = 10; // Max bytes for a 64-bit varint
        private static final int MAX_LENGTH_LIMIT = 50 * 1024 * 1024; // e.g., 50MB limit for strings/bytes/messages

        /**
         * Reads a 32-bit integer encoded as a varint from the stream.
         * Protects against malformed or overly long varints.
         *
         * @param d The DataInputStream to read from.
         * @return The decoded 32-bit integer value.
         * @throws IOException If an I/O error occurs, the varint is malformed, too long,
         *                     or represents a value larger than 32 bits.
         */
        static int readVarint32(DataInputStream d) throws IOException {
            int result = 0;
            int shift = 0;
            for (int i = 0; i < MAX_VARINT_BYTES; i++) { // Use MAX_VARINT_BYTES for safety
                byte b = d.readByte();
                result |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) return result;
                shift += 7;
                // Check for overflow for 32-bit result
                if (shift >= 32 && (b & 0x80) != 0) {
                    // If we read > 5 bytes and the last one had MSB set, it's an overflow for a 32-bit int.
                    // Consume remaining bytes of the varint to keep stream position correct.
                    for (int j = i + 1; j < MAX_VARINT_BYTES; j++) {
                        byte nextByte = d.readByte();
                        if ((nextByte & 0x80) == 0) break; // Stop consuming if MSB is 0
                    }
                    throw new IOException("Varint32 too large/malformed");
                }
            }
            throw new IOException("Varint too long"); // Should not happen if MAX_VARINT_BYTES is sufficient (10 for 64-bit)
        }

        /**
         * Reads a length-prefixed byte array from the stream (wire type 2).
         * Reads the length (as varint) and then the specified number of bytes.
         * Protects against excessively large lengths.
         *
         * @param d The DataInputStream to read from.
         * @return The byte array read from the stream.
         * @throws IOException If an I/O error occurs, the length prefix is invalid,
         *                     the length exceeds {@link #MAX_LENGTH_LIMIT}, or not enough bytes are available.
         */
        static byte[] readBytes(DataInputStream d) throws IOException {
            int l = readVarint32(d); // Length prefix (varint)
            if (l < 0) throw new IOException("Negative length: " + l);
            if (l > MAX_LENGTH_LIMIT) throw new IOException("Length too long: " + l + " > " + MAX_LENGTH_LIMIT);
            if (l > d.available()) throw new IOException("Length exceeds available bytes: " + l + " > " + d.available());
            byte[] dt = new byte[l];
            d.readFully(dt); // Read the exact number of bytes
            return dt;
        }

        /**
         * Reads a length-prefixed String from the stream (wire type 2).
         * Reads the bytes using {@link #readBytes} and decodes them as UTF-8.
         *
         * @param d The DataInputStream to read from.
         * @return The String read from the stream.
         * @throws IOException If an I/O error occurs during reading or decoding.
         */
        static String readString(DataInputStream d) throws IOException {
            return new String(readBytes(d), StandardCharsets.UTF_8);
        }

        /**
         * Reads a double value from the stream (wire type 1, fixed 64-bit).
         * Reads 8 bytes and interprets them as a little-endian double.
         *
         * @param d The DataInputStream to read from.
         * @return The double value read from the stream.
         * @throws IOException If an I/O error occurs or not enough bytes are available.
         */
        static double readDouble(DataInputStream d) throws IOException {
            // Doubles are encoded as 64-bit little-endian, but DataInputStream reads big-endian.
            // Need to reverse the bytes.
            return Double.longBitsToDouble(Long.reverseBytes(d.readLong()));
        }

        /**
         * Reads a boolean value from the stream (wire type 0, varint).
         * Reads a varint and returns true if it's non-zero, false otherwise.
         *
         * @param d The DataInputStream to read from.
         * @return The boolean value read from the stream.
         * @throws IOException If an I/O error occurs during reading the varint.
         */
        static boolean readBool(DataInputStream d) throws IOException {
            // Booleans are encoded as 0 or 1 varint. Read as varint and check if non-zero.
            return readVarint32(d) != 0;
        }

        /**
         * Skips over a field in the protobuf stream based on its tag (which includes the wire type).
         * Reads and discards the appropriate number of bytes for the given wire type.
         * Handles varint, fixed32, fixed64, and length-delimited types.
         * Includes basic handling for deprecated group types (wire types 3 and 4).
         *
         * @param d The DataInputStream to read from.
         * @param t The tag value (field number << 3 | wire type) of the field to skip.
         * @throws IOException If an I/O error occurs, data is truncated, an unknown wire type
         *                     is encountered, or a malformed structure (like mismatched groups) is found.
         */
        static void skipField(DataInputStream d, int t) throws IOException {
            int w = t & 7; // wire type
            switch (w) {
                case 0: // Varint
                    // Skip byte by byte until MSB is 0
                    int bytesSkipped = 0;
                    do {
                        if (d.available() <= 0) throw new EOFException("Unexpected end of data while skipping varint");
                        if (bytesSkipped++ > MAX_VARINT_BYTES) throw new IOException("Varint skip too long");
                    } while ((d.readByte() & 0x80) != 0);
                    break;
                case 1: // 64-bit fixed size (fixed64, sfixed64, double)
                    if (d.available() < 8) throw new EOFException("Unexpected end of data while skipping 64-bit");
                    d.skipBytes(8);
                    break;
                case 2: // Length-delimited (string, bytes, embedded messages, packed repeated fields)
                    int l = readVarint32(d); // Read the length prefix
                    if (l < 0) throw new IOException("Negative length skip: " + l);
                    if (l > MAX_LENGTH_LIMIT) throw new IOException("Length skip too long: " + l); // Check against max limit
                    if (l > d.available()) throw new EOFException("Length skip exceeds available bytes: " + l + " > " + d.available());
                    d.skipBytes(l); // Skip the payload bytes
                    break;
                case 5: // 32-bit fixed size (fixed32, sfixed32, float)
                    if (d.available() < 4) throw new EOFException("Unexpected end of data while skipping 32-bit");
                    d.skipBytes(4);
                    break;
                case 3: // Start group (deprecated)
                    Logger.printInfo(() -> "VOT: Skipping StartGroup (WireType 3) field " + (t >>> 3) + ". This is not fully robust.");
                    int fieldNo = t >>> 3;
                    int depth = 1; // Start at depth 1 for the current group
                    while (depth > 0) {
                        if (d.available() <= 0) throw new EOFException("Unexpected end of data while skipping group");
                        int nextTag = ProtoReader.readVarint32(d);
                        int nextWireType = nextTag & 7;
                        int nextFieldNo = nextTag >>> 3;

                        if (nextWireType == 4) { // EndGroup
                            if (nextFieldNo != fieldNo) {
                                throw new IOException("Mismatched EndGroup tag (" + nextFieldNo + ") while skipping group " + fieldNo);
                            }
                            depth--;
                        } else if (nextWireType == 3) { // StartGroup
                            // Recurse or manage depth to skip nested groups
                            depth++;
                            // We need to know the field number of the *nested* group to skip it properly,
                            // which makes simple depth counting insufficient for robust skipping.
                            // For now, we just increase depth and rely on skipField for inner content.
                            // A more robust skip would require recursive calls or tracking field numbers at each depth.
                            // However, since groups are deprecated, this basic depth approach might suffice.
                            // Let's assume for now we just need to find the matching EndGroup tag.
                        } else {
                            // Skip the field inside the group
                            skipField(d, nextTag);
                        }
                    }
                    break;

                case 4: // End group (deprecated)
                    // This should not normally be encountered directly when skipping *a field*,
                    // as it's expected *within* the skip logic for wire type 3 (StartGroup).
                    throw new IOException("Unexpected EndGroup tag encountered during skip for tag " + t);

                default:
                    throw new IOException("Cannot skip unknown wire type " + w + " for tag " + t);
            }
        }
    }

    // endregion Protobuf Manual Classes

}
