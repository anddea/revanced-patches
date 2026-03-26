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

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.morphe.extension.shared.settings.AppLanguage;
import app.morphe.extension.shared.ui.SheetBottomDialog;
import app.morphe.extension.shared.utils.IntentUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.showToastLong;
import static app.morphe.extension.shared.utils.Utils.showToastShort;

/**
 * Manages Gemini API operations (Summarization, Transcription) and Yandex VOT Transcription.
 * Handles UI display (progress dialogs, results, subtitle overlay), caching, and state management.
 * This class is designed as a singleton to maintain a consistent state across the application.
 * <p>
 * It supports simultaneous operations and persists results for all videos until the app is restarted.
 */
@SuppressWarnings("RegExpRedundantEscape")
public final class GeminiManager {
    /**
     * Pattern to match transcription timestamps and text.
     * Matches [ HH:MM:SS.ms - HH:MM:SS.ms ]: TEXT and dot/colon/comma separator for milliseconds.
     */
    private static final Pattern TRANSCRIPTION_PATTERN = Pattern.compile(
            "\\[\\s*(?:(\\d{2}):)?(\\d{2}):(\\d{2})[.,:](\\d{1,3})\\s*-\\s*(?:(\\d{2}):)?(\\d{2}):(\\d{2})[.,:](\\d{1,3})\\s*\\]:?\\s*(.*)"
    );
    /**
     * Pattern to match summary timestamps (HH:MM:SS or MM:SS), with optional milliseconds.
     */
    private static final Pattern SUMMARY_TIMESTAMP_PATTERN = Pattern.compile(
            "(?<!\\d)(?:(\\d{1,2}):)?(\\d{2}):(\\d{2})(?:[.,:](\\d{1,3}))?(?!\\d)"
    );
    private static final String VIDEO_SCHEME_INTENT_FORMAT = "vnd.youtube://%s?start=%d";

    /**
     * Pattern to extract video ID from YouTube URLs.
     */
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("(?<=v=|/)([0-9A-Za-z_-]{11})(?![0-9A-Za-z_-])");

    /**
     * Interval for updating subtitles in milliseconds.
     */
    private static final long SUBTITLE_UPDATE_INTERVAL_MS = 250;

    /**
     * Placeholder text for empty subtitles.
     */
    private static final String EMPTY_SUBTITLE_PLACEHOLDER = "...";

    /**
     * Key for using app language in Yandex settings.
     */
    private static final String APP_LANGUAGE_SETTING_KEY = "app";

