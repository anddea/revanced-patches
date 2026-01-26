package app.revanced.extension.youtube.utils;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.revanced.extension.shared.settings.AppLanguage;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.showToastLong;
import static app.revanced.extension.shared.utils.Utils.showToastShort;

/**
 * Manages Gemini API operations (Summarization, Transcription) and Yandex VOT Transcription.
 * Handles UI display (progress dialogs, results, subtitle overlay), caching, and state management.
 * This class is designed as a singleton to maintain a consistent state across the application.
 * <p>
 * It ensures that only one operation (Summarize or Transcribe) is logically active at a time,
 * cancelling previous operations if a new one is requested. It also manages the lifecycle
 * of UI elements like progress dialogs and the {@link SubtitleOverlay}.
 */
@SuppressWarnings("RegExpRedundantEscape")
public final class GeminiManager {
    // --- Constants ---
    private static final Pattern TRANSCRIPTION_PATTERN = Pattern.compile(
            // Matches [ HH:MM:SS.ms - HH:MM:SS.ms ]: TEXT and dot/colon/comma separator for milliseconds
            "\\[\\s*(?:(\\d{2}):)?(\\d{2}):(\\d{2})[.,:](\\d{1,3})\\s*-\\s*(?:(\\d{2}):)?(\\d{2}):(\\d{2})[.,:](\\d{1,3})\\s*\\]:?\\s*(.*)"
    );
    private static final long SUBTITLE_UPDATE_INTERVAL_MS = 250; // How often to check for subtitle text updates
    private static final String EMPTY_SUBTITLE_PLACEHOLDER = "..."; // Displayed when no subtitle text is active
    private static final String APP_LANGUAGE_SETTING_KEY = "app"; // Keyword for using app language in Yandex setting

    // --- Singleton Instance ---
    private static volatile GeminiManager instance;

