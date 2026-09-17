/*
 * Copyright (C) 2025-2026 anddea
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

package app.morphe.extension.youtube.utils;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.youtube.shared.VideoInformation.getVideoTitle;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.patches.voiceovertranslation.VotApiClient;
import app.morphe.extension.youtube.patches.voiceovertranslation.VotProtobuf;
import app.morphe.extension.youtube.patches.voiceovertranslation.VotProtobuf.SubtitleTrack;
import app.morphe.extension.youtube.patches.voiceovertranslation.VotProtobuf.SubtitlesResponse;
import app.morphe.extension.youtube.patches.voiceovertranslation.VotAudioUploadState;

import static app.morphe.extension.youtube.patches.voiceovertranslation.VotApiClient.STATUS_FAILED;
import static app.morphe.extension.youtube.patches.voiceovertranslation.VotApiClient.STATUS_FINISHED;
import static app.morphe.extension.youtube.patches.voiceovertranslation.VotApiClient.STATUS_WAITING;
import static app.morphe.extension.youtube.patches.voiceovertranslation.VotApiClient.STATUS_LONG_WAITING;
import static app.morphe.extension.youtube.patches.voiceovertranslation.VotApiClient.STATUS_PART_CONTENT;
import static app.morphe.extension.youtube.patches.voiceovertranslation.VotApiClient.STATUS_AUDIO_REQUESTED;
import static app.morphe.extension.youtube.patches.voiceovertranslation.VotApiClient.STATUS_SESSION_REQUIRED;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Utility class for interacting with Yandex API to fetch and process video subtitles.
 * Translation, authentication, transport and audio uploads are delegated to VOT.
 * Subtitle polling stays here because subtitle readiness does not require an audio URL.
 */
public class YandexVotUtils {
    // --- Constants ---
    private static final int MIN_POLLING_INTERVAL_MS = 5000;  // Poll at least every 5 seconds
    private static final int MAX_POLLING_INTERVAL_MS = 60000; // Poll at most every 60 seconds
    private static final int POLLING_TIME_BUFFER_MS = 2000; // Add 2-second buffer to remainingTime
    private static final long WORKFLOW_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes total timeout

    private static final int MAX_STUCK_POLLS = 3;
    private static final String YANDEX_ERROR_SERVER_TRY_AGAIN = "Возникла ошибка при переводе, попробуйте позже";

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();
    private static final Map<String, AtomicBoolean> urlCancellationFlags = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> urlWorkflowLocks = new ConcurrentHashMap<>();
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private static final ExecutorService workflowExecutor = Executors.newCachedThreadPool();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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
            if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled before start");