    /**
     * A placeholder Future used when the actual task Future is not available
     * (e.g. implicitly managed by a utility class) but we still need to track active state in ConcurrentHashMap.
     */
    private static final Future<?> DUMMY_FUTURE = new Future<>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    };

    /**
     * Singleton instance of GeminiManager.
     */
    private static volatile GeminiManager instance;

    /**
     * Handler for progress dialog timer.
     */
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    /**
     * Handler for subtitle overlay updates.
     */
    private final Handler subtitleUpdateHandler = new Handler(Looper.getMainLooper());

    /**
     * Handler for posting to the main thread.
     */
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Executor for fetching metadata in background.
     */
    private final ExecutorService metadataExecutor = Executors.newCachedThreadPool();

    /**
     * Active loading bottom sheet for summarize/transcribe operations.
     */
    @Nullable
    private GeminiBottomSheetUi.LoadingSheet loadingSheet;

    /**
     * Active summary bottom sheet used after summarization completes.
     */
    @Nullable
    private GeminiBottomSheetUi.SummarySheet summarySheet;

    /**
     * Active transcription bottom sheet used after Gemini transcription completes.
     */
    @Nullable
    private WeakReference<SheetBottomDialog.SlideDialog> transcriptionDialogRef;

    /**
     * Active follow-up chat task inside the summary bottom sheet.
     */
    @Nullable
    private Future<?> activeSummaryChatTask;

    /**
     * Flag indicating if the progress dialog is minimized.
     */
    private boolean isProgressDialogMinimized = false;

    /**
     * Flag indicating if the operation is cancelled.
     */
    private volatile boolean isCancelled = false;

    /**
     * Current operation type.
     */
    private volatile OperationType currentOperation = OperationType.NONE;

    /**
     * Current video URL.
     */
    @Nullable
    private volatile String currentVideoUrl = null;

    /**
     * Map of task keys to start times.
     */
    private final Map<String, Long> taskStartTimes = new ConcurrentHashMap<>();

    /**
     * Base loading message for the progress dialog.
     */
    @Nullable
    private String baseLoadingMessage;

    /**
     * Flag indicating if waiting for Yandex retry.
     */
    private volatile boolean isWaitingForYandexRetry = false;

    /**
     * Last Yandex status message.
     */
    @Nullable
    private String lastYandexStatusMessage = null;

    /**
     * Set of active tasks mapped to their Future (if applicable) or a placeholder object.
     * This allows us to interrupt specific background threads.
     * Key: TaskKey (VideoID + OperationType)
     * Value: Future object (can be null for pure async callbacks that don't expose Future, like Yandex wrapper)
     */
    private final Map<String, Future<?>> activeTasks = new ConcurrentHashMap<>();

    /**
     * Determined target language code for transcription.
     */
    @Nullable
    private volatile String determinedTargetLanguageCode = null;

    /**
     * Intermediate language code if needed.
     */
    @Nullable
    private volatile String intermediateLanguageCode = null;

    /**
     * Cache for video summaries.
     */
    private final Map<String, String> summaryCache = new ConcurrentHashMap<>();

    /**
     * Cache for summary times.
     */
    private final Map<String, Integer> summaryTimeCache = new ConcurrentHashMap<>();

    /**
     * Cache for summary generation model names.
     */
    private final Map<String, String> summaryModelCache = new ConcurrentHashMap<>();

    /**
     * Cache for per-video summary follow-up chat history.
     */
    private final Map<String, List<GeminiUtils.ChatMessage>> summaryConversationCache = new ConcurrentHashMap<>();

    /**
     * Cache for parsed transcriptions.
     */
    private final Map<String, TreeMap<Long, Pair<Long, String>>> transcriptionCache = new ConcurrentHashMap<>();

    /**
     * Cache for raw transcriptions.
     */
    private final Map<String, String> rawTranscriptionCache = new ConcurrentHashMap<>();

    /**
     * Cache for transcription times.
     */
    private final Map<String, Integer> transcriptionTimeCache = new ConcurrentHashMap<>();

    /**
     * Cache for video metadata.
     */
    private final Map<String, String> videoMetadataCache = new ConcurrentHashMap<>();

    /**
     * Reference to the subtitle overlay.
     */
    @Nullable
    private WeakReference<SubtitleOverlay> subtitleOverlayRef;

    /**
     * Flag indicating if the subtitle overlay is showing.
     */
    private volatile boolean isSubtitleOverlayShowing = false;

    /**
     * Runnable for the timer.
     */
    @Nullable
    private Runnable timerRunnable;

    /**
     * Runnable for subtitle updates.
     */
    @Nullable
    private Runnable subtitleUpdateRunnable;

    /**
     * Private constructor for the Singleton pattern.
     */
    private GeminiManager() {}

    /**
     * Returns the singleton instance of GeminiManager.
     *
     * @return The singleton instance.
     */
    public static GeminiManager getInstance() {
        if (instance == null) {
            synchronized (GeminiManager.class) {
                if (instance == null) {
                    instance = new GeminiManager();
                }
            }
        }
        return instance;
    }

    // region Public API Methods

    /**
     * Copies the given text to the system clipboard and displays a toast message.
     *
     * @param context      The application context, used to access the {@link ClipboardManager}. Must not be null.
     * @param text         The string text to be copied to the clipboard.
     * @param toastMessage The message to display in a short toast notification upon successful copying.
     *                     This message should ideally be informative to the user (e.g., "Text copied!").
     */
    private static void setClipboard(Context context, String text, String toastMessage) {
        if (context == null) {
            Logger.printException(() -> "Context null for clipboard");
            return;
        }
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", text);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                showToastShort(toastMessage);
            } else {
                showToastShort(str("revanced_copy_error"));
            }
        } catch (Exception e) {
            showToastShort(str("revanced_copy_error"));
        }
    }

    /**
     * Initiates the video summarization workflow using the Gemini API.
     * <p>
     * Checks cache first. If not cached, check if busy with the *same* task.
     * If not busy, starts the new operation. Previous operations run in background.
     *
     * @param context  Android Context, needed for UI operations (Dialogs, Toasts). Must be a UI context.
     * @param videoUrl The URL of the video to summarize. Must be a valid, non-placeholder URL.
     */
    public void startSummarization(@NonNull Context context, @NonNull final String videoUrl) {
        ensureMainThread(() -> {
            hideTranscriptionOverlayInternal();

            String videoId = getVideoIdFromUrl(videoUrl);
            captureVideoMetadata(videoId);

            if (summaryCache.containsKey(videoId)) {
                Logger.printDebug(() -> "Displaying cached summary for ID: " + videoId);
                String result = summaryCache.get(videoId);
                Integer time = summaryTimeCache.getOrDefault(videoId, -1);
                String model = summaryModelCache.get(videoId);
                showSummaryDialog(context, Objects.requireNonNull(result), Objects.requireNonNull(time), videoUrl, model);
                resetOperationStateInternal(OperationType.SUMMARIZE, false);
                return;
            }

            String taskKey = getTaskKey(videoId, OperationType.SUMMARIZE);

            if (activeTasks.containsKey(taskKey)) {
                Logger.printDebug(() -> "Task already active for " + videoId);
                prepareForNewOperationInternal(OperationType.SUMMARIZE, videoUrl);
                showProgressDialogInternal(context, OperationType.SUMMARIZE);
                return;
            }

            prepareForNewOperationInternal(OperationType.SUMMARIZE, videoUrl);

            final List<String> apiKeys = GeminiUtils.parseApiKeys(Settings.GEMINI_API_KEY.get());
            if (isEmptyApiKey(apiKeys)) {
                resetOperationStateInternal(OperationType.SUMMARIZE, true);
                return;
            }

            Logger.printDebug(() -> "Starting new summarization workflow: " + videoId);
            taskStartTimes.put(taskKey, System.currentTimeMillis());

            showProgressDialogInternal(context, OperationType.SUMMARIZE);

            Future<?> summaryTask = GeminiUtils.getVideoSummary(videoUrl, apiKeys, new GeminiUtils.Callback() {
                @Override
                public void onPartial(String partialText, String accumulatedText, @Nullable String model) {
                    if (!activeTasks.containsKey(taskKey)) {
                        Logger.printDebug(() -> "Summary partial callback ignored - task was cancelled: " + taskKey);
                        return;
                    }
                    ensureMainThread(() -> handleSummaryStreamUpdate(videoUrl, accumulatedText));
                }

                @Override
                public void onSuccess(String result) {
                    onSuccessWithModel(result, null);
                }

                @Override
                public void onSuccessWithModel(String result, String model) {
                    if (!activeTasks.containsKey(taskKey)) {
                        Logger.printDebug(() -> "Summary success callback ignored - task was cancelled: " + taskKey);
                        return;
                    }
                    activeTasks.remove(taskKey);
                    handleApiResponseInternal(context, OperationType.SUMMARIZE, videoUrl, result, null, model);
                }

                @Override
                public void onFailure(String error) {
                    if (!activeTasks.containsKey(taskKey)) {
                        Logger.printDebug(() -> "Summary failure callback ignored - task was cancelled: " + taskKey);
                        return;
                    }
                    activeTasks.remove(taskKey);
                    handleApiResponseInternal(context, OperationType.SUMMARIZE, videoUrl, null, error);
                }
            });
            activeTasks.put(taskKey, summaryTask != null ? summaryTask : DUMMY_FUTURE);
        });
    }

    // endregion Public API Methods

    // region Internal State Management & Workflow Logic

    /**
     * Initiates the video transcription workflow using either Yandex VOT or Gemini API.
     * Checks cache first. If not cached, starts new operation.
     *
     * @param context  Android Context, needed for UI operations. Must be a UI context.
     * @param videoUrl The URL of the video to transcribe. Must be a valid, non-placeholder URL.
     */
    public void startTranscription(@NonNull Context context, @NonNull final String videoUrl) {
        ensureMainThread(() -> {
            final long videoLengthMs = VideoInformation.getVideoLength();
            final double durationSeconds = videoLengthMs > 0 ? videoLengthMs / 1000.0 : 0;

            hideTranscriptionOverlayInternal();

            String videoId = getVideoIdFromUrl(videoUrl);
            captureVideoMetadata(videoId);

            // region Cache check
            boolean cacheDisplayed = false;
            // Check for previously parsed result (could be from Yandex direct or Yandex+Gemini)
            if (transcriptionCache.containsKey(videoId)) {
                Logger.printDebug(() -> "Attempting display cached transcription overlay for ID: " + videoId);
                if (displayTranscriptionOverlayInternal(videoUrl)) {
                    Logger.printDebug(() -> "Cached transcription overlay display succeeded.");
                    showToastShort(str("revanced_gemini_transcription_parse_success"));
                    cacheDisplayed = true;
                }
            }
            // Check for raw Gemini result (only relevant if Yandex was OFF last time)
            else if (!Settings.YANDEX_TRANSCRIBE_SUBTITLES.get() && rawTranscriptionCache.containsKey(videoId)) {
                Logger.printDebug(() -> "Displaying cached Gemini raw transcription dialog: " + videoId);
                String result = rawTranscriptionCache.get(videoId);
                Integer time = transcriptionTimeCache.getOrDefault(videoId, -1);
                showTranscriptionResultDialogInternal(context, Objects.requireNonNull(result), Objects.requireNonNull(time), videoUrl);
                cacheDisplayed = true;
            }

            if (cacheDisplayed) {
                resetOperationStateInternal(OperationType.TRANSCRIBE, false);
                return;
            }

            String taskKey = getTaskKey(videoId, OperationType.TRANSCRIBE);

            if (activeTasks.containsKey(taskKey)) {
                Logger.printDebug(() -> "Transcription task already active for " + videoId);
                prepareForNewOperationInternal(OperationType.TRANSCRIBE, videoUrl);
                showProgressDialogInternal(context, OperationType.TRANSCRIBE);
                return;
            }

            prepareForNewOperationInternal(OperationType.TRANSCRIBE, videoUrl);
            // endregion Cache check

            // region Determine target language
            String targetLangCode;
            final String yandexSettingValue = Settings.YANDEX_TRANSCRIBE_SUBTITLES_LANGUAGE.get();

            if (APP_LANGUAGE_SETTING_KEY.equalsIgnoreCase(yandexSettingValue)) {
                try {
                    AppLanguage appLangEnum = Settings.REVANCED_LANGUAGE.get();
                    targetLangCode = appLangEnum.getLanguage();
                    String finalTargetLangCode = targetLangCode;
                    Logger.printInfo(() -> "Yandex target language set to 'app', using app language code: " + finalTargetLangCode);
                } catch (Exception e) {
                    Logger.printException(() -> "Failed to get app language code when Yandex setting was 'app'. Falling back to English.", e);
                    targetLangCode = "en";
                }
            } else {
                targetLangCode = yandexSettingValue;
                String finalTargetLangCode1 = targetLangCode;
                Logger.printInfo(() -> "Using Yandex target language code from setting: " + finalTargetLangCode1);
            }

            if (TextUtils.isEmpty(targetLangCode)) {
                showToastLong(str("revanced_yandex_error_no_language_selected"));
                resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                return;
            }

            determinedTargetLanguageCode = targetLangCode;
            intermediateLanguageCode = null;
            // endregion Determine target language

            activeTasks.put(taskKey, DUMMY_FUTURE);
            taskStartTimes.put(taskKey, System.currentTimeMillis());

            final boolean useYandex = Settings.YANDEX_TRANSCRIBE_SUBTITLES.get();

            if (useYandex) {
                startYandexTranscriptionWorkflow(context, videoUrl, durationSeconds, Objects.requireNonNull(determinedTargetLanguageCode));
            } else {
                startGeminiTranscriptionWorkflow(context, videoUrl);
            }
        });
    }

    // endregion Public API Methods

    // region Internal State Management & Workflow Logic

    /**
     * Starts the Yandex VOT transcription workflow.
     * This method now simply passes the determined target language code to YandexVotUtils,
     * which handles the logic for direct vs. intermediate language requests.
     * Must be called on the Main Thread.
     *
     * @param context             UI Context.
     * @param videoUrl            Video URL.
     * @param durationSeconds     Video duration.
     * @param finalTargetLangCode The final language code the user wants (e.g., "en", "es", "de").
     */
    @MainThread
    private void startYandexTranscriptionWorkflow(@NonNull Context context, @NonNull String videoUrl, double durationSeconds, @NonNull String finalTargetLangCode) {
        String videoId = getVideoIdFromUrl(videoUrl);
        Logger.printInfo(() -> "Starting new Yandex workflow: " + videoId + " (Final Target: " + finalTargetLangCode + ")");

        String taskKey = getTaskKey(videoId, OperationType.TRANSCRIBE);

        showProgressDialogInternal(context, OperationType.TRANSCRIBE);

        YandexVotUtils.getYandexSubtitlesWorkflowAsync(
                videoUrl, durationSeconds, finalTargetLangCode,
                new YandexVotUtils.SubtitleWorkflowCallback() {
                    @Override
                    public void onProcessingStarted(String statusMessage) {
                        ensureMainThread(() -> {
                            if (!activeTasks.containsKey(taskKey)) {
                                Logger.printDebug(() -> "Yandex status update ignored - task was cancelled: " + taskKey);
                                return;
                            }
                            handleYandexStatusUpdate(context, videoUrl, statusMessage);
                        });
                    }

                    @Override
                    public void onIntermediateSuccess(String rawIntermediateJson, String receivedIntermediateLang) {
                        ensureMainThread(() -> {
                            if (!activeTasks.containsKey(taskKey)) {
                                Logger.printDebug(() -> "Yandex intermediate result ignored - task was cancelled: " + taskKey);
                                return;
                            }

                            Logger.printInfo(() -> "Yandex intermediate success (Lang: " + receivedIntermediateLang + "). Starting Gemini translation step to " + determinedTargetLanguageCode);

                            intermediateLanguageCode = receivedIntermediateLang;

                            Locale targetLocale = getLocaleFromCode(determinedTargetLanguageCode);
                            String targetLangName = getLanguageNameFromLocale(targetLocale);

                            if (Objects.equals(currentVideoUrl, videoUrl)) {
                                baseLoadingMessage = str("revanced_gemini_status_translating", targetLangName);
                                updateTimerMessageInternal();
                            }

                            final List<String> apiKeys = GeminiUtils.parseApiKeys(Settings.GEMINI_API_KEY.get());
                            if (isEmptyApiKey(apiKeys)) {
                                activeTasks.remove(taskKey);
                                taskStartTimes.remove(taskKey);
                                if (Objects.equals(currentVideoUrl, videoUrl)) {
                                    resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                                }
                                return;
                            }

                            // Call Gemini for translation
                            String cleanedJson = YandexVotUtils.stripTokensFromYandexJson(rawIntermediateJson);

                            Future<?> translationTask = GeminiUtils.translateYandexJson(
                                    cleanedJson,
                                    targetLangName,
                                    apiKeys,
                                    new GeminiUtils.Callback() {
                                        @Override
                                        public void onSuccess(String translatedJson) {
                                            if (!activeTasks.containsKey(taskKey)) {
                                                Logger.printDebug(() -> "Translation success ignored - task cancelled.");
                                                return;
                                            }
                                            activeTasks.remove(taskKey);
                                            handleGeminiTranslationSuccess(videoUrl, translatedJson);
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            if (!activeTasks.containsKey(taskKey)) {
                                                Logger.printDebug(() -> "Translation failure ignored - task cancelled.");
                                                return;
                                            }
                                            activeTasks.remove(taskKey);
                                            handleGeminiTranslationFailure(videoUrl, error);
                                        }
                                    }
                            );
                            activeTasks.put(taskKey, translationTask != null ? translationTask : DUMMY_FUTURE);
                        });
                    }

                    @Override
                    public void onFinalSuccess(TreeMap<Long, Pair<Long, String>> parsedData) {
                        ensureMainThread(() -> {
                            if (!activeTasks.containsKey(taskKey)) {
                                Logger.printDebug(() -> "Yandex final success ignored - task was cancelled: " + taskKey);
                                return;
                            }
                            activeTasks.remove(taskKey);
                            handleYandexDirectSuccess(videoUrl, parsedData);
                        });
                    }

                    @Override
                    public void onFinalFailure(String errorMessage) {
                        ensureMainThread(() -> {
                            if (!activeTasks.containsKey(taskKey)) {
                                Logger.printDebug(() -> "Yandex failure ignored - task was cancelled: " + taskKey);
                                return;
                            }
                            activeTasks.remove(taskKey);
                            handleYandexWorkflowFailure(videoUrl, errorMessage);
                        });
                    }
                });
    }

    /**
     * Handles status updates during Yandex polling. Updates the progress dialog message.
     * Ensures updates are only applied if the operation is still relevant.
     * Must run on the Main Thread.
     *
     * @param context       UI Context.
     * @param videoUrl      The URL for which the status update applies.
     * @param statusMessage The status message from YandexVotUtils.
     */
    @MainThread
    private void handleYandexStatusUpdate(@NonNull Context context, @NonNull String videoUrl, @NonNull String statusMessage) {
        ensureMainThread(() -> {
            if (!Objects.equals(currentVideoUrl, videoUrl)) {
                Logger.printDebug(() -> "Yandex status update ignored for UI (background task): " + videoUrl);
                return;
            }

            if (intermediateLanguageCode != null) return;

            if (!Objects.equals(statusMessage, lastYandexStatusMessage)) {
                lastYandexStatusMessage = statusMessage;
                isWaitingForYandexRetry = true;
                baseLoadingMessage = str("revanced_gemini_loading_yandex_transcribe") + "\n(" + statusMessage + ")";

                if (loadingSheet != null && loadingSheet.isShowing() && !isProgressDialogMinimized) {
                    updateTimerMessageInternal();
                } else if (loadingSheet == null && !isProgressDialogMinimized) {
                    showProgressDialogInternal(context, OperationType.TRANSCRIBE);
                }
            } else {
                if (intermediateLanguageCode == null) {
                    baseLoadingMessage = str("revanced_gemini_loading_yandex_transcribe") + "\n(" + statusMessage + ")";
                }
            }
        });
    }

    /**
     * Handles the successful result from the Gemini JSON translation step.
     * Parses the translated JSON, caches, displays overlay, and resets state.
     * Must run on the Main Thread.
     *
     * @param videoUrl       The video URL for which the translation applies.
     * @param translatedJson The JSON string returned by Gemini, expected to be in the Yandex format.
     */
    @MainThread
    private void handleGeminiTranslationSuccess(@NonNull String videoUrl, @NonNull String translatedJson) {
        ensureMainThread(() -> {
            String videoId = getVideoIdFromUrl(videoUrl);
            String taskKey = getTaskKey(videoId, OperationType.TRANSCRIBE);

            YandexVotUtils.forceReleaseWorkflowLock(videoUrl);
            Logger.printInfo(() -> "Gemini translation SUCCESS. Parsing translated JSON for lang " + determinedTargetLanguageCode);

            if (Objects.equals(currentVideoUrl, videoUrl)) {
                dismissProgressDialogInternal();
            }

            try {
                TreeMap<Long, Pair<Long, String>> finalParsedData = YandexVotUtils.parseYandexJsonSubtitles(translatedJson);

                if (finalParsedData == null) {
                    Logger.printException(() -> "Gemini returned unparseable JSON after translation.", null);
                    if (Objects.equals(currentVideoUrl, videoUrl)) {
                        showToastLong(str("revanced_gemini_error_translation_parse_failed"));
                        resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                    }
                    taskStartTimes.remove(taskKey);
                    return;
                }

                transcriptionCache.put(videoId, finalParsedData);
                int time = calculateElapsedTimeSeconds(videoId, OperationType.TRANSCRIBE);
                taskStartTimes.remove(taskKey);
                transcriptionTimeCache.put(videoId, Math.max(time, 0));

                if (Objects.equals(currentVideoUrl, videoUrl)) {
                    Logger.printDebug(() -> "Displaying final translated overlay for current video...");
                    if (displayTranscriptionOverlayInternal(videoUrl)) {
                        resetOperationStateInternal(OperationType.TRANSCRIBE, false);
                        showToastShort(str("revanced_gemini_transcription_parse_success"));
                    } else {
                        showToastLong(str("revanced_gemini_error_overlay_display"));
                        resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                    }
                } else {
                    Logger.printInfo(() -> "Background Gemini translation saved to cache for " + videoId);
                }
            } catch (Exception e) {
                Logger.printException(() -> "Failed to parse Gemini's translated JSON response.", e);
                taskStartTimes.remove(taskKey);
                if (Objects.equals(currentVideoUrl, videoUrl)) {
                    showToastLong(str("revanced_gemini_error_translation_parse_failed"));
                    resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                }
            }
        });
    }

    /**
     * Handles the failure result from the Gemini JSON translation step.
     * Shows an error toast and resets state.
     * Must run on the Main Thread.
     *
     * @param videoUrl The video URL for which the translation applies.
     * @param error    The error message from GeminiUtils.
     */
    @MainThread
    private void handleGeminiTranslationFailure(@NonNull String videoUrl, @NonNull String error) {
        ensureMainThread(() -> {
            String videoId = getVideoIdFromUrl(videoUrl);
            String taskKey = getTaskKey(videoId, OperationType.TRANSCRIBE);

            Logger.printException(() -> "Gemini translation FAILED: " + error, null);
            YandexVotUtils.forceReleaseWorkflowLock(videoUrl);
            taskStartTimes.remove(taskKey);

            if (Objects.equals(currentVideoUrl, videoUrl)) {
                dismissProgressDialogInternal();
                showToastLong(str("revanced_gemini_error_translation_failed", error));
                resetOperationStateInternal(OperationType.TRANSCRIBE, true);
            }
        });
    }

    /**
     * Handles the successful completion of the Yandex transcription workflow
     * when the result is already in the final desired language (en/ru/kk).
     * Caches the result, displays the overlay, and resets state.
     * Must run on the Main Thread.
     *
     * @param videoUrl   The URL for which the success applies.
     * @param parsedData The successfully parsed subtitle data in the final language.
     */
    @MainThread
    private void handleYandexDirectSuccess(@NonNull String videoUrl, @Nullable TreeMap<Long, Pair<Long, String>> parsedData) {
        ensureMainThread(() -> {
            String videoId = getVideoIdFromUrl(videoUrl);

            if (Objects.equals(currentVideoUrl, videoUrl)) {
                lastYandexStatusMessage = null;
                isWaitingForYandexRetry = false;
                intermediateLanguageCode = null;
                dismissProgressDialogInternal();
            }

            if (parsedData == null) {
                handleYandexWorkflowFailure(videoUrl, str("revanced_yandex_error_subs_parsing_failed"));
                return;
            }

            Logger.printInfo(() -> "Yandex Workflow SUCCEEDED directly for " + videoId);

            transcriptionCache.put(videoId, parsedData);
            int time = calculateElapsedTimeSeconds(videoId, OperationType.TRANSCRIBE);
            taskStartTimes.remove(getTaskKey(videoId, OperationType.TRANSCRIBE));
            transcriptionTimeCache.put(videoId, Math.max(time, 0));

            if (Objects.equals(currentVideoUrl, videoUrl)) {
                Logger.printDebug(() -> "Displaying final Yandex overlay for current video...");
                if (displayTranscriptionOverlayInternal(videoUrl)) {
                    resetOperationStateInternal(OperationType.TRANSCRIBE, false);
                    showToastShort(str("revanced_gemini_transcription_parse_success"));
                } else {
                    showToastLong(str("revanced_gemini_error_overlay_display"));
                    resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                }
            } else {
                Logger.printInfo(() -> "Background Yandex success saved to cache for " + videoId);
            }
        });
    }

    /**
     * Handles the failure of the Yandex transcription workflow (at any stage before Gemini translation).
     * Shows an error toast and resets state.
     * Must run on the Main Thread.
     *
     * @param videoUrl     The URL for which the failure applies.
     * @param errorMessage The error message describing the failure.
     */
    @MainThread
    private void handleYandexWorkflowFailure(@NonNull String videoUrl, @NonNull String errorMessage) {
        ensureMainThread(() -> {
            String videoId = getVideoIdFromUrl(videoUrl);

            Logger.printException(() -> "Yandex Workflow FAILED for " + videoId + ": " + errorMessage, null);
            taskStartTimes.remove(getTaskKey(videoId, OperationType.TRANSCRIBE));

            if (Objects.equals(currentVideoUrl, videoUrl)) {
                lastYandexStatusMessage = null;
                isWaitingForYandexRetry = false;
                intermediateLanguageCode = null;
                dismissProgressDialogInternal();
                showToastLong(errorMessage);
                resetOperationStateInternal(OperationType.TRANSCRIBE, true);
            }
        });
    }

    /**
     * Starts the Gemini API transcription workflow (direct, without Yandex).
     * Assumes preparation (prepareForNewOperationInternal) has already occurred.
     * Must be called on the Main Thread.
     *
     * @param context  UI Context.
     * @param videoUrl Video URL.
     */
    @MainThread
    private void startGeminiTranscriptionWorkflow(@NonNull Context context, @NonNull String videoUrl) {
        String videoId = getVideoIdFromUrl(videoUrl);
        Logger.printInfo(() -> "Starting new Gemini direct transcription workflow: " + videoId);
        String taskKey = getTaskKey(videoId, OperationType.TRANSCRIBE);

        final List<String> apiKeys = GeminiUtils.parseApiKeys(Settings.GEMINI_API_KEY.get());
        if (isEmptyApiKey(apiKeys)) {
            resetOperationStateInternal(OperationType.TRANSCRIBE, true);
            taskStartTimes.remove(taskKey);
            return;
        }

        showProgressDialogInternal(context, OperationType.TRANSCRIBE);

        Future<?> transcriptionTask = GeminiUtils.getVideoTranscription(videoUrl, apiKeys, new GeminiUtils.Callback() {
            @Override
            public void onSuccess(String result) {
                if (!activeTasks.containsKey(taskKey)) {
                    Logger.printDebug(() -> "Gemini Transcription success ignored - task cancelled.");
                    return;
                }
                activeTasks.remove(taskKey);
                handleApiResponseInternal(context, OperationType.TRANSCRIBE, videoUrl, result, null);
            }

            @Override
            public void onFailure(String error) {
                if (!activeTasks.containsKey(taskKey)) {
                    Logger.printDebug(() -> "Gemini Transcription failure ignored - task cancelled.");
                    return;
                }
                activeTasks.remove(taskKey);
                handleApiResponseInternal(context, OperationType.TRANSCRIBE, videoUrl, null, error);
            }
        });
        activeTasks.put(taskKey, transcriptionTask != null ? transcriptionTask : DUMMY_FUTURE);
    }

    /**
     * Rebuilds the {@link #baseLoadingMessage} based on the current operation type and state,
     * including the Gemini translation step if active.
     * Must be called on the Main Thread.
     *
     * @param opType The operation type the dialog is for.
     */
    @MainThread
    private void rebuildBaseLoadingMessage(@NonNull OperationType opType) {
        if (opType == OperationType.TRANSCRIBE) {
            boolean isUsingYandex = Settings.YANDEX_TRANSCRIBE_SUBTITLES.get();
            boolean isTranslating = isUsingYandex && intermediateLanguageCode != null && determinedTargetLanguageCode != null;

            if (isTranslating) {
                Locale targetLocale = getLocaleFromCode(determinedTargetLanguageCode);
                String targetLangName = getLanguageNameFromLocale(targetLocale);
                baseLoadingMessage = str("revanced_gemini_status_translating", targetLangName);
            } else if (isUsingYandex) {
                baseLoadingMessage = str("revanced_gemini_loading_yandex_transcribe")
                        + (isWaitingForYandexRetry && lastYandexStatusMessage != null ? "\n(" + lastYandexStatusMessage + ")" : "");
            } else {
                baseLoadingMessage = str("revanced_gemini_loading_transcribe");
            }
        } else if (opType == OperationType.SUMMARIZE) {
            baseLoadingMessage = str("revanced_gemini_loading_summarize");
        } else {
            baseLoadingMessage = str("revanced_gemini_loading_default");
        }
    }

    /**
     * Prepares for a new operation by resetting relevant state.
     * Must be called on the Main Thread.
     *
     * @param newOperationType The new operation type.
     * @param newVideoUrl      The new video URL.
     */
    @MainThread
    private void prepareForNewOperationInternal(@NonNull OperationType newOperationType, @NonNull String newVideoUrl) {
        Logger.printDebug(() -> "Preparing UI for operation: " + newOperationType + " for URL: " + newVideoUrl);

        dismissSummarySheetInternal();
        dismissTranscriptionDialogInternal();

        isCancelled = false;
        isProgressDialogMinimized = false;
        isWaitingForYandexRetry = false;
        lastYandexStatusMessage = null;
        baseLoadingMessage = null;

        currentOperation = newOperationType;
        currentVideoUrl = newVideoUrl;

    }

    /**
     * Handles the result (success or failure) from the *direct* Gemini API callback
     * (used for Summarize or direct Transcribe when Yandex is OFF).
     * Assumes the callback runs on the Main Thread.
     *
     * @param context  UI Context.
     * @param opType   The operation type this response is for (SUMMARIZE or TRANSCRIBE).
     * @param videoUrl The video URL this response is for.
     * @param result   The successful result string (non-null on success).
     * @param error    The error message string (non-null on failure).
     */
    @MainThread
    private void handleApiResponseInternal(@NonNull Context context, @NonNull OperationType opType, @NonNull String videoUrl, @Nullable String result, @Nullable String error) {
        handleApiResponseInternal(context, opType, videoUrl, result, error, null);
    }

    /**
     * Handles the result (success or failure) from the *direct* Gemini API callback
     * with optional model metadata.
     */
    @MainThread
    private void handleApiResponseInternal(@NonNull Context context, @NonNull OperationType opType, @NonNull String videoUrl, @Nullable String result, @Nullable String error, @Nullable String usedModel) {
        dismissProgressDialogInternal();
        String videoId = getVideoIdFromUrl(videoUrl);

        if (error != null) {
            Logger.printException(() -> "Direct Gemini " + opType + " failed for " + videoId + ": " + error, null);
            taskStartTimes.remove(getTaskKey(videoId, opType));
            if (Objects.equals(currentVideoUrl, videoUrl)) {
                showToastLong(str("revanced_gemini_error_api_failed", error));
                resetOperationStateInternal(opType, true);
            }
            return;
        }

        if (result != null) {
            Logger.printInfo(() -> "Direct Gemini " + opType + " success for " + videoId);
            int time = calculateElapsedTimeSeconds(videoId, opType);
            taskStartTimes.remove(getTaskKey(videoId, opType));
            if (time < 0) time = 0;

            if (opType == OperationType.SUMMARIZE) {
                summaryCache.put(videoId, result);
                summaryTimeCache.put(videoId, time);
                summaryConversationCache.remove(videoId);
                if (!TextUtils.isEmpty(usedModel)) {
                    summaryModelCache.put(videoId, usedModel);
                } else {
                    summaryModelCache.remove(videoId);
                }

                if (Objects.equals(currentVideoUrl, videoUrl)) {
                    showSummaryDialog(context, result, time, videoUrl, usedModel);
                    resetOperationStateInternal(opType, false);
                }
            } else if (opType == OperationType.TRANSCRIBE) {
                rawTranscriptionCache.put(videoId, result);
                transcriptionTimeCache.put(videoId, time);

                if (Objects.equals(currentVideoUrl, videoUrl)) {
                    hideTranscriptionOverlayInternal();
                    showTranscriptionResultDialogInternal(context, result, time, videoUrl);
                    resetOperationStateInternal(opType, false);
                }
            }
        }
    }

    /**
     * Resets the operational state of the manager. Stops timers, clears state flags.
     * Optionally clears caches and UI elements (overlay, dialog) based on the `clearCacheAndUI` flag.
     * This is the central method for cleaning up after an operation finishes, fails, or is canceled/replaced.
     * Must be called on the Main Thread.
     *
     * @param opBeingReset    The operation type whose state is being reset. Should typically match
     *                        the `currentOperation` at the time of the call, unless called from
     *                        `prepareForNewOperationInternal` where it represents the *old* operation.
     * @param clearCacheAndUI If true, clears associated caches and forces removal of UI elements (dialog, overlay).
     *                        If false, only resets core state flags (like setting `currentOperation` to NONE),
     *                        preserving caches and potentially leaving successful UI (like the overlay) visible.
     */
    @MainThread
    private void resetOperationStateInternal(@NonNull OperationType opBeingReset, boolean clearCacheAndUI) {
        Logger.printDebug(() -> String.format("Resetting UI state for %s, clearCacheAndUI=%b.", opBeingReset, clearCacheAndUI));

        stopTimerInternal();

        if (currentOperation == opBeingReset) {
            isCancelled = false;
            isProgressDialogMinimized = false;
            currentOperation = OperationType.NONE;
            currentVideoUrl = null;
            baseLoadingMessage = null;
            determinedTargetLanguageCode = null;
            intermediateLanguageCode = null;
        }

        if (clearCacheAndUI) {
            dismissProgressDialogInternal();
            if (opBeingReset == OperationType.TRANSCRIBE || opBeingReset == OperationType.NONE) {
                hideTranscriptionOverlayInternal();
            }

            isWaitingForYandexRetry = false;
            lastYandexStatusMessage = null;
        }
    }

    // endregion Internal State Management & Workflow Logic

    // region Utility Methods

    /**
     * Extracts the Video ID from a given URL to ensure consistent caching keys.
     *
     * @param videoUrl The video URL.
     * @return The extracted video ID.
     */
    @NonNull
    private String getVideoIdFromUrl(@Nullable String videoUrl) {
        if (videoUrl == null) return "";
        try {
            Matcher matcher = VIDEO_ID_PATTERN.matcher(videoUrl);
            if (matcher.find()) {
                String id = matcher.group(1);
                if (id != null) return id;
            }
        } catch (Exception e) {
            Logger.printDebug(() -> "Failed to extract ID from URL: " + videoUrl);
        }
        return videoUrl;
    }

    /**
     * Helper to generate a unique key for tracking tasks using the normalized Video ID.
     *
     * @param videoIdOrUrl The video ID or URL.
     * @param type         The operation type.
     * @return The task key.
     */
    private String getTaskKey(@NonNull String videoIdOrUrl, @NonNull OperationType type) {
        return videoIdOrUrl + "_" + type.name();
    }

    /**
     * Captures current Channel Name and Video Title from VideoInformation and stores it in cache.
     * Uses the Video ID as key.
     * If metadata is missing, it launches a background OEmbed fetch.
     *
     * @param videoId The video ID.
     */
    @MainThread
    private void captureVideoMetadata(@NonNull String videoId) {
        try {
            String currentId = VideoInformation.getVideoId();
            if (Objects.equals(videoId, currentId)) {
                String title = VideoInformation.getVideoTitle();
                String channel = VideoInformation.getChannelName();

                String meta = (TextUtils.isEmpty(channel) ? "?" : channel) + " - " +
                        (TextUtils.isEmpty(title) ? "?" : title);
                videoMetadataCache.put(videoId, meta);
            } else {
                if (!videoMetadataCache.containsKey(videoId)) {
                    videoMetadataCache.put(videoId, str("revanced_gemini_loading_default"));
                    fetchMetadataInBackground(videoId);
                }
            }
        } catch (Exception e) {
            Logger.printException(() -> "Failed to capture video metadata", e);
        }
    }

    /**
     * Fetches Video Title and Author Name from YouTube OEmbed API in the background.
     * Updates cache and refreshes UI if needed.
     *
     * @param videoId The video ID.
     */
    private void fetchMetadataInBackground(@NonNull String videoId) {
        metadataExecutor.submit(() -> {
            try {
                String oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=" + videoId + "&format=json";
                URL url = new URL(oembedUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                    }

                    JSONObject json = new JSONObject(response.toString());
                    String title = json.optString("title", "");
                    String author = json.optString("author_name", "");

                    if (!title.isEmpty()) {
                        String meta = (author.isEmpty() ? "?" : author) + " - " + title;
                        videoMetadataCache.put(videoId, meta);

                        mainThreadHandler.post(() -> {
                            if (currentVideoUrl != null && Objects.equals(getVideoIdFromUrl(currentVideoUrl), videoId)) {
                                updateTimerMessageInternal();
                            }
                        });
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                Logger.printDebug(() -> "Failed to fetch OEmbed metadata for " + videoId);
            }
        });
    }

    /**
     * Hides the transcription overlay.
     * Must be called on the Main Thread.
     */
    @MainThread
    private void hideTranscriptionOverlayInternal() {
        ensureMainThread(() -> {
            stopSubtitleUpdaterInternal();

            SubtitleOverlay overlayInstance = (subtitleOverlayRef != null) ? subtitleOverlayRef.get() : null;

            if (overlayInstance != null) {
                Logger.printInfo(() -> "Hiding and cleaning up transcription overlay instance.");
                final View viewToRemove = overlayInstance.getOverlayView();

                this.isSubtitleOverlayShowing = false;
                subtitleOverlayRef.clear();
                subtitleOverlayRef = null;

                if (viewToRemove != null) {
                    try {
                        WindowManager wm = (WindowManager) overlayInstance.context.getSystemService(Context.WINDOW_SERVICE);
                        if (wm != null) {
                            wm.removeViewImmediate(viewToRemove);
                        }
                    } catch (Exception e) {
                        Logger.printDebug(() -> "Manager skipping direct removal attempt or failed.");
                    }
                }
                overlayInstance.destroy();
            } else {
                if (isSubtitleOverlayShowing) {
                    isSubtitleOverlayShowing = false;
                }
            }
        });
    }

    /**
     * Checks if the API key is empty and shows a toast if so.
     *
     * @param apiKeys List of API keys.
     * @return True if the key is empty, false otherwise.
     */
    private boolean isEmptyApiKey(@Nullable List<String> apiKeys) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            showToastLong(str("revanced_gemini_error_no_api_key"));
            Logger.printDebug(() -> "isValidApiKey: API key is empty or null.");
            return true;
        }
        return false;
    }

    /**
     * Calculates the elapsed time in seconds since the operation started.
     *
     * @return Elapsed time in seconds, or -1 if start time is invalid.
     */
    private int calculateElapsedTimeSeconds() {
        if (currentVideoUrl == null || currentOperation == OperationType.NONE) return -1;
        String videoId = getVideoIdFromUrl(currentVideoUrl);
        return calculateElapsedTimeSeconds(videoId, currentOperation);
    }

    /**
     * Calculates time elapsed for a specific task using ID.
     *
     * @param videoId The video ID.
     * @param type    The operation type.
     * @return Elapsed time in seconds, or -1 if start time is invalid.
     */
    private int calculateElapsedTimeSeconds(@NonNull String videoId, @NonNull OperationType type) {
        String key = getTaskKey(videoId, type);
        Long start = taskStartTimes.get(key);
        if (start == null) return -1;
        return (int) ((System.currentTimeMillis() - start) / 1000);
    }

    /**
     * Ensures the provided Runnable is executed on the main thread.
     * If already on the main thread, run it immediately. Otherwise, post it.
     *
     * @param action The Runnable to execute on the main thread.
     */
    private void ensureMainThread(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mainThreadHandler.post(action);
        }
    }

    /**
     * Gets a Locale object from a language code string (e.g., "en", "es", "pt-BR").
     * Handles potential exceptions during Locale creation.
     *
     * @param langCode The language code string.
     * @return A Locale object, or Locale.ENGLISH as a fallback if the code is invalid/null.
     */
    @NonNull
    private Locale getLocaleFromCode(@Nullable String langCode) {
        if (TextUtils.isEmpty(langCode)) {
            return Locale.ENGLISH;
        }
        try {
            return new Locale.Builder().setLanguageTag(langCode.replace("_", "-")).build();
        } catch (Exception e) {
            Logger.printException(() -> "Could not create Locale from language tag: " + langCode + ". Returning Locale.ENGLISH.", e);
        }
        return Locale.ENGLISH;
    }

    /**
     * Gets the display name for a given Locale, in English.
     * Uses standard Locale methods.
     *
     * @param locale The Locale object.
     * @return The display name in English (e.g., "Spanish", "German"), or the language code as fallback.
     */
    @NonNull
    private String getLanguageNameFromLocale(@Nullable Locale locale) {
        if (locale == null) {
            return "English";
        }
        try {
            String displayName = locale.getDisplayLanguage(Locale.ENGLISH);
            if (!TextUtils.isEmpty(displayName) && !displayName.equalsIgnoreCase(locale.getLanguage())) {
                return displayName;
            } else {
                String langCode = locale.getLanguage();
                if (!TextUtils.isEmpty(langCode)) {
                    return langCode.substring(0, 1).toUpperCase(Locale.ENGLISH) + langCode.substring(1);
                }
            }
        } catch (Exception e) {
            String langCode = locale.getLanguage();
            if (!TextUtils.isEmpty(langCode)) {
                return langCode;
            }
        }
        return "English";
    }

    @Nullable
    private String getVideoMetaText(@Nullable String videoUrl) {
        if (TextUtils.isEmpty(videoUrl)) {
            return null;
        }

        String videoId = getVideoIdFromUrl(videoUrl);
        String meta = videoMetadataCache.getOrDefault(videoId, "");
        if (!TextUtils.isEmpty(meta) && !meta.equals(str("revanced_gemini_loading_default"))) {
            return meta;
        }
        return null;
    }

    @Nullable
    private String getCurrentVideoMetaText() {
        return getVideoMetaText(currentVideoUrl);
    }

    @Nullable
    private String buildInfoText(int seconds, @Nullable String usedModel) {
        StringBuilder infoText = new StringBuilder();
        if (!TextUtils.isEmpty(usedModel)) {
            infoText.append(usedModel);
        }
        if (seconds >= 0) {
            if (infoText.length() > 0) {
                infoText.append('\n');
            }
            infoText.append(str("revanced_gemini_time_taken", seconds));
        }
        return infoText.length() == 0 ? null : infoText.toString();
    }

    @MainThread
    private void handleSummaryStreamUpdate(@NonNull String videoUrl, @NonNull String accumulatedText) {
        if (!Objects.equals(currentVideoUrl, videoUrl) || isProgressDialogMinimized || loadingSheet == null || !loadingSheet.isShowing()) {
            return;
        }
        loadingSheet.updatePreview(accumulatedText);
    }

    @MainThread
    private void handleSummaryChatSend(
            @NonNull String videoUrl,
            @NonNull String summary,
            @NonNull GeminiBottomSheetUi.SummarySheet sheet
    ) {
        if (summarySheet != sheet || !sheet.isShowing()) {
            sheet.failPendingResponse();
            return;
        }

        List<String> apiKeys = GeminiUtils.parseApiKeys(Settings.GEMINI_API_KEY.get());
        if (isEmptyApiKey(apiKeys)) {
            sheet.failPendingResponse();
            return;
        }

        cancelActiveSummaryChatTaskInternal();

        Future<?> chatTask = GeminiUtils.chatWithVideo(
                videoUrl,
                summary,
                sheet.getConversationWithPendingQuestion(),
                apiKeys,
                new GeminiUtils.Callback() {
                    @Override
                    public void onSuccess(String result) {
                        onSuccessWithModel(result, null);
                    }

                    @Override
                    public void onPartial(String partialText, String accumulatedText, @Nullable String model) {
                        ensureMainThread(() -> {
                            if (summarySheet == sheet && sheet.isShowing()) {
                                sheet.updatePendingResponse(accumulatedText);
                            }
                        });
                    }

                    @Override
                    public void onSuccessWithModel(String result, @Nullable String model) {
                        ensureMainThread(() -> {
                            if (summarySheet == sheet && sheet.isShowing()) {
                                sheet.commitPendingResponse(normalizeGeminiText(result));
                            }
                            activeSummaryChatTask = null;
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        ensureMainThread(() -> {
                            if (summarySheet == sheet && sheet.isShowing()) {
                                sheet.failPendingResponse();
                            }
                            activeSummaryChatTask = null;
                            if (!"Operation cancelled.".equals(error)) {
                                showToastLong(str("revanced_gemini_error_api_failed", error));
                            }
                        });
                    }
                }
        );

        if (chatTask == null) {
            sheet.failPendingResponse();
            return;
        }
        activeSummaryChatTask = chatTask;
    }

    @MainThread
    private void cancelActiveSummaryChatTaskInternal() {
        boolean hadActiveTask = activeSummaryChatTask != null;
        if (hadActiveTask) {
            try {
                activeSummaryChatTask.cancel(true);
            } catch (Exception e) {
                Logger.printException(() -> "Error cancelling Gemini summary chat task", e);
            } finally {
                activeSummaryChatTask = null;
            }
        }

        if (hadActiveTask && summarySheet != null && summarySheet.isShowing()) {
            summarySheet.failPendingResponse();
        }
    }

    @MainThread
    private void dismissSummarySheetInternal() {
        cancelActiveSummaryChatTaskInternal();

        if (summarySheet != null) {
            try {
                if (summarySheet.isShowing()) {
                    summarySheet.dismiss();
                }
            } catch (Exception e) {
                Logger.printException(() -> "Error dismissing Gemini summary sheet", e);
            } finally {
                summarySheet = null;
            }
        }
    }

    @MainThread
    private void dismissTranscriptionDialogInternal() {
        if (transcriptionDialogRef != null) {
            SheetBottomDialog.SlideDialog dialog = transcriptionDialogRef.get();
            if (dialog != null && dialog.isShowing()) {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    Logger.printException(() -> "Error dismissing Gemini transcription sheet", e);
                }
            }
            transcriptionDialogRef.clear();
            transcriptionDialogRef = null;
        }
    }

    // endregion Utility Methods

    // region UI Methods: Dialogs

    /**
     * Displays or updates the progress dialog. Handles minimizing and cancelling.
     * Must be called on the Main Thread.
     *
     * @param context Context for creating the dialog.
     * @param opType  The type of the operation (SUMMARIZE or TRANSCRIBE).
     */
    @MainThread
    private void showProgressDialogInternal(@NonNull Context context, @NonNull OperationType opType) {
        dismissProgressDialogInternal();

        if (isCancelled || isProgressDialogMinimized) {
            return;
        }

        if (currentOperation != opType) {
            return;
        }

        if (context instanceof Activity activity) {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        rebuildBaseLoadingMessage(opType);
        try {
            final String statusText = (baseLoadingMessage != null && !baseLoadingMessage.isEmpty())
                    ? baseLoadingMessage
                    : str("revanced_gemini_loading_default");
            final String videoUrlAtShow = currentVideoUrl;

            loadingSheet = new GeminiBottomSheetUi.LoadingSheet(
                    context,
                    opType == OperationType.SUMMARIZE
                            ? str("revanced_gemini_summary_title")
                            : str("revanced_gemini_loading_default"),
                    getCurrentVideoMetaText(),
                    statusText,
                    () -> ensureMainThread(() -> {
                        if (videoUrlAtShow == null) {
                            return;
                        }
                        if (Objects.equals(currentVideoUrl, videoUrlAtShow)) {
                            Logger.printInfo(() -> opType + " UI cancelled by user for " + videoUrlAtShow);
                            isCancelled = true;

                            String videoId = getVideoIdFromUrl(videoUrlAtShow);
                            String taskKey = getTaskKey(videoId, opType);
                            Future<?> future = activeTasks.remove(taskKey);
                            if (future != null) {
                                try {
                                    future.cancel(true);
                                } catch (Exception e) {
                                    Logger.printException(() -> "Error cancelling future for task: " + taskKey, e);
                                }
                            }

                            if (opType == OperationType.TRANSCRIBE) {
                                YandexVotUtils.forceReleaseWorkflowLock(videoUrlAtShow);
                            }

                            resetOperationStateInternal(opType, true);
                            showToastShort(str("revanced_gemini_cancelled"));
                        }
                    }),
                    () -> ensureMainThread(() -> {
                        if (Objects.equals(currentVideoUrl, videoUrlAtShow) && !isProgressDialogMinimized) {
                            isProgressDialogMinimized = true;
                            stopTimerInternal();
                            loadingSheet = null;
                        }
                    })
            );
            updateTimerMessageInternal();
            startTimerInternal();
        } catch (Exception e) {
            Logger.printException(() -> "Error showing progress dialog for " + opType, e);
            ensureMainThread(() -> {
                showToastLong(str("revanced_gemini_error_dialog_show") + ": " + e.getMessage());
                resetOperationStateInternal(opType, true);
            });
        }
    }

    /**
     * Dismisses the current progress dialog if it is showing and stops its timer.
     * Safe to call if the dialog is null or not showing.
     * Must be called on the Main Thread.
     */
    @MainThread
    private void dismissProgressDialogInternal() {
        stopTimerInternal();
        if (loadingSheet != null) {
            try {
                if (loadingSheet.isShowing()) {
                    loadingSheet.dismiss();
                }
            } catch (Exception e) {
                Logger.printException(() -> "Error dismissing progress dialog", e);
            } finally {
                loadingSheet = null;
            }
        }
    }

    /**
     * Displays the summary result in an AlertDialog.
     * Must be called on the Main Thread.
     *
     * @param context  Context for creating the dialog.
     * @param summary  The summary text.
     * @param seconds  Time taken for the operation.
     * @param videoUrl The video URL.
     * @param usedModel The Gemini model used for this summary (nullable).
     */
    @MainThread
    private void showSummaryDialog(@NonNull Context context, @NonNull String summary, int seconds, @NonNull String videoUrl, @Nullable String usedModel) {
        String videoId = getVideoIdFromUrl(videoUrl);
        String normalizedSummary = normalizeGeminiText(summary);
        CharSequence formattedSummary = formatGeminiText(normalizedSummary, videoUrl);
        List<GeminiUtils.ChatMessage> cachedConversation = new ArrayList<>(
                Objects.requireNonNull(summaryConversationCache.getOrDefault(videoId, Collections.emptyList()))
        );

        dismissSummarySheetInternal();
        summarySheet = new GeminiBottomSheetUi.SummarySheet(
                context,
                getVideoMetaText(videoUrl),
                buildInfoText(seconds, usedModel),
                formattedSummary,
                normalizedSummary,
                cachedConversation,
                text -> formatGeminiText(text, videoUrl),
                text -> setClipboard(context, normalizeGeminiText(text), str("revanced_gemini_copy_success")),
                history -> summaryConversationCache.put(videoId, new ArrayList<>(history)),
                () -> ensureMainThread(() -> {
                    cancelActiveSummaryChatTaskInternal();
                    summarySheet = null;
                }),
                (sheet, question) -> ensureMainThread(() -> handleSummaryChatSend(videoUrl, normalizedSummary, sheet))
        );
    }

    @NonNull
    private String normalizeGeminiText(@NonNull String text) {
        return text.replace("\r\n", "\n").trim();
    }

    @NonNull
    private CharSequence formatGeminiText(@NonNull String text, @NonNull String videoUrl) {
        String normalizedText = normalizeGeminiText(text);
        String displayText = wrapTimestampReferencesForPillStyle(normalizedText);
        Spanned formattedText = MarkdownUtils.fromMarkdown(displayText);
        return createClickableTimestampText(formattedText, videoUrl);
    }

    @NonNull
    private String wrapTimestampReferencesForPillStyle(@NonNull String summary) {
        Matcher matcher = SUMMARY_TIMESTAMP_PATTERN.matcher(summary);
        StringBuilder builder = new StringBuilder(summary.length() + 16);
        int cursor = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            builder.append(summary, cursor, start);

            boolean alreadyWrapped = (start > 0 && summary.charAt(start - 1) == '`')
                    || (end < summary.length() && summary.charAt(end) == '`');

            if (alreadyWrapped) {
                builder.append(summary, start, end);
            } else {
                builder.append('`').append(summary, start, end).append('`');
            }
            cursor = end;
        }

        builder.append(summary, cursor, summary.length());
        return builder.toString();
    }

    @NonNull
    private SpannableStringBuilder createClickableTimestampText(@NonNull Spanned formattedText, @NonNull String videoUrl) {
        SpannableStringBuilder clickableText = new SpannableStringBuilder(formattedText);
        String videoId = getVideoIdFromUrl(videoUrl);
        if (TextUtils.isEmpty(videoId)) {
            return clickableText;
        }

        Matcher matcher = SUMMARY_TIMESTAMP_PATTERN.matcher(clickableText.toString());
        while (matcher.find()) {
            final long timestampSeconds;
            try {
                timestampSeconds = parseTimestampToSeconds(matcher);
            } catch (Exception ignored) {
                continue;
            }

            String vndLink = String.format(Locale.ENGLISH, VIDEO_SCHEME_INTENT_FORMAT, videoId, timestampSeconds);
            clickableText.setSpan(
                    new TimestampClickableSpan(vndLink),
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        return clickableText;
    }

    private long parseTimestampToSeconds(@NonNull Matcher matcher) {
        long hours = 0L;
        String hoursPart = matcher.group(1);
        if (!TextUtils.isEmpty(hoursPart)) {
            hours = Long.parseLong(hoursPart);
        }

        long minutes = Long.parseLong(Objects.requireNonNull(matcher.group(2)));
        long seconds = Long.parseLong(Objects.requireNonNull(matcher.group(3)));
        return TimeUnit.HOURS.toSeconds(hours) + TimeUnit.MINUTES.toSeconds(minutes) + seconds;
    }

    private static final class TimestampClickableSpan extends ClickableSpan {
        private final String vndLink;

        private TimestampClickableSpan(@NonNull String vndLink) {
            this.vndLink = vndLink;
        }

        @Override
        public void onClick(@NonNull View widget) {
            try {
                IntentUtils.launchView(vndLink, widget.getContext().getPackageName());
            } catch (Exception e) {
                Logger.printException(() -> "Failed to open summary timestamp link: " + vndLink, e);
            }
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setColor(ThemeUtils.getAppForegroundColor());
        }
    }

    /**
     * Displays the raw Gemini transcription result in an AlertDialog.
     * Includes options to copy or attempt to parse and show as an overlay.
     * This is primarily used when Yandex subtitles are OFF.
     * Must be called on the Main Thread.
     *
     * @param context          Context for creating the dialog.
     * @param rawTranscription The raw transcription text from Gemini.
     * @param seconds          Time taken for the operation.
     * @param videoUrl         The video URL.
     */
    @MainThread
    private void showTranscriptionResultDialogInternal(@NonNull Context context, @NonNull String rawTranscription, int seconds, @NonNull String videoUrl) {
        Logger.printDebug(() -> "Showing raw Gemini transcription dialog.");
        final String dialogVideoUrl = videoUrl;
        dismissTranscriptionDialogInternal();
        SheetBottomDialog.SlideDialog dialog = GeminiBottomSheetUi.showTranscriptionSheet(
                context,
                getVideoMetaText(videoUrl),
                buildInfoText(seconds, null),
                rawTranscription,
                () -> setClipboard(context, rawTranscription, str("revanced_gemini_copy_success")),
                () -> ensureMainThread(() -> {
                    Logger.printDebug(() -> "Parse button clicked (Gemini raw). Attempting parse and display overlay.");
                    hideTranscriptionOverlayInternal();
                    parseAndShowTranscriptionInternal(rawTranscription, dialogVideoUrl);
                })
        );
        transcriptionDialogRef = new WeakReference<>(dialog);
    }

    // endregion UI Methods: Dialogs

    // region UI Methods: Transcription Parsing and Overlay

    /**
     * Attempts to parse the provided raw Gemini transcription data and, if successful,
     * displays it using the subtitle overlay. Updates the cache upon success.
     * Used when the user explicitly clicks "Subtitle" from the results dialog.
     * Must be called on the Main Thread.
     *
     * @param rawText  The raw transcription text to parse.
     * @param videoUrl The video URL associated with this text.
     */
    @MainThread
    private void parseAndShowTranscriptionInternal(@NonNull String rawText, @NonNull String videoUrl) {
        if (TextUtils.isEmpty(rawText)) {
            showToastLong(str("revanced_gemini_error_transcription_no_raw_data"));
            hideTranscriptionOverlayInternal();
            return;
        }

        TreeMap<Long, Pair<Long, String>> parsedData;
        try {
            parsedData = parseGeminiTranscriptionInternal(rawText);
        } catch (Exception e) {
            Logger.printException(() -> "Failed to parse raw Gemini data.", e);
            showToastLong(str("revanced_gemini_error_transcription_parse") + ": " + e.getMessage());
            hideTranscriptionOverlayInternal();
            return;
        }

        String videoId = getVideoIdFromUrl(videoUrl);

        if (parsedData.isEmpty()) {
            if (!rawText.trim().isEmpty()) {
                showToastLong(str("revanced_gemini_error_transcription_parse"));
            }
            transcriptionCache.put(videoId, parsedData);
            rawTranscriptionCache.put(videoId, rawText);
            hideTranscriptionOverlayInternal();
            return;
        }

        transcriptionCache.put(videoId, parsedData);
        rawTranscriptionCache.put(videoId, rawText);

        if (displayTranscriptionOverlayInternal(videoUrl)) {
            showToastShort(str("revanced_gemini_transcription_parse_success"));
            resetOperationStateInternal(OperationType.TRANSCRIBE, false);
        }
    }

    /**
     * Parses the raw transcription text (expected Gemini format) into a timed map.
     *
     * @param rawText The raw transcription string.
     * @return A TreeMap mapping start times (ms) to Pairs of end times (ms) and subtitle text.
     * Returns an empty map if the input is empty or no valid lines are found.
     */
    @NotNull
    private TreeMap<Long, Pair<Long, String>> parseGeminiTranscriptionInternal(@NonNull String rawText) {
        if (TextUtils.isEmpty(rawText)) {
            return new TreeMap<>();
        }
        TreeMap<Long, Pair<Long, String>> map = new TreeMap<>();
        Matcher m = TRANSCRIPTION_PATTERN.matcher("");

        for (String line : rawText.split("\r?\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            m.reset(trimmedLine);
            if (m.find()) {
                try {
                    // Group indices from TRANSCRIPTION_PATTERN:
                    // 1: start Hr (opt), 2: start Min, 3: start Sec, 4: start Ms
                    // 5: end Hr (opt),   6: end Min,   7: end Sec,   8: end Ms
                    // 9: text

                    long sh = (m.group(1) != null) ? Long.parseLong(Objects.requireNonNull(m.group(1))) : 0;
                    long sm = Long.parseLong(Objects.requireNonNull(m.group(2)));
                    long ss = Long.parseLong(Objects.requireNonNull(m.group(3)));
                    long sms = parseAndPadMilliseconds(m.group(4));
                    long st = TimeUnit.HOURS.toMillis(sh) + TimeUnit.MINUTES.toMillis(sm) + TimeUnit.SECONDS.toMillis(ss) + sms;

                    long eh = (m.group(5) != null) ? Long.parseLong(Objects.requireNonNull(m.group(5))) : 0;
                    long em = Long.parseLong(Objects.requireNonNull(m.group(6)));
                    long es = Long.parseLong(Objects.requireNonNull(m.group(7)));
                    long ems = parseAndPadMilliseconds(m.group(8));
                    long et = TimeUnit.HOURS.toMillis(eh) + TimeUnit.MINUTES.toMillis(em) + TimeUnit.SECONDS.toMillis(es) + ems;

                    String text = m.group(9);
                    String trimmedText = (text != null) ? text.trim() : "";

                    if (et > st && !trimmedText.isEmpty()) {
                        map.put(st, new Pair<>(et, trimmedText));
                    }
                } catch (Exception e) {
                    Logger.printException(() -> "Unexpected error processing Gemini line: " + trimmedLine, e);
                }
            }
        }
        return map;
    }

    /**
     * Parses and pads milliseconds from the string.
     *
     * @param msString The milliseconds string.
     * @return The parsed milliseconds.
     */
    private long parseAndPadMilliseconds(@Nullable String msString) {
        if (msString == null || msString.isEmpty()) return 0L;
        try {
            String cleanedMs = msString.replaceAll("[^0-9]", "");
            if (cleanedMs.isEmpty()) return 0L;
            if (cleanedMs.length() > 3) {
                cleanedMs = cleanedMs.substring(0, 3);
            }

            long ms = Long.parseLong(cleanedMs);

            int originalLength = msString.length();
            if (originalLength == 1) {
                return ms * 100;
            } else if (originalLength == 2) {
                return ms * 10;
            } else {
                return ms;
            }
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Displays the transcription overlay.
     * Must be called on the Main Thread.
     *
     * @param videoUrl The video URL.
     * @return True if displayed successfully, false otherwise.
     */
    @MainThread
    private boolean displayTranscriptionOverlayInternal(String videoUrl) {
        Logger.printDebug(() -> "Attempting display transcription overlay...");

        String videoId = getVideoIdFromUrl(videoUrl);
        TreeMap<Long, Pair<Long, String>> data = transcriptionCache.get(videoId);

        if (data == null) {
            hideTranscriptionOverlayInternal();
            return false;
        }

        hideTranscriptionOverlayInternal();

        try {
            SubtitleOverlay overlay = new SubtitleOverlay();

            long currentTime = VideoInformation.getVideoTime();
            String initialText = findSubtitleTextForTimeInternal(data, currentTime);
            overlay.updateText(initialText);

            overlay.show();
            subtitleOverlayRef = new WeakReference<>(overlay);

            isSubtitleOverlayShowing = true;
            Logger.printInfo(() -> "Subtitle overlay displayed successfully.");

            startSubtitleUpdaterInternal(data);
            return true;
        } catch (Exception e) {
            Logger.printException(() -> "CRITICAL - Failed during SubtitleOverlay creation or show()", e);
            if (e instanceof WindowManager.BadTokenException) {
                showToastLong(str("revanced_gemini_transcribe_bad_token_exception"));
            } else {
                showToastLong(str("revanced_gemini_error_overlay_display") + ": " + e.getMessage());
            }

            hideTranscriptionOverlayInternal();
            return false;
        }
    }

    /**
     * Starts the subtitle updater.
     * Must be called on the Main Thread.
     *
     * @param data The transcription data.
     */
    @MainThread
    private void startSubtitleUpdaterInternal(TreeMap<Long, Pair<Long, String>> data) {
        if (!isSubtitleOverlayShowing || data == null || subtitleOverlayRef == null) {
            stopSubtitleUpdaterInternal();
            return;
        }

        stopSubtitleUpdaterInternal();

        subtitleUpdateRunnable = new Runnable() {
            private String lastTextSentToOverlay = null;

            @Override
            public void run() {
                SubtitleOverlay currentOverlay = subtitleOverlayRef.get();

                if (!isSubtitleOverlayShowing || currentOverlay == null) {
                    subtitleUpdateRunnable = null;
                    return;
                }

                long currentTime = VideoInformation.getVideoTime();
                if (currentTime < 0) {
                    if (isSubtitleOverlayShowing) {
                        subtitleUpdateHandler.postDelayed(this, SUBTITLE_UPDATE_INTERVAL_MS);
                    } else {
                        subtitleUpdateRunnable = null;
                    }
                    return;
                }

                String textToShow = findSubtitleTextForTimeInternal(data, currentTime);

                if (!Objects.equals(textToShow, lastTextSentToOverlay)) {
                    try {
                        currentOverlay.updateText(textToShow);
                        lastTextSentToOverlay = textToShow;
                    } catch (Exception e) {
                        hideTranscriptionOverlayInternal();
                        return;
                    }
                }

                if (isSubtitleOverlayShowing) {
                    subtitleUpdateHandler.postDelayed(this, SUBTITLE_UPDATE_INTERVAL_MS);
                } else {
                    subtitleUpdateRunnable = null;
                }
            }
        };

        subtitleUpdateHandler.post(subtitleUpdateRunnable);
    }

    /**
     * Stops the periodic runnable that updates the subtitle overlay text.
     * Safe to call multiple times or if not running.
     * Must be called on the Main Thread.
     */
    @MainThread
    private void stopSubtitleUpdaterInternal() {
        if (subtitleUpdateRunnable != null) {
            subtitleUpdateHandler.removeCallbacks(subtitleUpdateRunnable);
            subtitleUpdateRunnable = null;
        }
    }

    /**
     * Finds the subtitle text for the given time.
     *
     * @param data              The transcription data.
     * @param currentTimeMillis The current time in milliseconds.
     * @return The subtitle text or placeholder.
     */
    @NonNull
    private String findSubtitleTextForTimeInternal(TreeMap<Long, Pair<Long, String>> data, long currentTimeMillis) {
        if (data == null || data.isEmpty() || currentTimeMillis < 0) {
            return EMPTY_SUBTITLE_PLACEHOLDER;
        }

        Map.Entry<Long, Pair<Long, String>> entry = data.floorEntry(currentTimeMillis);

        if (entry != null) {
            long startTime = entry.getKey();
            Pair<Long, String> value = entry.getValue();
            long endTime = value.first;
            String text = value.second;

            if (currentTimeMillis >= startTime && currentTimeMillis < endTime) {
                return !TextUtils.isEmpty(text) ? text : EMPTY_SUBTITLE_PLACEHOLDER;
            }
        }

        return EMPTY_SUBTITLE_PLACEHOLDER;
    }

    // endregion UI Methods: Transcription Parsing and Overlay

    // region UI Methods: Timer for Progress Dialog

    /**
     * Starts the periodic timer that updates the elapsed time in the progress dialog.
     * Must be called on the Main Thread.
     */
    @MainThread
    private void startTimerInternal() {
        if (currentOperation == OperationType.NONE
                || isCancelled
                || isProgressDialogMinimized
                || loadingSheet == null
                || !loadingSheet.isShowing()) {
            stopTimerInternal();
            return;
        }

        stopTimerInternal();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentOperation != OperationType.NONE
                        && !isCancelled
                        && !isProgressDialogMinimized
                        && loadingSheet != null
                        && loadingSheet.isShowing()) {
                    updateTimerMessageInternal();
                    timerHandler.postDelayed(this, 1000);
                } else {
                    timerRunnable = null;
                }
            }
        };

        updateTimerMessageInternal();
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    /**
     * Stops the timer.
     * Must be called on the Main Thread.
     */
    @MainThread
    private void stopTimerInternal() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    /**
     * Updates the timer message in the progress dialog.
     * Must be called on the Main Thread.
     */
    @MainThread
    private void updateTimerMessageInternal() {
        if (loadingSheet == null) {
            stopTimerInternal();
            return;
        }

        if (loadingSheet.isShowing() && !isCancelled && !isProgressDialogMinimized) {
            int sec = calculateElapsedTimeSeconds();
            if (sec < 0) {
                return;
            }

            String base = (baseLoadingMessage != null && !baseLoadingMessage.isEmpty())
                    ? baseLoadingMessage
                    : str("revanced_gemini_loading_default");
            try {
                loadingSheet.updateMeta(getCurrentVideoMetaText());
                loadingSheet.updateStatus(base);
                loadingSheet.updateElapsed(sec);
            } catch (Exception e) {
                stopTimerInternal();
            }
        } else {
            stopTimerInternal();
        }
    }

    // endregion UI Methods: Timer for Progress Dialog

    /**
     * Represents the type of operation the manager is currently performing or NONE if idle.
     */
    private enum OperationType {
        /**
         * Video summarization task.
         */
        SUMMARIZE,
        /**
         * Video transcription task (Yandex or Gemini).
         */
        TRANSCRIBE,
        /**
         * Manager is idle, no active operation.
         */
        NONE
    }
}