    // --- Handlers ---
    private final Handler timerHandler = new Handler(Looper.getMainLooper()); // For progress dialog timer
    private final Handler subtitleUpdateHandler = new Handler(Looper.getMainLooper()); // For subtitle overlay updates
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper()); // For posting actions to the main thread

    // --- UI State ---
    @Nullable
    private AlertDialog progressDialog;
    private boolean isProgressDialogMinimized = false;

    // --- Operation State ---
    private volatile boolean isCancelled = false;
    private volatile OperationType currentOperation = OperationType.NONE;
    @Nullable
    private volatile String currentVideoUrl = null;
    private long startTimeMillis = -1;
    @Nullable
    private String baseLoadingMessage;
    private volatile boolean isWaitingForYandexRetry = false;
    @Nullable
    private String lastYandexStatusMessage = null;
    // Store the final target language code determined at the start of transcription
    @Nullable
    private volatile String determinedTargetLanguageCode = null;
    // Store the intermediate language if Gemini translation is needed
    @Nullable
    private volatile String intermediateLanguageCode = null;


    // --- Caches ---
    @Nullable
    private String cachedSummaryVideoUrl = null;
    @Nullable
    private String cachedSummaryResult = null;
    private int totalSummarizationTimeSeconds = -1;

    @Nullable
    private String cachedTranscriptionVideoUrl = null;
    @Nullable
    private String cachedRawTranscription = null; // Raw Gemini output / Null for Yandex
    @Nullable
    private TreeMap<Long, Pair<Long, String>> parsedTranscription = null;
    private int totalTranscriptionTimeSeconds = -1;

    // --- Subtitle Overlay State ---
    @Nullable
    private SubtitleOverlay subtitleOverlay;
    private volatile boolean isSubtitleOverlayShowing = false;

    // --- Runnables ---
    @Nullable
    private Runnable timerRunnable;
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
        // Double-checked locking for thread safety
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
     * If not busy or busy with a different task, prepare for the new operation (cancelling any old one).
     * Checks for a valid API key. If valid, shows a progress dialog and starts the asynchronous API call.
     * Handles success/failure callbacks to display results or errors.
     * Ensures any active transcription overlay is hidden before starting or showing results.
     *
     * @param context  Android Context, needed for UI operations (Dialogs, Toasts). Must be a UI context.
     * @param videoUrl The URL of the video to summarize. Must be a valid, non-placeholder URL.
     */
    public void startSummarization(@NonNull Context context, @NonNull final String videoUrl) {
        ensureMainThread(() -> {
            hideTranscriptionOverlayInternal();

            if (Objects.equals(videoUrl, cachedSummaryVideoUrl) && cachedSummaryResult != null) {
                Logger.printDebug(() -> "Displaying cached summary: " + videoUrl);
                showSummaryDialog(context, cachedSummaryResult, totalSummarizationTimeSeconds);
                resetOperationStateInternal(OperationType.SUMMARIZE, false);
                return;
            }

            if (isBusyInternal(context, videoUrl, OperationType.SUMMARIZE)) return;

            prepareForNewOperationInternal(OperationType.SUMMARIZE, videoUrl);

            final String apiKey = Settings.GEMINI_API_KEY.get();
            if (isEmptyApiKey(apiKey)) {
                resetOperationStateInternal(OperationType.SUMMARIZE, true);
                return;
            }

            Logger.printDebug(() -> "Starting new summarization workflow: " + videoUrl);

            showProgressDialogInternal(context, OperationType.SUMMARIZE);

            GeminiUtils.getVideoSummary(videoUrl, apiKey, new GeminiUtils.Callback() {
                @Override
                public void onSuccess(String result) {
                    handleApiResponseInternal(context, OperationType.SUMMARIZE, videoUrl, result, null);
                }

                @Override
                public void onFailure(String error) {
                    handleApiResponseInternal(context, OperationType.SUMMARIZE, videoUrl, null, error);
                }
            });
        });
    }

    // endregion Public API Methods

    // region Internal State Management & Workflow Logic

    /**
     * Initiates the video transcription workflow using either Yandex VOT or Gemini API.
     * <p>
     * Determines target language based on settings (including "app" keyword).
     * Routes to Yandex (potentially with Gemini translation step) or direct Gemini transcription.
     * <p>
     * Checks cache first (parsed data for Yandex, raw text for Gemini).
     * If not cached, check if busy with the *same* task.
     * If not busy or busy with a different task, prepare for the new operation (cancelling any old one).
     * Check the necessary prerequisites (API key for Gemini, language support for Yandex).
     * Shows a progress dialog and starts the asynchronous workflow (Yandex polling or Gemini API call).
     * Handles success/failure callbacks to display results (overlay for Yandex/parsed Gemini, dialog for raw Gemini) or errors.
     * Ensures any previous transcription overlay is hidden before starting a new transcription or showing results.
     *
     * @param context  Android Context, needed for UI operations. Must be a UI context.
     * @param videoUrl The URL of the video to transcribe. Must be a valid, non-placeholder URL.
     */
    public void startTranscription(@NonNull Context context, @NonNull final String videoUrl) {
        ensureMainThread(() -> {
            final long videoLengthMs = VideoInformation.getVideoLength();
            final double durationSeconds = videoLengthMs > 0 ? videoLengthMs / 1000.0 : 0;

            hideTranscriptionOverlayInternal();

            // --- Cache Check (Checks for final parsed result) ---
            if (Objects.equals(videoUrl, cachedTranscriptionVideoUrl)) {
                boolean cacheDisplayed = false;
                // Check for previously parsed result (could be from Yandex direct or Yandex+Gemini)
                if (parsedTranscription != null && !parsedTranscription.isEmpty()) {
                    Logger.printDebug(() -> "Attempting display cached transcription overlay: " + videoUrl);
                    if (displayTranscriptionOverlayInternal()) {
                        Logger.printDebug(() -> "Cached transcription overlay display succeeded.");
                        showToastShort(str("revanced_gemini_transcription_parse_success"));
                        cacheDisplayed = true;
                    } else {
                        Logger.printException(() -> "Failed to display cached transcription overlay!", null);
                        showToastLong(str("revanced_gemini_error_overlay_display"));
                        clearTranscriptionCacheAndHideOverlay();
                    }
                }
                // Check for raw Gemini result (only relevant if Yandex was OFF last time)
                else if (!Settings.YANDEX_TRANSCRIBE_SUBTITLES.get() && cachedRawTranscription != null) {
                    Logger.printDebug(() -> "Displaying cached Gemini raw transcription dialog: " + videoUrl);
                    showTranscriptionResultDialogInternal(context, cachedRawTranscription, totalTranscriptionTimeSeconds);
                    cacheDisplayed = true;
                }

                if (cacheDisplayed) {
                    resetOperationStateInternal(OperationType.TRANSCRIBE, false);
                    return;
                }
                // If cache existed but wasn't usable (e.g., Yandex setting true but only raw Gemini cached), continue to fetch.
            }

            if (isBusyInternal(context, videoUrl, OperationType.TRANSCRIBE)) return;

            prepareForNewOperationInternal(OperationType.TRANSCRIBE, videoUrl);

            // --- Determine Target Language ---
            String targetLangCode;
            final String yandexSettingValue = Settings.YANDEX_TRANSCRIBE_SUBTITLES_LANGUAGE.get();

            if (APP_LANGUAGE_SETTING_KEY.equalsIgnoreCase(yandexSettingValue)) {
                // User wants app language, get code from main app language setting
                try {
                    AppLanguage appLangEnum = Settings.REVANCED_LANGUAGE.get();
                    targetLangCode = appLangEnum.getLanguage();
                    String finalTargetLangCode = targetLangCode;
                    Logger.printInfo(() -> "Yandex target language set to 'app', using app language code: " + finalTargetLangCode);
                } catch (Exception e) {
                    Logger.printException(() -> "Failed to get app language code when Yandex setting was 'app'. Falling back to English.", e);
                    targetLangCode = "en"; // Fallback if reading app language fails
                }
            } else {
                // Use the code directly from the Yandex setting
                targetLangCode = yandexSettingValue;
                String finalTargetLangCode1 = targetLangCode;
                Logger.printInfo(() -> "Using Yandex target language code from setting: " + finalTargetLangCode1);
            }

            // Validate the determined language code
            if (TextUtils.isEmpty(targetLangCode)) {
                showToastLong(str("revanced_yandex_error_no_language_selected"));
                resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                return;
            }

            // Store the final determined target code for use in callbacks
            determinedTargetLanguageCode = targetLangCode;
            intermediateLanguageCode = null; // Reset intermediate state

            // --- Determine Workflow ---
            final boolean useYandex = Settings.YANDEX_TRANSCRIBE_SUBTITLES.get();

            if (useYandex) {
                startYandexTranscriptionWorkflow(context, videoUrl, durationSeconds, Objects.requireNonNull(determinedTargetLanguageCode));
            } else {
                // Use direct Gemini transcription (will use app language via getLanguageName())
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
        Logger.printInfo(() -> "Starting new Yandex workflow: " + videoUrl + " (Final Target: " + finalTargetLangCode + ")");


        showProgressDialogInternal(context, OperationType.TRANSCRIBE);

        YandexVotUtils.getYandexSubtitlesWorkflowAsync(
                videoUrl, durationSeconds, finalTargetLangCode,
                new YandexVotUtils.SubtitleWorkflowCallback() {
                    @Override
                    public void onProcessingStarted(String statusMessage) {
                        ensureMainThread(() -> handleYandexStatusUpdate(context, videoUrl, statusMessage));
                    }

                    @Override
                    public void onIntermediateSuccess(String rawIntermediateJson, String receivedIntermediateLang) {
                        // Yandex returned intermediate (likely English) JSON, need Gemini translation
                        ensureMainThread(() -> {
                            if (!isOperationRelevant(OperationType.TRANSCRIBE, videoUrl)) {
                                handleIrrelevantResponseInternal(OperationType.TRANSCRIBE, videoUrl, true); // Yandex part succeeded
                                return;
                            }
                            Logger.printInfo(() -> "Yandex intermediate success (Lang: " + receivedIntermediateLang + "). Starting Gemini translation step to " + determinedTargetLanguageCode);

                            intermediateLanguageCode = receivedIntermediateLang; // Store the intermediate language

                            Locale targetLocale = getLocaleFromCode(determinedTargetLanguageCode); // Helper to get Locale from code
                            String targetLangName = getLanguageNameFromLocale(targetLocale); // Helper to get name from Locale

                            baseLoadingMessage = str("revanced_gemini_status_translating", targetLangName);
                            updateTimerMessageInternal();

                            final String apiKey = Settings.GEMINI_API_KEY.get();
                            if (isEmptyApiKey(apiKey)) {
                                resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                                return;
                            }

                            // Call Gemini for translation
                            String cleanedJson = YandexVotUtils.stripTokensFromYandexJson(rawIntermediateJson);

                            GeminiUtils.translateYandexJson(
                                    cleanedJson,
                                    targetLangName,
                                    apiKey,
                                    new GeminiUtils.Callback() {
                                        @Override
                                        public void onSuccess(String translatedJson) {
                                            handleGeminiTranslationSuccess(videoUrl, translatedJson);
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            handleGeminiTranslationFailure(videoUrl, error);
                                        }
                                    }
                            );
                        });
                    }

                    @Override
                    public void onFinalSuccess(TreeMap<Long, Pair<Long, String>> parsedData) {
                        ensureMainThread(() -> handleYandexDirectSuccess(videoUrl, parsedData));
                    }

                    @Override
                    public void onFinalFailure(String errorMessage) {
                        ensureMainThread(() -> handleYandexWorkflowFailure(videoUrl, errorMessage));
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
            if (!isOperationRelevant(OperationType.TRANSCRIBE, videoUrl)) {
                Logger.printDebug(() -> "Yandex status update ignored, operation no longer relevant.");
                return;
            }

            if (intermediateLanguageCode != null) {
                Logger.printDebug(() -> "Yandex status update ignored, Gemini translation step is active.");
                return;
            }

            // Only update UI if message changed and not translating
            if (!Objects.equals(statusMessage, lastYandexStatusMessage)) {
                lastYandexStatusMessage = statusMessage;
                isWaitingForYandexRetry = true;
                baseLoadingMessage = str("revanced_gemini_loading_yandex_transcribe") + "\n(" + statusMessage + ")";
                Logger.printDebug(() -> "Yandex status update (UI): " + statusMessage);

                if (progressDialog != null && progressDialog.isShowing() && !isProgressDialogMinimized) {
                    updateTimerMessageInternal();
                } else if (progressDialog == null && !isProgressDialogMinimized) {
                    Logger.printDebug(() -> "Yandex status update: Progress dialog was null/hidden, attempting to show again.");
                    showProgressDialogInternal(context, OperationType.TRANSCRIBE);
                }
            } else {
                // Even if the message is the same, update baseLoadingMessage in case dialog is re-shown later
                // Only update if not translating
                if (intermediateLanguageCode == null) {
                    baseLoadingMessage = str("revanced_gemini_loading_yandex_transcribe") + "\n(" + statusMessage + ")";
                }
                Logger.printDebug(() -> "Yandex status update (no UI change needed): " + statusMessage);
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
            if (!isOperationRelevant(OperationType.TRANSCRIBE, videoUrl)) {
                handleIrrelevantResponseInternal(OperationType.TRANSCRIBE, videoUrl, true); // Gemini part succeeded technically
                return;
            }
            YandexVotUtils.forceReleaseWorkflowLock(videoUrl);

            Logger.printInfo(() -> "Gemini translation SUCCESS. Parsing translated JSON for lang " + determinedTargetLanguageCode);
            dismissProgressDialogInternal();

            // Parse the *translated* JSON using Yandex parser
            try {
                // Use the public static parser from YandexVotUtils
                TreeMap<Long, Pair<Long, String>> finalParsedData = YandexVotUtils.parseYandexJsonSubtitles(translatedJson);

                // Validate parsing result
                if (finalParsedData == null) {
                    // Handle cases where Gemini returns invalid/unparseable JSON
                    Logger.printException(() -> "Gemini returned unparseable JSON after translation.", null);
                    showToastLong(str("revanced_gemini_error_translation_parse_failed"));
                    resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                    return;
                }
                // Optional: Check if parsing yielded empty map from non-empty JSON
                if (finalParsedData.isEmpty() && !translatedJson.trim().isEmpty() && !(translatedJson.trim().equals("[]") || translatedJson.trim().equals("{}"))) {
                    Logger.printInfo(() -> "Gemini translation resulted in empty parsed data from potentially non-empty JSON input. Raw: " + translatedJson.substring(0, Math.min(200, translatedJson.length())));
                    // Proceed with empty data, could be valid (e.g., no text found in source).
                }

                // Cache the final translated & parsed result
                parsedTranscription = finalParsedData;
                cachedTranscriptionVideoUrl = videoUrl;
                cachedRawTranscription = null; // Not applicable here
                totalTranscriptionTimeSeconds = calculateElapsedTimeSeconds(); // Total time for Yandex + Gemini

                Logger.printDebug(() -> "Attempting display final translated overlay...");
                if (displayTranscriptionOverlayInternal()) {
                    Logger.printDebug(() -> "Final translated overlay display succeeded.");
                    resetOperationStateInternal(OperationType.TRANSCRIBE, false);
                    showToastShort(str("revanced_gemini_transcription_parse_success"));
                } else {
                    Logger.printException(() -> "Failed to display final translated overlay!", null);
                    showToastLong(str("revanced_gemini_error_overlay_display"));
                    resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                }
            } catch (Exception e) {
                Logger.printException(() -> "Failed to parse Gemini's translated JSON response.", e);
                showToastLong(str("revanced_gemini_error_translation_parse_failed"));
                resetOperationStateInternal(OperationType.TRANSCRIBE, true);
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
            if (!isOperationRelevant(OperationType.TRANSCRIBE, videoUrl)) {
                handleIrrelevantResponseInternal(OperationType.TRANSCRIBE, videoUrl, false); // Gemini part failed
                return;
            }
            Logger.printException(() -> "Gemini translation FAILED: " + error, null);
            YandexVotUtils.forceReleaseWorkflowLock(videoUrl);

            dismissProgressDialogInternal();
            showToastLong(str("revanced_gemini_error_translation_failed", error));
            resetOperationStateInternal(OperationType.TRANSCRIBE, true);
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
            lastYandexStatusMessage = null;
            isWaitingForYandexRetry = false;
            intermediateLanguageCode = null; // Ensure intermediate state is clear

            if (!isOperationRelevant(OperationType.TRANSCRIBE, videoUrl)) {
                handleIrrelevantResponseInternal(OperationType.TRANSCRIBE, videoUrl, true);
                return;
            }
            Logger.printInfo(() -> "Yandex Workflow SUCCEEDED directly for " + videoUrl + " (Lang: " + determinedTargetLanguageCode + ")");

            dismissProgressDialogInternal();

            if (parsedData == null) {
                Logger.printException(() -> "Yandex success callback received null data (parse failed?)!", null);
                handleYandexWorkflowFailure(videoUrl, str("revanced_yandex_error_subs_parsing_failed"));
                return;
            }
            if (parsedData.isEmpty()) {
                Logger.printInfo(() -> "Yandex success callback received empty subtitle map for " + determinedTargetLanguageCode);
                // Proceed to show empty overlay
            }

            // Cache the successful Yandex result (which is the final result here)
            parsedTranscription = parsedData;
            cachedTranscriptionVideoUrl = videoUrl;
            cachedRawTranscription = null;
            totalTranscriptionTimeSeconds = calculateElapsedTimeSeconds(); // Yandex time only

            Logger.printDebug(() -> "Attempting display final Yandex overlay...");
            if (displayTranscriptionOverlayInternal()) {
                Logger.printDebug(() -> "Final Yandex overlay display succeeded.");
                resetOperationStateInternal(OperationType.TRANSCRIBE, false);
                showToastShort(str("revanced_gemini_transcription_parse_success"));
            } else {
                Logger.printException(() -> "Failed to display final Yandex overlay after success!", null);
                showToastLong(str("revanced_gemini_error_overlay_display"));
                resetOperationStateInternal(OperationType.TRANSCRIBE, true);
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
            lastYandexStatusMessage = null;
            isWaitingForYandexRetry = false;
            intermediateLanguageCode = null; // Ensure intermediate state is clear

            if (!isOperationRelevant(OperationType.TRANSCRIBE, videoUrl)) {
                handleIrrelevantResponseInternal(OperationType.TRANSCRIBE, videoUrl, false);
                return;
            }
            Logger.printException(() -> "Yandex Workflow FAILED for " + videoUrl + ": " + errorMessage, null);

            dismissProgressDialogInternal();
            showToastLong(errorMessage); // Show specific Yandex/workflow error
            resetOperationStateInternal(OperationType.TRANSCRIBE, true);
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
        Logger.printInfo(() -> "Starting new Gemini direct transcription workflow: " + videoUrl);

        final String apiKey = Settings.GEMINI_API_KEY.get();
        if (isEmptyApiKey(apiKey)) {
            resetOperationStateInternal(OperationType.TRANSCRIBE, true);
            return;
        }

        showProgressDialogInternal(context, OperationType.TRANSCRIBE);

        GeminiUtils.getVideoTranscription(videoUrl, apiKey, new GeminiUtils.Callback() {
            @Override
            public void onSuccess(String result) {
                handleApiResponseInternal(context, OperationType.TRANSCRIBE, videoUrl, result, null);
            }

            @Override
            public void onFailure(String error) {
                handleApiResponseInternal(context, OperationType.TRANSCRIBE, videoUrl, null, error);
            }
        });
    }

    /**
     * Checks if the manager is currently busy with the specified operation for the specified URL.
     * If busy with the *exact same* task, it may re-show a minimized progress dialog.
     * If busy with a *different* task, it returns false, allowing the new task request to proceed
     * (which will then trigger cancellation of the old task via {@link #prepareForNewOperationInternal}).
     * Must be called on the Main Thread.
     *
     * @param context      Context for UI actions (Toast, Dialog).
     * @param requestedUrl The video URL being requested.
     * @param requestedOp  The operation type being requested.
     * @return {@code true} if the manager is busy with this specific task, {@code false} otherwise.
     */
    @MainThread
    private boolean isBusyInternal(@NonNull Context context, @NonNull String requestedUrl, @NonNull OperationType requestedOp) {
        if (currentOperation == OperationType.NONE) {
            return false; // Not busy
        }

        if (currentOperation == requestedOp && Objects.equals(currentVideoUrl, requestedUrl)) {
            // Busy with this exact task
            Logger.printDebug(() -> requestedOp + " already running for " + requestedUrl);
            if (isProgressDialogMinimized) {
                // Re-show minimized dialog
                Logger.printDebug(() -> "Re-showing minimized dialog for " + requestedOp);
                isProgressDialogMinimized = false;
                rebuildBaseLoadingMessage(requestedOp); // Ensure the message is correct
                showProgressDialogInternal(context, requestedOp); // Re-show dialog
            } else if (isWaitingForYandexRetry && progressDialog != null && progressDialog.isShowing()) {
                // Yandex task is active and dialog visible, ensure timer updates continue
                Logger.printDebug(() -> "Yandex task active and dialog showing, ensuring timer update.");
                updateTimerMessageInternal();
            }
            return true; // Indicate busy with this task
        } else {
            // Busy, but with a different task (different type or different URL)
            Logger.printInfo(() -> "Request for " + requestedOp + " (" + requestedUrl + ") received while busy with " + currentOperation + " (" + currentVideoUrl + "). Proceeding to cancel old task.");
            // Return false to allow the new task to start.
            // prepareForNewOperationInternal will handle cancelling the old one.
            return false;
        }
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
            // Check if we are in the intermediate state (Yandex finished, waiting for Gemini)
            boolean isTranslating = isUsingYandex && intermediateLanguageCode != null && determinedTargetLanguageCode != null;

            if (isTranslating) {
                // We are in the Gemini translation phase
                Locale targetLocale = getLocaleFromCode(determinedTargetLanguageCode);
                String targetLangName = getLanguageNameFromLocale(targetLocale);
                baseLoadingMessage = str("revanced_gemini_status_translating", targetLangName);
            } else if (isUsingYandex) {
                // Yandex polling phase (intermediateLanguageCode is null)
                baseLoadingMessage = str("revanced_gemini_loading_yandex_transcribe")
                        + (isWaitingForYandexRetry && lastYandexStatusMessage != null ? "\n(" + lastYandexStatusMessage + ")" : "");
            } else {
                // Direct Gemini transcription phase
                baseLoadingMessage = str("revanced_gemini_loading_transcribe");
            }
        } else if (opType == OperationType.SUMMARIZE) {
            // Simple message for Gemini Summarize
            baseLoadingMessage = str("revanced_gemini_loading_summarize");
        } else {
            baseLoadingMessage = str("revanced_gemini_loading_default");
        }
        Logger.printDebug(() -> "Rebuilt baseLoadingMessage: " + baseLoadingMessage);
    }

    /**
     * Prepares the manager for a new operation.
     * MUST be called after {@link #isBusyInternal(Context, String, OperationType)} returns false and before starting async work.
     * It cancels any currently running operation, cleans up its UI (dialog, overlay),
     * clears the associated state, and then sets up the state for the *new* operation.
     * Must be called on the Main Thread.
     *
     * @param newOperationType The type of the new operation being started.
     * @param newVideoUrl      The URL for the new operation.
     */
    @MainThread
    private void prepareForNewOperationInternal(@NonNull OperationType newOperationType, @NonNull String newVideoUrl) {
        Logger.printDebug(() -> "Preparing for new operation: " + newOperationType + " for URL: " + newVideoUrl);

        // Cancel and fully reset the state/UI of any PREVIOUS operation.
        if (currentOperation != OperationType.NONE) {
            Logger.printInfo(() -> "Cancelling previous operation (" + currentOperation + " for " + currentVideoUrl + ") to start new one.");
            resetOperationStateInternal(currentOperation, true);
        } else {
            Logger.printDebug(() -> "Manager was idle. No previous operation to cancel.");
        }

        // Set up state for the NEW operation.
        isCancelled = false;
        isProgressDialogMinimized = false;
        isWaitingForYandexRetry = false;
        lastYandexStatusMessage = null;
        baseLoadingMessage = null;
        determinedTargetLanguageCode = null;
        intermediateLanguageCode = null;

        currentOperation = newOperationType;
        currentVideoUrl = newVideoUrl;
        startTimeMillis = System.currentTimeMillis(); // Start timer for the new operation

        Logger.printDebug(() -> "State prepared for new operation: " + currentOperation + " for " + currentVideoUrl);
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
        if (!isOperationRelevant(opType, videoUrl)) {
            handleIrrelevantResponseInternal(opType, videoUrl, result != null);
            return;
        }
        Logger.printDebug(() -> "Handling relevant direct Gemini response for " + opType + ".");

        dismissProgressDialogInternal();

        if (error != null) {
            Logger.printException(() -> "Direct Gemini " + opType + " failed for " + videoUrl + ": " + error, null);
            showToastLong(str("revanced_gemini_error_api_failed", error));
            resetOperationStateInternal(opType, true);
            return;
        }

        if (result != null) {
            Logger.printInfo(() -> "Direct Gemini " + opType + " success for " + videoUrl);
            int time = calculateElapsedTimeSeconds();

            hideTranscriptionOverlayInternal();

            if (opType == OperationType.SUMMARIZE) {
                totalSummarizationTimeSeconds = time;
                cachedSummaryVideoUrl = videoUrl;
                cachedSummaryResult = result;
                showSummaryDialog(context, result, totalSummarizationTimeSeconds);
                resetOperationStateInternal(opType, false);
            } else if (opType == OperationType.TRANSCRIBE) {
                totalTranscriptionTimeSeconds = time;
                cachedTranscriptionVideoUrl = videoUrl;
                cachedRawTranscription = result;
                parsedTranscription = null;
                showTranscriptionResultDialogInternal(context, result, totalTranscriptionTimeSeconds);
                resetOperationStateInternal(opType, false);
            }
        } else {
            // Should not happen if error is null, but handle defensively
            Logger.printException(() -> "Direct Gemini " + opType + " received null response without error for " + videoUrl, null);
            showToastLong(str("revanced_gemini_error_unknown"));
            resetOperationStateInternal(opType, true);
        }
    }

    /**
     * Handles an API response (Gemini or Yandex) that arrives after the operation
     * it belongs to is no longer the active one (e.g., canceled or replaced).
     * Logs the event and cleans up any orphaned background tasks.
     *
     * @param opType     The type of the operation that finished irrelevantly.
     * @param videoUrl   The URL associated with the irrelevant operation.
     * @param wasSuccess True if the original operation succeeded, false if it failed.
     */
    private void handleIrrelevantResponseInternal(@NonNull OperationType opType, @NonNull String videoUrl, boolean wasSuccess) {
        String status = wasSuccess ? "succeeded" : "failed";
        String source = (opType == OperationType.SUMMARIZE) ? "Gemini Summary" :
                (opType == OperationType.TRANSCRIBE && Settings.YANDEX_TRANSCRIBE_SUBTITLES.get()) ? "Yandex/Gemini Transcription" :
                        "Gemini Transcription";

        String reason = isCancelled ? "operation was cancelled" :
                (currentOperation != OperationType.NONE ? "new operation (" + currentOperation + " for " + currentVideoUrl + ") started" : "operation finished/reset");


        Logger.printDebug(() -> "Ignoring irrelevant " + source + " response (" + status + ") for " + videoUrl + " because " + reason + ".");

        if (opType == OperationType.TRANSCRIBE && Settings.YANDEX_TRANSCRIBE_SUBTITLES.get()) {
            Logger.printDebug(() -> "Irrelevant Yandex response detected. Forcing workflow cleanup for: " + videoUrl);
            YandexVotUtils.forceReleaseWorkflowLock(videoUrl);
        }
        // No state changes needed here, the relevant reset happened during cancellation or preparation for the new task.
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
        Logger.printDebug(() -> String.format("Resetting state for %s, clearCacheAndUI=%b. (Current logical op before reset: %s)", opBeingReset, clearCacheAndUI, currentOperation));

        stopTimerInternal();

        // Only reset core state flags if the operation being reset IS the currently active one.
        if (currentOperation == opBeingReset) {
            Logger.printDebug(() -> "Resetting core flags (currentOp=NONE) as opBeingReset matches currentOperation.");
            isCancelled = false;
            isProgressDialogMinimized = false;
            currentOperation = OperationType.NONE;
            currentVideoUrl = null;
            startTimeMillis = -1;
            baseLoadingMessage = null;
            determinedTargetLanguageCode = null;
            intermediateLanguageCode = null;
        } else {
            Logger.printDebug(() -> "Skipping core flag reset - current logical operation (" + currentOperation + ") differs from opBeingReset (" + opBeingReset + ").");
        }

        // Cache and UI Clearing Logic
        if (clearCacheAndUI) {
            Logger.printDebug(() -> "clearCacheAndUI=true: Clearing relevant caches and UI...");

            dismissProgressDialogInternal();

            if (opBeingReset == OperationType.SUMMARIZE || opBeingReset == OperationType.NONE) {
                clearSummaryCache();
            }
            if (opBeingReset == OperationType.TRANSCRIBE || opBeingReset == OperationType.NONE) {
                clearTranscriptionCacheAndHideOverlay();
            }

            // This ensures a cancelled Yandex poll doesn't leave stale state for the next operation,
            // even if currentOperation was quickly changed by a new request.
            isWaitingForYandexRetry = false;
            lastYandexStatusMessage = null;
            Logger.printDebug(() -> "Unconditionally reset Yandex polling state flags due to clearCacheAndUI=true.");
        } else {
            Logger.printDebug(() -> "clearCacheAndUI=false: Preserving caches and potentially visible UI (overlay).");
        }
    }

    /**
     * Clears the summary cache variables.
     */
    private void clearSummaryCache() {
        totalSummarizationTimeSeconds = -1;
        cachedSummaryVideoUrl = null;
        cachedSummaryResult = null;
        Logger.printDebug(() -> "Summary cache cleared.");
    }

    // endregion Internal State Management & Workflow Logic

    // region Utility Methods

    /**
     * Clears all transcription-related cache variables AND ensures the overlay is hidden and destroyed.
     * Must be called on the Main Thread because it triggers UI removal.
     */
    @MainThread
    private void clearTranscriptionCacheAndHideOverlay() {
        Logger.printDebug(() -> "Clearing transcription cache and ensuring overlay is hidden.");
        hideTranscriptionOverlayInternal();

        // Clear cache variables
        parsedTranscription = null;
        cachedRawTranscription = null;
        cachedTranscriptionVideoUrl = null;
        totalTranscriptionTimeSeconds = -1;
        Logger.printDebug(() -> "Transcription cache cleared.");
    }

    /**
     * Checks if the provided Gemini API key is empty.
     * Shows a toast if empty.
     *
     * @param key The API key to check.
     * @return {@code true} if the key is empty, {@code false} otherwise.
     */
    private boolean isEmptyApiKey(@Nullable String key) {
        if (TextUtils.isEmpty(key)) {
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
        if (startTimeMillis <= 0) return -1;
        return (int) ((System.currentTimeMillis() - startTimeMillis) / 1000);
    }

    /**
     * Checks if the current state of the manager matches the expected operation type and URL,
     * and that the operation hasn't been canceled. Used to validate callbacks.
     *
     * @param expectedOp  The expected operation type.
     * @param expectedUrl The expected video URL.
     * @return {@code true} if the current state matches and is not canceled, {@code false} otherwise.
     */
    private boolean isOperationRelevant(@NonNull OperationType expectedOp, @NonNull String expectedUrl) {
        OperationType currentOp = this.currentOperation;
        String currentUrl = this.currentVideoUrl;
        boolean cancelled = this.isCancelled;

        boolean relevant = currentOp == expectedOp && Objects.equals(currentUrl, expectedUrl) && !cancelled;

        if (!relevant) {
            Logger.printDebug(() -> String.format("Operation relevance check failed: expectedOp=%s, currentOp=%s, expectedUrl=%s, currentUrl=%s, isCancelled=%b",
                    expectedOp, currentOp, expectedUrl, currentUrl, cancelled));
        }
        return relevant;
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
            Logger.printDebug(() -> "getLocaleFromCode: received empty code, returning Locale.ENGLISH");
            return Locale.ENGLISH;
        }
        try {
            // Use Locale.Builder for better BCP 47 tag handling
            return new Locale.Builder().setLanguageTag(langCode.replace("_", "-")).build();
        } catch (Exception e) {
            Logger.printException(() -> "Could not create Locale from language tag: " + langCode + ". Returning Locale.ENGLISH.", e);
        }
        return Locale.ENGLISH; // Fallback on error
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
            Logger.printDebug(() -> "getLanguageNameFromLocale: received null locale, returning 'English'");
            return "English"; // Default name for null locale
        }
        try {
            String displayName = locale.getDisplayLanguage(Locale.ENGLISH);
            // Return display name if it's valid and different from the code (more user-friendly)
            if (!TextUtils.isEmpty(displayName) && !displayName.equalsIgnoreCase(locale.getLanguage())) {
                return displayName;
            } else {
                // If display name is empty or just the code, return the code capitalized
                String langCode = locale.getLanguage();
                if (!TextUtils.isEmpty(langCode)) {
                    return langCode.substring(0, 1).toUpperCase(Locale.ENGLISH) + langCode.substring(1);
                }
                Logger.printInfo(() -> "getLanguageNameFromLocale: Could not get valid display name or code for locale: " + locale);
            }
        } catch (Exception e) {
            Logger.printException(() -> "Failed to get display name for locale " + locale + ". Returning code.", e);
            String langCode = locale.getLanguage();
            if (!TextUtils.isEmpty(langCode)) {
                return langCode; // Return code on error
            }
        }
        Logger.printInfo(() -> "getLanguageNameFromLocale: Reached absolute fallback, returning 'English'");
        return "English"; // Absolute fallback
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

        if (isCancelled || isProgressDialogMinimized || currentOperation != opType) {
            Logger.printDebug(() -> "Progress dialog show aborted: isCancelled=" + isCancelled + ", isMinimized=" + isProgressDialogMinimized + ", currentOp=" + currentOperation + ", requestedOp=" + opType);
            return;
        }

        rebuildBaseLoadingMessage(opType);
        String initialMsg = (baseLoadingMessage != null && !baseLoadingMessage.isEmpty())
                ? baseLoadingMessage
                : str("revanced_gemini_loading_default");

        // Add time suffix
        String timeSuffix = "";
        int elapsedSeconds = calculateElapsedTimeSeconds();
        if (elapsedSeconds >= 0) {
            timeSuffix = "\n" + elapsedSeconds + "s";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(initialMsg + timeSuffix);
        builder.setCancelable(false);

        // Cancel Button
        builder.setNegativeButton(str("revanced_cancel"), (d, w) -> {
            final OperationType opAtClick = currentOperation;
            final String urlAtClick = currentVideoUrl;

            ensureMainThread(() -> {
                if (urlAtClick == null) {
                    Logger.printInfo(() -> "Cancel clicked but urlAtClick was null.");
                    try {d.dismiss();} catch (Exception ignored) {}
                    return;
                }

                // Check relevance *at the time of the click*
                if (isOperationRelevant(opAtClick, urlAtClick)) {
                    Logger.printInfo(() -> opAtClick + " cancelled by user for " + urlAtClick);
                    isCancelled = true;

                    if (opAtClick == OperationType.TRANSCRIBE && Settings.YANDEX_TRANSCRIBE_SUBTITLES.get()) {
                        YandexVotUtils.forceReleaseWorkflowLock(urlAtClick);
                    }

                    if (opAtClick == OperationType.SUMMARIZE || (opAtClick == OperationType.TRANSCRIBE && !Settings.YANDEX_TRANSCRIBE_SUBTITLES.get())) {
                        GeminiUtils.cancelCurrentTask();
                    }

                    isWaitingForYandexRetry = false;
                    lastYandexStatusMessage = null;

                    // Note: Yandex workflow cancellation needs to be handled within YandexVotUtils if possible,
                    // otherwise, we just rely on the isOperationRelevant checks in its callbacks.

                    // Reset state fully on manual cancel (clears UI, cache)
                    resetOperationStateInternal(opAtClick, true);
                    showToastShort(str("revanced_gemini_cancelled"));
                } else {
                    Logger.printDebug(() -> "Cancel button clicked for irrelevant dialog. Dismissing.");
                    try {d.dismiss();} catch (Exception ignored) {}
                    // Do not reset state here, as the *current* operation might be valid
                }
            });
        });

        // Minimize Button
        builder.setNeutralButton(str("revanced_minimize"), (d, w) -> {
            final OperationType opAtClick = currentOperation;
            final String urlAtClick = currentVideoUrl;

            ensureMainThread(() -> {
                assert urlAtClick != null;
                if (isOperationRelevant(opAtClick, urlAtClick) && !isProgressDialogMinimized) {
                    Logger.printInfo(() -> opAtClick + " progress dialog minimized for " + urlAtClick);
                    isProgressDialogMinimized = true;
                    stopTimerInternal(); // Stop UI updates while minimized
                    dismissProgressDialogInternal();
                    // State remains active, just UI is hidden
                } else {
                    Logger.printDebug(() -> "Minimize button clicked for irrelevant or already minimized dialog. Dismissing.");
                    try {d.dismiss();} catch (Exception ignored) {}
                }
            });
        });

        try {
            progressDialog = builder.create();
            progressDialog.show();
            startTimerInternal();

            Logger.printDebug(() -> "Progress dialog shown for " + opType);
        } catch (Exception e) {
            Logger.printException(() -> "Error showing progress dialog for " + opType, e);
            progressDialog = null;
            ensureMainThread(() -> {
                if (isOperationRelevant(opType, Objects.requireNonNull(currentVideoUrl))) {
                    showToastLong(str("revanced_gemini_error_dialog_show") + ": " + e.getMessage());
                    isCancelled = true;
                    if (opType == OperationType.SUMMARIZE || (opType == OperationType.TRANSCRIBE && !Settings.YANDEX_TRANSCRIBE_SUBTITLES.get())) {
                        GeminiUtils.cancelCurrentTask();
                    }
                    resetOperationStateInternal(opType, true);
                }
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

        AlertDialog dialog = this.progressDialog; // Use local copy for thread safety
        if (dialog != null) {
            if (dialog.isShowing()) {
                try {
                    dialog.dismiss();
                    Logger.printDebug(() -> "Progress dialog dismissed.");
                } catch (Exception e) {
                    Logger.printException(() -> "Error dismissing progress dialog", e);
                }
            }
            this.progressDialog = null;
        }
        // Do NOT reset isProgressDialogMinimized here. That flag indicates the *logical* state,
        // managed by the minimize button and prepare/reset methods.
    }

    /**
     * Displays the summary result in an AlertDialog.
     * Must be called on the Main Thread.
     *
     * @param context Context for creating the dialog.
     * @param summary The summary text.
     * @param seconds Time taken for the operation.
     */
    @MainThread
    private void showSummaryDialog(@NonNull Context context, @NonNull String summary, int seconds) {
        String timeMsg = (seconds >= 0) ? "\n\n" + str("revanced_gemini_time_taken", seconds) : "";
        String msg = summary + timeMsg;
        new AlertDialog.Builder(context)
                .setTitle(str("revanced_gemini_summary_title"))
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .setNeutralButton(str("revanced_copy"), (d, w) -> setClipboard(context, summary, str("revanced_gemini_copy_success")))
                .setCancelable(true)
                .show();
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
     */
    @MainThread
    private void showTranscriptionResultDialogInternal(@NonNull Context context, @NonNull String rawTranscription, int seconds) {
        Logger.printDebug(() -> "Showing raw Gemini transcription dialog.");
        String timeMsg = (seconds >= 0) ? "\n\n" + str("revanced_gemini_time_taken", seconds) : "";
        String msg = rawTranscription + timeMsg;

        final String dialogVideoUrl = currentVideoUrl;

        new AlertDialog.Builder(context)
                .setTitle(str("revanced_gemini_transcription_result_title"))
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .setNegativeButton(str("revanced_copy"), (d, w) -> setClipboard(context, rawTranscription, str("revanced_gemini_copy_success")))
                .setNeutralButton(str("revanced_gemini_transcription_parse_button"), (dialog, which) -> {
                    // Ensure this action is still relevant when the button is clicked
                    ensureMainThread(() -> {
                        Logger.printDebug(() -> "Parse button clicked (Gemini raw). Attempting parse and display overlay.");

                        // Ensure any previous overlay is gone before attempting parse/display
                        hideTranscriptionOverlayInternal();

                        // Pass the exact text and URL associated with this dialog to the parser.
                        // This avoids race conditions where the singleton cache or currentUrl might have changed or been cleared.
                        String urlToUse = dialogVideoUrl != null ? dialogVideoUrl : "";
                        parseAndShowTranscriptionInternal(rawTranscription, urlToUse);

                        // Close dialog since we are moving to overlay
                        try { dialog.dismiss(); } catch (Exception ignored) {}
                    });
                })
                .show();
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
        Logger.printDebug(() -> "Parsing and showing Gemini transcription from dialog input...");

        if (TextUtils.isEmpty(rawText)) {
            Logger.printException(() -> "Cannot parse raw Gemini data, input text is empty.", null);
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
            parsedTranscription = null;
            return;
        }

        // Check if parsing succeeded but yielded no results
        if (parsedData.isEmpty()) {
            if (rawText.trim().isEmpty()) {
                Logger.printInfo(() -> "Raw Gemini transcription was empty, parsed to empty map.");
            } else {
                Logger.printException(() -> "Parsed Gemini data is null or empty after parsing non-empty raw text.", null);
                showToastLong(str("revanced_gemini_error_transcription_parse"));
            }
            parsedTranscription = parsedData;
            cachedTranscriptionVideoUrl = videoUrl;
            cachedRawTranscription = rawText;
            hideTranscriptionOverlayInternal();
            return;
        }

        // Successfully parsed Gemini data
        Logger.printDebug(() -> "Successfully parsed " + parsedData.size() + " Gemini entries.");

        parsedTranscription = parsedData;
        cachedTranscriptionVideoUrl = videoUrl;
        cachedRawTranscription = rawText;

        // Attempt to display using the overlay
        if (displayTranscriptionOverlayInternal()) {
            Logger.printInfo(() -> "Successfully parsed and displayed Gemini transcription overlay.");
            showToastShort(str("revanced_gemini_transcription_parse_success"));

            // Ensure state flags are clean (though they should be already)
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
            Logger.printInfo(() -> "parseGeminiTranscription received empty text.");
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

                    // Basic validation: end time > start time and the text is not empty
                    if (et > st && !trimmedText.isEmpty()) {
                        map.put(st, new Pair<>(et, trimmedText));
                    } else {
                        Logger.printDebug(() -> "Skipping invalid Gemini line (timing=" + (et > st) + ", emptyText=" + trimmedText.isEmpty() + "): " + trimmedLine);
                    }
                } catch (NumberFormatException e) {
                    Logger.printException(() -> "Number format error parsing Gemini line: " + trimmedLine, e);
                } catch (NullPointerException e) {
                    Logger.printException(() -> "Regex group missing in Gemini line (unexpected format): " + trimmedLine, e);
                } catch (Exception e) {
                    Logger.printException(() -> "Unexpected error processing Gemini line: " + trimmedLine, e);
                }
            } else {
                Logger.printDebug(() -> "Skipping non-matching Gemini line format: " + trimmedLine);
            }
        }

        // If parsing yielded no results from a non-empty input, log it.
        if (map.isEmpty() && !rawText.trim().isEmpty()) {
            Logger.printInfo(() -> "Parsing Gemini text completed, but no valid subtitle entries were found.");
        }
        return map; // Return the map (possibly empty)
    }

    /**
     * Parses a millisecond string (potentially 1-3 digits) and returns its long value.
     * Handles padding implicitly by parsing and assuming subsequent units handle scale.
     * TODO: Consider instructing the AI to return raw milliseconds instead of the current format.
     *       Ai does not always follow instructions correctly anyway.
     *       Pros (of current format): Better transcription readability (non-subtitle mode).
     *
     * @param msString The millisecond string from the regex group.
     * @return Milliseconds as a long value (e.g., "1" -> 1, "12" -> 12, "123" -> 123). Returns 0 for null/empty/invalid.
     */
    private long parseAndPadMilliseconds(@Nullable String msString) {
        if (msString == null || msString.isEmpty()) return 0L;
        try {
            // Ensure string is max 3 digits and contains only digits before parsing
            String cleanedMs = msString.replaceAll("[^0-9]", "");
            if (cleanedMs.isEmpty()) return 0L;
            if (cleanedMs.length() > 3) {
                cleanedMs = cleanedMs.substring(0, 3);
                String finalCleanedMs = cleanedMs;
                Logger.printInfo(() -> "Millisecond string longer than 3 digits, truncated: " + msString + " -> " + finalCleanedMs);
            }

            long ms = Long.parseLong(cleanedMs);

            int originalLength = msString.length();
            if (originalLength == 1) {
                return ms * 100; // "1" -> 100
            } else if (originalLength == 2) {
                return ms * 10;  // "12" -> 120
            } else {
                return ms;     // "123" -> 123
            }
        } catch (NumberFormatException e) {
            Logger.printException(() -> "Could not parse millisecond string: " + msString, e);
            return 0L;
        }
    }

    /**
     * Creates, shows, and starts updating the subtitle overlay using {@link #parsedTranscription}.
     * Ensures any previous overlay is fully removed before creating the new one.
     * Must be called on the Main Thread.
     *
     * @return {@code true} if overlay was successfully created and shown, {@code false} otherwise.
     */
    @MainThread
    private boolean displayTranscriptionOverlayInternal() {
        Logger.printDebug(() -> "Attempting display transcription overlay...");

        // Check if we have parsed data (could be from Yandex direct, Yandex+Gemini, or Gemini direct)
        if (parsedTranscription == null) {
            Logger.printInfo(() -> "No parsed transcription data available to display overlay (parsedTranscription is null).");
            hideTranscriptionOverlayInternal();
            return false;
        }
        if (parsedTranscription.isEmpty()) {
            Logger.printInfo(() -> "Parsed transcription data is empty. Displaying overlay with placeholder.");
            // Proceed to show overlay, it will just display "..."
        }

        hideTranscriptionOverlayInternal();

        // Create and show the new overlay
        try {
            Logger.printDebug(() -> "Creating new SubtitleOverlay instance.");
            subtitleOverlay = new SubtitleOverlay();

            long currentTime = VideoInformation.getVideoTime();
            String initialText = findSubtitleTextForTimeInternal(currentTime);
            subtitleOverlay.updateText(initialText);

            // Show the overlay window (can throw BadTokenException etc.)
            subtitleOverlay.show();

            isSubtitleOverlayShowing = true;
            Logger.printInfo(() -> "Subtitle overlay displayed successfully.");

            startSubtitleUpdaterInternal();
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
     * The definitive method to hide and destroy the subtitle overlay UI element and associated state.
     * Stops updates, attempts direct view removal using the manager's context,
     * nullifies manager references, and sets flags.
     * Ensures UI actions happen on the main thread.
     */
    @MainThread
    private void hideTranscriptionOverlayInternal() {
        ensureMainThread(() -> {
            stopSubtitleUpdaterInternal();

            final SubtitleOverlay overlayInstance = this.subtitleOverlay;

            if (overlayInstance != null) {
                Logger.printInfo(() -> "Hiding and cleaning up transcription overlay instance.");
                final View viewToRemove = overlayInstance.getOverlayView();

                // Nullify references immediately
                this.isSubtitleOverlayShowing = false;
                this.subtitleOverlay = null;

                // Attempt to remove the view
                if (viewToRemove != null) {
                    try {
                        // Get WindowManager using the context stored in the overlay instance
                        WindowManager wm = (WindowManager) overlayInstance.context.getSystemService(Context.WINDOW_SERVICE);
                        if (wm != null) {
                            Logger.printDebug(() -> "Attempting removeViewImmediate directly from GeminiManager...");
                            wm.removeViewImmediate(viewToRemove);
                            Logger.printDebug(() -> "View removed successfully by manager's explicit call.");
                        } else {
                            Logger.printException(() -> "WindowManager was null when attempting removal from manager.");
                        }
                    } catch (IllegalArgumentException e) {
                        // View might already be removed if user action was faster or if destroy() was called first
                        Logger.printInfo(() -> "Manager's direct removal attempt failed (IllegalArgumentException - view likely already gone or never properly added).");
                    } catch (Exception e) {
                        Logger.printException(() -> "Manager's direct removal attempt failed unexpectedly.", e);
                    }
                } else {
                    Logger.printDebug(() -> "Manager skipping direct removal attempt (view was null in the tracked overlay instance).");
                }

                // Finally, call destroy on the original instance to release its resources
                overlayInstance.destroy();
            } else {
                if (isSubtitleOverlayShowing) {
                    Logger.printDebug(() -> "hideTranscriptionOverlayInternal: Overlay instance was null, ensuring flag is false.");
                    isSubtitleOverlayShowing = false;
                }
            }
            // Caches (parsedTranscription, etc.) are NOT cleared here. That's done by resetOperationState.
        });
    }

    /**
     * Starts the periodic runnable that updates the subtitle overlay text based on video time.
     * Must be called on the Main Thread.
     */
    @MainThread
    private void startSubtitleUpdaterInternal() {
        if (!isSubtitleOverlayShowing || parsedTranscription == null || subtitleOverlay == null) {
            Logger.printInfo(() -> "Subtitle updater preconditions not met. Stopping. isShowing=" + isSubtitleOverlayShowing + ", parsedDataNull=" + (parsedTranscription == null) + ", overlayNull=" + (subtitleOverlay == null));
            stopSubtitleUpdaterInternal();
            return;
        }

        stopSubtitleUpdaterInternal();
        Logger.printDebug(() -> "Starting subtitle updater.");

        subtitleUpdateRunnable = new Runnable() {
            private String lastTextSentToOverlay = null; // Track last text to avoid redundant UI updates

            @Override
            public void run() {
                SubtitleOverlay currentOverlay = subtitleOverlay;
                TreeMap<Long, Pair<Long, String>> currentParsedData = parsedTranscription;

                if (!isSubtitleOverlayShowing || currentParsedData == null || currentOverlay == null) {
                    Logger.printDebug(() -> "Stopping updater in runnable: state became invalid. isShowing=" + isSubtitleOverlayShowing + ", parsedDataNull=" + (currentParsedData == null) + ", overlayNull=" + (currentOverlay == null));
                    subtitleUpdateRunnable = null;
                    return;
                }

                long currentTime = VideoInformation.getVideoTime();
                if (currentTime < 0) {
                    // Don't update text, just reschedule
                    if (isSubtitleOverlayShowing) {
                        subtitleUpdateHandler.postDelayed(this, SUBTITLE_UPDATE_INTERVAL_MS);
                    } else {
                        subtitleUpdateRunnable = null;
                    }
                    return;
                }

                String textToShow = findSubtitleTextForTimeInternal(currentTime);

                // Only call updateText on the overlay if the text actually changed
                if (!Objects.equals(textToShow, lastTextSentToOverlay)) {
                    try {
                        currentOverlay.updateText(textToShow);
                        lastTextSentToOverlay = textToShow;
                    } catch (Exception e) {
                        Logger.printException(() -> "CRITICAL - Error updating overlay text view.", e);
                        hideTranscriptionOverlayInternal();
                        return;
                    }
                }

                // Schedule next run if overlay still should be showing
                if (isSubtitleOverlayShowing) {
                    subtitleUpdateHandler.postDelayed(this, SUBTITLE_UPDATE_INTERVAL_MS);
                } else {
                    Logger.printDebug(() -> "Updater loop ending: isSubtitleOverlayShowing is false.");
                    subtitleUpdateRunnable = null;
                }
            }
        };

        // Post the first execution
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
            Logger.printDebug(() -> "Stopping subtitle updater.");
            subtitleUpdateHandler.removeCallbacks(subtitleUpdateRunnable);
            subtitleUpdateRunnable = null;
        }
    }

    /**
     * Finds the subtitle text corresponding to the given playback time from {@link #parsedTranscription}.
     *
     * @param currentTimeMillis Current playback time in milliseconds.
     * @return The subtitle text if the time falls within a valid entry's range,
     * otherwise {@link #EMPTY_SUBTITLE_PLACEHOLDER}.
     */
    @NonNull
    private String findSubtitleTextForTimeInternal(long currentTimeMillis) {
        TreeMap<Long, Pair<Long, String>> currentParsed = this.parsedTranscription;
        if (currentParsed == null || currentParsed.isEmpty() || currentTimeMillis < 0) {
            return EMPTY_SUBTITLE_PLACEHOLDER;
        }

        // Find the latest entry whose start time is less than or equal to the current time
        Map.Entry<Long, Pair<Long, String>> entry = currentParsed.floorEntry(currentTimeMillis);

        if (entry != null) {
            // No need to read key/value again, use entry directly
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
        if (currentOperation == OperationType.NONE || isCancelled || isProgressDialogMinimized || startTimeMillis <= 0 || progressDialog == null || !progressDialog.isShowing()) {
            Logger.printDebug(() -> "Progress timer start preconditions not met. Stopping.");
            stopTimerInternal();
            return;
        }

        stopTimerInternal();
        Logger.printDebug(() -> "Starting progress timer.");


        timerRunnable = new Runnable() {
            @Override
            public void run() {
                AlertDialog currentDialog = progressDialog;
                if (currentOperation != OperationType.NONE && !isCancelled && !isProgressDialogMinimized && startTimeMillis > 0 && currentDialog != null && currentDialog.isShowing()) {
                    updateTimerMessageInternal();
                    timerHandler.postDelayed(this, 1000);
                } else {
                    Logger.printDebug(() -> "Stopping timer in runnable: state became invalid. Op=" + currentOperation + ", cancelled=" + isCancelled + ", minimized=" + isProgressDialogMinimized + ", dialogNull=" + (currentDialog == null));
                    timerRunnable = null;
                    // Do not call stopTimerInternal() from here to avoid potential recursion if state check fails repeatedly
                }
            }
        };

        updateTimerMessageInternal();
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    /**
     * Stops the periodic timer for the progress dialog.
     * Safe to call multiple times or if not running.
     * Must be called on the Main Thread.
     */
    @MainThread
    private void stopTimerInternal() {
        if (timerRunnable != null) {
            Logger.printDebug(() -> "Stopping progress timer.");
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    /**
     * Updates the message text in the progress dialog with the base message and elapsed time.
     * Must be called on the Main Thread.
     */
    @MainThread
    private void updateTimerMessageInternal() {
        AlertDialog currentDialog = this.progressDialog;

        if (currentDialog != null && currentDialog.isShowing() && !isCancelled && !isProgressDialogMinimized && startTimeMillis > 0) {
            int sec = calculateElapsedTimeSeconds();
            if (sec < 0) return;

            String time = "\n" + sec + "s";
            String base = (baseLoadingMessage != null && !baseLoadingMessage.isEmpty())
                    ? baseLoadingMessage
                    : str("revanced_gemini_loading_default");
            String msg = base + time;

            try {
                // Try direct TextView access (potentially slightly faster if ID is reliable)
                TextView tv = currentDialog.findViewById(android.R.id.message);
                if (tv != null) {
                    tv.setText(msg);
                } else {
                    // Fallback to standard setMessage if TextView ID fails
                    Logger.printDebug(() -> "Could not find TextView with android.R.id.message, using dialog.setMessage().");
                    currentDialog.setMessage(msg);
                }
            } catch (Exception e) {
                Logger.printException(() -> "Error updating progress dialog timer message", e);
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