            Logger.printInfo(() -> "VOT: Step 2/3 - Checking existing subtitles for lang: " + state.yandexTargetLang);
            if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled during subtitle check");
            SubtitlesResponse subsResponse = getFinalSubtitleTracks(state.videoUrl);
            if (subsResponse != null && !subsResponse.waiting) {
                SubtitleTrack chosenSub = findBestSubtitleForLanguage(subsResponse.subtitles, state.yandexTargetLang);
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
            String userMessage = (e instanceof IOException)
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

                VotApiClient.TranslationResult transResponse = VotApiClient.requestTranslation(
                        state.videoUrl, state.durationSeconds, "auto", state.yandexTargetLang,
                        state.videoTitle, state.firstRequest);
                state.firstRequest = false;
                if (transResponse == null) {
                    scheduler.schedule(() -> pollForTranslation(state), 5, TimeUnit.SECONDS);
                    return;
                }

                Logger.printInfo(() -> "VOT: Poll - Status: " + transResponse.status() +
                        ", RemainingTime: " + transResponse.remainingTime() + "s, Message: " + transResponse.message());

                switch (transResponse.status()) {
                    case STATUS_FINISHED:
                        Logger.printInfo(() -> "VOT: Translation completed for " + state.yandexTargetLang);
                        if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled");
                        SubtitlesResponse subsResponse = getFinalSubtitleTracks(state.videoUrl);
                        // Subtitle generation may finish after the translation result is cached.
                        if (subsResponse == null || subsResponse.waiting) {
                            scheduler.schedule(() -> pollForTranslation(state), 5, TimeUnit.SECONDS);
                            return;
                        }
                        processAndFetchFinalSubtitles(subsResponse, state.originalTargetLang, state.yandexTargetLang, state.callback);
                        return; // Workflow complete

                    case STATUS_AUDIO_REQUESTED:
                        Logger.printInfo(() -> "VOT: Handling STATUS_AUDIO_REQUESTED for YouTube");
                        postToMainThread(() -> state.callback.onProcessingStarted(str("revanced_yandex_status_youtube_specific")));
                        if (state.isCancelled.get()) throw new InterruptedException("Workflow cancelled");
                        state.audioUploadState.upload(state.videoId, state.videoUrl, transResponse.translationId());
                        // Schedule next poll after a short delay
                        scheduler.schedule(() -> pollForTranslation(state), 1, TimeUnit.SECONDS);
                        break;

                    case STATUS_WAITING:
                    case STATUS_PART_CONTENT:
                    case STATUS_LONG_WAITING:
                        if (!state.isStuck) {
                            if (transResponse.remainingTime() > 0 && transResponse.remainingTime() == state.lastRemainingTime) {
                                state.stuckPollCount++;
                            } else {
                                state.stuckPollCount = 0; // Reset counter if time changes
                            }
                            state.lastRemainingTime = transResponse.remainingTime();

                            if (state.stuckPollCount >= MAX_STUCK_POLLS) {
                                Logger.printInfo(() -> "VOT: Poll is now considered stuck. Setting persistent delayed state.");
                                state.isStuck = true;
                            }
                        }

                        if (state.isStuck) {
                            postToMainThread(() -> state.callback.onProcessingStarted(str("revanced_yandex_status_transcription_delayed")));
                        } else {
                            String waitMsg = secsToStrTime(transResponse.remainingTime());
                            postToMainThread(() -> state.callback.onProcessingStarted(waitMsg));
                        }

                        long delayMs = calculateSleepTime(transResponse.remainingTime());
                        Logger.printDebug(() -> "VOT: Scheduling next poll in " + (delayMs / 1000.0) + "s");
                        scheduler.schedule(() -> pollForTranslation(state), delayMs, TimeUnit.MILLISECONDS);
                        break;

                    case STATUS_SESSION_REQUIRED:
                        throw new IOException(str("revanced_vot_auth_required"));

                    case STATUS_FAILED:
                    default:
                        String errMsg = str("revanced_yandex_error_translation_failed") +
                                (TextUtils.isEmpty(transResponse.message()) ? "" : ": " + translateServerMessage(transResponse.message()));
                        throw new IOException(errMsg);
                }
            } catch (InterruptedException e) {
                Logger.printInfo(() -> "VOT: Polling cancelled for " + state.videoUrl);
                postToMainThread(() -> state.callback.onFinalFailure(str("revanced_gemini_cancelled")));
            } catch (Exception e) {
                Logger.printException(() -> "VOT: Polling failed: " + e.getMessage(), e);
                String userMessage = (e instanceof IOException)
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

        // Queue once. Subsequent calls only check the existing translation.
        boolean firstRequest = true;
        final VotAudioUploadState audioUploadState = new VotAudioUploadState();
        final String videoId;
        final String videoTitle = getVideoTitle();
        int lastRemainingTime = -1;
        int stuckPollCount = 0;
        boolean isStuck = false;

        WorkflowState(String videoUrl, double durationSeconds, String originalTargetLang, String yandexTargetLang, SubtitleWorkflowCallback callback, AtomicBoolean isCancelled) {
            this.videoUrl = videoUrl;
            android.net.Uri uri = android.net.Uri.parse(videoUrl);
            this.videoId = "youtu.be".equalsIgnoreCase(uri.getHost())
                    ? uri.getLastPathSegment() : uri.getQueryParameter("v");
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

    /** Fetches subtitle tracks using VOT's shared transport and protobuf codec. */
    private static SubtitlesResponse getFinalSubtitleTracks(String videoUrl) throws IOException {
        byte[] response = VotApiClient.requestSubtitles(
                VotProtobuf.encodeSubtitlesRequest(videoUrl, "auto"));
        return response == null ? null : VotProtobuf.decodeSubtitlesResponse(response);
    }

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
     * @param response           The {@link SubtitlesResponse} from the API.
     * @param originalTargetLang The user's desired language.
     * @param yandexTargetLang   The Yandex requested language.
     * @param callback           The callback for results or errors.
     */
    private static void processAndFetchFinalSubtitles(
            @Nullable SubtitlesResponse response,
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

        List<SubtitleTrack> availableSubs = response.subtitles;
        if (availableSubs.isEmpty()) {
            Logger.printInfo(() -> "VOT: No subtitle tracks returned");
            postToMainThread(() -> callback.onFinalFailure(str("revanced_yandex_error_no_subs_returned")));
            return;
        }

        String log = availableSubs.stream()
                .map(s -> "{Orig:" + s.language + (TextUtils.isEmpty(s.url) ? "(X)" : "")
                        + (s.translatedLanguage != null ? ",Trans:" + s.translatedLanguage + (TextUtils.isEmpty(s.translatedUrl) ? "(X)" : "") : "") + "}")
                .collect(Collectors.joining(", "));
        Logger.printInfo(() -> "VOT: Processing tracks - OriginalTarget: " + originalTargetLang + ", YandexTarget: " + yandexTargetLang + ", Available: [" + log + "]");

        SubtitleTrack chosenSub = findBestSubtitleForLanguage(availableSubs, yandexTargetLang);
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
     * @return The best matching {@link SubtitleTrack} or null.
     */
    @Nullable
    private static SubtitleTrack findBestSubtitleForLanguage(List<SubtitleTrack> subs, String targetLang) {
        if (subs == null || subs.isEmpty()) return null;

        SubtitleTrack translatedMatch = null;
        for (SubtitleTrack sub : subs) {
            if (targetLang.equals(sub.translatedLanguage) && !TextUtils.isEmpty(sub.translatedUrl)) {
                translatedMatch = sub;
                break;
            }
        }

        if (translatedMatch != null) {
            Logger.printInfo(() -> "VOT: Selected translated track for " + targetLang);
            return translatedMatch;
        }

        for (SubtitleTrack sub : subs) {
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
    private static String determineSubtitleUrl(@NonNull SubtitleTrack chosenSub, @NonNull String targetLang) {
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
        if (map.isEmpty() && subsArray.length() > 0) {
            return null;
        }
        return splitLongSubtitleEntries(map);
    }

    private static final double SUBTITLE_TARGET_MAX_WEIGHT = 70.0;
    private static final long SUBTITLE_MIN_CHUNK_DURATION_MS = 300L;

    /**
     * Splits long subtitle entries into smaller word/character chunks with interpolated timestamps
     * to prevent long sentences from blocking the video player UI.
     *
     * @param map The original parsed subtitle map.
     * @return A new {@link TreeMap} containing split subtitle entries.
     */
    @Nullable
    public static TreeMap<Long, Pair<Long, String>> splitLongSubtitleEntries(@Nullable TreeMap<Long, Pair<Long, String>> map) {
        if (map == null || map.isEmpty()) {
            return map;
        }

        TreeMap<Long, Pair<Long, String>> result = new TreeMap<>();
        for (Map.Entry<Long, Pair<Long, String>> entry : map.entrySet()) {
            long startMs = entry.getKey();
            Pair<Long, String> pair = entry.getValue();
            if (pair == null) continue;

            long endMs = pair.first;
            String text = pair.second;

            if (TextUtils.isEmpty(text) || endMs <= startMs) {
                putUniqueSubtitleEntry(result, startMs, endMs, text);
                continue;
            }

            // Normalize spaces/newlines in subtitle text
            String normalizedText = text.replaceAll("\\r?\\n", " ").trim();
            if (getSubtitleTextWeight(normalizedText) <= SUBTITLE_TARGET_MAX_WEIGHT) {
                putUniqueSubtitleEntry(result, startMs, endMs, normalizedText);
                continue;
            }

            List<String> chunks = chunkSubtitleText(normalizedText);
            if (chunks.isEmpty()) {
                putUniqueSubtitleEntry(result, startMs, endMs, normalizedText);
                continue;
            }

            int n = chunks.size();
            if (n == 1) {
                putUniqueSubtitleEntry(result, startMs, endMs, chunks.get(0));
                continue;
            }

            double totalWeight = 0;
            for (String chunk : chunks) {
                totalWeight += getSubtitleTextWeight(chunk);
            }
            if (totalWeight <= 0) {
                totalWeight = 1.0;
            }

            long totalDuration = endMs - startMs;
            long currentStart = startMs;

            for (int i = 0; i < n; i++) {
                String chunkText = chunks.get(i);
                long chunkEnd;
                if (i == n - 1) {
                    chunkEnd = endMs;
                } else {
                    double ratio = getSubtitleTextWeight(chunkText) / totalWeight;
                    long duration = Math.max(SUBTITLE_MIN_CHUNK_DURATION_MS, Math.round(totalDuration * ratio));
                    long maxAllowedEnd = endMs - (n - 1 - i) * SUBTITLE_MIN_CHUNK_DURATION_MS;
                    chunkEnd = currentStart + duration;
                    if (chunkEnd > maxAllowedEnd) {
                        chunkEnd = maxAllowedEnd;
                    }
                }

                if (chunkEnd <= currentStart) {
                    chunkEnd = currentStart + 1;
                }

                putUniqueSubtitleEntry(result, currentStart, chunkEnd, chunkText);
                currentStart = chunkEnd;
            }
        }

        return result.isEmpty() ? null : result;
    }

    private static void putUniqueSubtitleEntry(TreeMap<Long, Pair<Long, String>> map, long startMs, long endMs, String text) {
        long key = startMs;
        while (map.containsKey(key)) {
            key++;
        }
        map.put(key, new Pair<>(Math.max(key + 1, endMs), text));
    }

    private static double getSubtitleTextWeight(String str) {
        if (str == null || str.isEmpty()) return 0.0;
        double weight = 0;
        int len = str.length();
        for (int i = 0; i < len; ) {
            int cp = str.codePointAt(i);
            weight += isCjkOrFullWidth(cp) ? 2.0 : 1.0;
            i += Character.charCount(cp);
        }
        return weight;
    }

    private static boolean isCjkOrFullWidth(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    private static List<String> chunkSubtitleText(String text) {
        List<String> chunks = new ArrayList<>();
        if (TextUtils.isEmpty(text)) return chunks;

        boolean hasSpace = text.contains(" ");
        List<String> tokens = new ArrayList<>();

        if (hasSpace) {
            String[] parts = text.split(" ");
            for (String p : parts) {
                if (!p.isEmpty()) tokens.add(p);
            }
        } else {
            StringBuilder currentToken = new StringBuilder();
            int len = text.length();
            for (int i = 0; i < len; ) {
                int cp = text.codePointAt(i);
                int charCount = Character.charCount(cp);
                currentToken.appendCodePoint(cp);

                if (isPunctuation(cp) || getSubtitleTextWeight(currentToken.toString()) >= 10.0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                i += charCount;
            }
            if (currentToken.length() > 0) {
                tokens.add(currentToken.toString());
            }
        }

        StringBuilder currentChunk = new StringBuilder();
        double currentWeight = 0;

        for (String token : tokens) {
            double tokenWeight = getSubtitleTextWeight(token);
            String separator = (hasSpace && currentChunk.length() > 0) ? " " : "";
            double additionWeight = tokenWeight + getSubtitleTextWeight(separator);

            if (currentChunk.length() > 0 && (currentWeight + additionWeight > SUBTITLE_TARGET_MAX_WEIGHT)) {
                chunks.add(currentChunk.toString());
                currentChunk.setLength(0);
                currentWeight = 0;
            }

            if (currentChunk.length() > 0 && hasSpace) {
                currentChunk.append(" ");
                currentWeight += getSubtitleTextWeight(" ");
            }

            if (tokenWeight > SUBTITLE_TARGET_MAX_WEIGHT) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                    currentWeight = 0;
                }
                int tLen = token.length();
                StringBuilder subToken = new StringBuilder();
                double subWeight = 0;
                for (int i = 0; i < tLen; ) {
                    int cp = token.codePointAt(i);
                    int charCount = Character.charCount(cp);
                    double w = isCjkOrFullWidth(cp) ? 2.0 : 1.0;
                    if (subWeight + w > SUBTITLE_TARGET_MAX_WEIGHT && subToken.length() > 0) {
                        chunks.add(subToken.toString());
                        subToken.setLength(0);
                        subWeight = 0;
                    }
                    subToken.appendCodePoint(cp);
                    subWeight += w;
                    i += charCount;
                }
                if (subToken.length() > 0) {
                    currentChunk.append(subToken);
                    currentWeight += subWeight;
                }
            } else {
                currentChunk.append(token);
                currentWeight += tokenWeight;
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private static boolean isPunctuation(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.CONNECTOR_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION;
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

    // endregion Interfaces and Inner Classes

}
