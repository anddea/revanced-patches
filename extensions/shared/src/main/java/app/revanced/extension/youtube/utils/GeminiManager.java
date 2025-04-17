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
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;
import org.jetbrains.annotations.NotNull;

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
            // Matches [HH:MM:SS.ms - HH:MM:SS.ms]: TEXT or [MM:SS.ms - MM:SS.ms]: TEXT
            "\\[(?:(\\d{2}):)?(\\d{2}):(\\d{2})\\.(\\d{1,3})\\s*-\\s*(?:(\\d{2}):)?(\\d{2}):(\\d{2})\\.(\\d{1,3})\\]:?\\s*(.*)"
    );
    private static final long SUBTITLE_UPDATE_INTERVAL_MS = 250; // How often to check for subtitle text updates
    private static final String EMPTY_SUBTITLE_PLACEHOLDER = "..."; // Displayed when no subtitle text is active

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
            final String videoTitle = VideoInformation.getVideoTitle();
            final long videoLengthMs = VideoInformation.getVideoLength();
            final double durationSeconds = videoLengthMs > 0 ? videoLengthMs / 1000.0 : 0;

            hideTranscriptionOverlayInternal();

            final boolean useYandex = Settings.YANDEX_TRANSCRIBE_SUBTITLES.get();
            final String targetLang = Settings.YANDEX_TRANSCRIBE_SUBTITLES_LANGUAGE.get();

            if (Objects.equals(videoUrl, cachedTranscriptionVideoUrl)) {
                boolean cacheDisplayed = false;
                if (useYandex && parsedTranscription != null && !parsedTranscription.isEmpty()) {
                    Logger.printDebug(() -> "Attempting display cached Yandex transcription overlay: " + videoUrl);
                    if (displayTranscriptionOverlayInternal()) {
                        Logger.printDebug(() -> "Cached Yandex overlay display succeeded.");
                        showToastShort(str("revanced_gemini_transcription_parse_success"));
                        cacheDisplayed = true;
                    } else {
                        Logger.printException(() -> "Failed to display cached Yandex overlay!", null);
                        showToastLong(str("revanced_gemini_error_overlay_display"));
                        resetOperationStateInternal(OperationType.TRANSCRIBE, true);
                    }
                } else if (!useYandex && cachedRawTranscription != null) {
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

            // Start the appropriate workflow (Yandex or Gemini)
            if (useYandex) {
                startYandexTranscriptionWorkflow(context, videoUrl, videoTitle, durationSeconds, targetLang);
            } else {
                startGeminiTranscriptionWorkflow(context, videoUrl);
            }
        });
    }

    /**
     * Starts the Yandex VOT transcription workflow.
     * Assumes preparation (prepareForNewOperationInternal) has already occurred.
     * Must be called on the Main Thread.
     *
     * @param context         UI Context.
     * @param videoUrl        Video URL.
     * @param videoTitle      Video Title.
     * @param durationSeconds Video duration.
     * @param targetLang      Target language code (raw from settings).
     */
    @MainThread
    private void startYandexTranscriptionWorkflow(@NonNull Context context, @NonNull String videoUrl, @Nullable String videoTitle, double durationSeconds, @NonNull String targetLang) {
        Logger.printInfo(() -> "Starting new Yandex workflow: " + videoUrl);

        if (!targetLang.equals("en") && !targetLang.equals("ru")) {
            showToastLong(str("revanced_yandex_error_language_not_supported_transcribe", targetLang));
            resetOperationStateInternal(OperationType.TRANSCRIBE, true);
            return;
        }

        showProgressDialogInternal(context, OperationType.TRANSCRIBE);

        YandexVotUtils.getYandexSubtitlesWorkflowAsync(
                videoUrl, videoTitle, durationSeconds, targetLang,
                new YandexVotUtils.SubtitleWorkflowCallback() {
                    @Override
                    public void onProcessingStarted(String statusMessage) {
                        handleYandexStatusUpdate(context, videoUrl, statusMessage);
                    }

                    @Override
                    public void onFinalSuccess(TreeMap<Long, Pair<Long, String>> parsedData) {
                        handleYandexSuccess(videoUrl, parsedData);
                    }

                    @Override
                    public void onFinalFailure(String errorMessage) {
                        handleYandexFailure(videoUrl, errorMessage);
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

            if (!Objects.equals(statusMessage, lastYandexStatusMessage)) {
                lastYandexStatusMessage = statusMessage;
                isWaitingForYandexRetry = true;
                baseLoadingMessage = str("revanced_gemini_loading_yandex_transcribe") + " (" + statusMessage + ")";
                Logger.printDebug(() -> "Yandex status update (UI): " + statusMessage);

                if (progressDialog != null && progressDialog.isShowing() && !isProgressDialogMinimized) {
                    updateTimerMessageInternal();
                } else if (progressDialog == null && !isProgressDialogMinimized) {
                    // Dialog might have been dismissed unexpectedly, try showing again
                    showProgressDialogInternal(context, OperationType.TRANSCRIBE);
                }
            } else {
                // Even if the message is the same, update baseLoadingMessage in case dialog is re-shown later
                baseLoadingMessage = str("revanced_gemini_loading_yandex_transcribe") + " (" + statusMessage + ")";
                Logger.printDebug(() -> "Yandex status update (no UI change needed): " + statusMessage);
            }
        });
    }

    /**
     * Handles the successful completion of the Yandex transcription workflow.
     * Caches the result, displays the overlay, and resets state.
     * Must run on the Main Thread.
     *
     * @param videoUrl   The URL for which the success applies.
     * @param parsedData The successfully parsed subtitle data.
     */
    @MainThread
    private void handleYandexSuccess(@NonNull String videoUrl, @Nullable TreeMap<Long, Pair<Long, String>> parsedData) {
        ensureMainThread(() -> {
            lastYandexStatusMessage = null;
            isWaitingForYandexRetry = false;

            if (!isOperationRelevant(OperationType.TRANSCRIBE, videoUrl)) {
                handleIrrelevantResponseInternal(OperationType.TRANSCRIBE, videoUrl, true);
                return;
            }
            Logger.printInfo(() -> "Yandex Workflow SUCCEEDED for " + videoUrl);

            dismissProgressDialogInternal();

            if (parsedData == null || parsedData.isEmpty()) {
                Logger.printException(() -> "Yandex success callback received null/empty data!", null);
                handleYandexFailure(videoUrl, str("revanced_yandex_error_internal_empty_data"));
                return;
            }

            // Cache the successful Yandex result
            parsedTranscription = parsedData;
            cachedTranscriptionVideoUrl = videoUrl;
            cachedRawTranscription = null; // Yandex provides parsed data directly
            totalTranscriptionTimeSeconds = calculateElapsedTimeSeconds();

            Logger.printDebug(() -> "Attempting display Yandex overlay...");
            if (displayTranscriptionOverlayInternal()) {
                Logger.printDebug(() -> "Yandex display overlay succeeded.");
                resetOperationStateInternal(OperationType.TRANSCRIBE, false);
                showToastShort(str("revanced_gemini_transcription_parse_success"));
            } else {
                Logger.printException(() -> "Failed to display Yandex overlay after success!", null);
                showToastLong(str("revanced_gemini_error_overlay_display"));
                resetOperationStateInternal(OperationType.TRANSCRIBE, true);
            }
        });
    }

    /**
     * Handles the failure of the Yandex transcription workflow.
     * Shows an error toast and resets state.
     * Must run on the Main Thread.
     *
     * @param videoUrl     The URL for which the failure applies.
     * @param errorMessage The error message describing the failure.
     */
    @MainThread
    private void handleYandexFailure(@NonNull String videoUrl, @NonNull String errorMessage) {
        ensureMainThread(() -> {
            lastYandexStatusMessage = null;
            isWaitingForYandexRetry = false;

            if (!isOperationRelevant(OperationType.TRANSCRIBE, videoUrl)) {
                handleIrrelevantResponseInternal(OperationType.TRANSCRIBE, videoUrl, false);
                return;
            }
            Logger.printException(() -> "Yandex Workflow FAILED for " + videoUrl + ": " + errorMessage, null);

            dismissProgressDialogInternal();
            showToastLong(errorMessage); // Show specific Yandex error
            resetOperationStateInternal(OperationType.TRANSCRIBE, true);
        });
    }

    /**
     * Starts the Gemini API transcription workflow.
     * Assumes preparation (prepareForNewOperationInternal) has already occurred.
     * Must be called on the Main Thread.
     *
     * @param context  UI Context.
     * @param videoUrl Video URL.
     */
    @MainThread
    private void startGeminiTranscriptionWorkflow(@NonNull Context context, @NonNull String videoUrl) {
        Logger.printInfo(() -> "Starting new Gemini transcription workflow: " + videoUrl);

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
     * used when re-showing a minimized dialog.
     * Must be called on the Main Thread.
     *
     * @param opType The operation type the dialog is for.
     */
    @MainThread
    private void rebuildBaseLoadingMessage(@NonNull OperationType opType) {
        boolean isYandex = opType == OperationType.TRANSCRIBE && Settings.YANDEX_TRANSCRIBE_SUBTITLES.get();
        if (isYandex) {
            // Include the last known Yandex status if polling was active
            baseLoadingMessage = str("revanced_gemini_loading_yandex_transcribe")
                    + (isWaitingForYandexRetry && lastYandexStatusMessage != null ? " (" + lastYandexStatusMessage + ")" : "");
        } else {
            // Simple message for Gemini Summarize or Transcribe
            String key = "revanced_gemini_loading_" + opType.name().toLowerCase();
            baseLoadingMessage = str(key);
        }
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

        currentOperation = newOperationType;
        currentVideoUrl = newVideoUrl;
        startTimeMillis = System.currentTimeMillis(); // Start timer for the new operation

        Logger.printDebug(() -> "State prepared for new operation: " + currentOperation + " for " + currentVideoUrl);
    }

    /**
     * Handles the result (success or failure) from the Gemini API callback.
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
        Logger.printDebug(() -> "Handling relevant Gemini response for " + opType + ".");

        dismissProgressDialogInternal();

        if (error != null) {
            Logger.printException(() -> "Gemini " + opType + " failed for " + videoUrl + ": " + error, null);
            showToastLong(str("revanced_gemini_error_api_failed", error));
            resetOperationStateInternal(opType, true);
            return;
        }

        if (result != null) {
            Logger.printInfo(() -> "Gemini " + opType + " success for " + videoUrl);
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
            Logger.printException(() -> "Gemini " + opType + " received null response without error for " + videoUrl, null);
            showToastLong(str("revanced_gemini_error_unknown"));
            resetOperationStateInternal(opType, true);
        }
    }

    /**
     * Handles an API response (Gemini or Yandex) that arrives after the operation
     * it belongs to is no longer the active one (e.g., canceled or replaced).
     * Log the event.
     *
     * @param opType     The type of the operation that finished irrelevantly.
     * @param videoUrl   The URL associated with the irrelevant operation.
     * @param wasSuccess True if the original operation succeeded, false if it failed.
     */
    private void handleIrrelevantResponseInternal(@NonNull OperationType opType, @NonNull String videoUrl, boolean wasSuccess) {
        String status = wasSuccess ? "succeeded" : "failed";
        String source = (opType == OperationType.SUMMARIZE) ? "Gemini Summary" :
                (opType == OperationType.TRANSCRIBE && Settings.YANDEX_TRANSCRIBE_SUBTITLES.get()) ? "Yandex Transcription" :
                        "Gemini Transcription";

        String reason = isCancelled ? "operation was cancelled" :
                "new operation (" + currentOperation + " for " + currentVideoUrl + ") started";

        Logger.printDebug(() -> "Ignoring irrelevant " + source + " response (" + status + ") for " + videoUrl + " because " + reason + ".");
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
        // This prevents a late callback from an old operation resetting the state of a new one.
        if (currentOperation == opBeingReset) {
            Logger.printDebug(() -> "Resetting core flags (currentOp=NONE) as opBeingReset matches currentOperation.");
            isCancelled = false;
            isProgressDialogMinimized = false;
            isWaitingForYandexRetry = false;
            lastYandexStatusMessage = null;
            currentOperation = OperationType.NONE;
            currentVideoUrl = null;
            startTimeMillis = -1;
            baseLoadingMessage = null;
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
        } else {
            Logger.printDebug(() -> "clearCacheAndUI=false: Preserving caches and potentially visible UI (overlay).");
            // If opBeingReset was TRANSCRIBE and clearCacheAndUI is false (i.e., success case leaving overlay visible),
            // the overlay instance (`subtitleOverlay`) and its showing flag (`isSubtitleOverlayShowing`) remain untouched here.
            // They are managed by display/hide calls. The core state (currentOperation=NONE) indicates the *manager* is idle,
            // even if the overlay UI persists from the last successful operation.
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
            final OperationType opAtClick = opType;
            final String urlAtClick = currentVideoUrl;
            ensureMainThread(() -> {
                assert urlAtClick != null;
                if (isOperationRelevant(opAtClick, urlAtClick)) {
                    Logger.printInfo(() -> currentOperation + " cancelled by user for " + currentVideoUrl);
                    isCancelled = true;
                    isWaitingForYandexRetry = false; // Stop potential Yandex polling state

                    if (currentOperation == OperationType.SUMMARIZE || (currentOperation == OperationType.TRANSCRIBE && !Settings.YANDEX_TRANSCRIBE_SUBTITLES.get())) {
                        GeminiUtils.cancelCurrentTask();
                    }

                    // Reset state fully on manual cancel (clears UI, cache)
                    resetOperationStateInternal(currentOperation, true);
                    showToastShort(str("revanced_gemini_cancelled"));
                } else {
                    Logger.printDebug(() -> "Cancel button clicked for irrelevant dialog. Dismissing.");
                    try {d.dismiss();} catch (Exception ignored) {}
                }
            });
        });

        // Minimize Button
        builder.setNeutralButton(str("revanced_minimize"), (d, w) -> {
            final OperationType opAtClick = opType;
            final String urlAtClick = currentVideoUrl;
            ensureMainThread(() -> {
                assert urlAtClick != null;
                if (isOperationRelevant(opAtClick, urlAtClick) && !isProgressDialogMinimized) {
                    Logger.printInfo(() -> currentOperation + " progress dialog minimized for " + currentVideoUrl);
                    isProgressDialogMinimized = true;
                    stopTimerInternal(); // Stop UI updates while minimized
                    dismissProgressDialogInternal();
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
                    if (currentOperation == OperationType.SUMMARIZE || (currentOperation == OperationType.TRANSCRIBE && !Settings.YANDEX_TRANSCRIBE_SUBTITLES.get())) {
                        GeminiUtils.cancelCurrentTask();
                    }
                    resetOperationStateInternal(currentOperation, true);
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

        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                try {
                    progressDialog.dismiss();
                    Logger.printDebug(() -> "Progress dialog dismissed.");
                } catch (Exception e) {
                    Logger.printException(() -> "Error dismissing progress dialog", e);
                }
            }
            progressDialog = null;
        }
        // Do NOT reset isProgressDialogMinimized here. It's reset when showing or fully resetting state.
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
        final OperationType dialogOpType = currentOperation; // Should be "TRANSCRIBE"

        new AlertDialog.Builder(context)
                .setTitle(str("revanced_gemini_transcription_result_title"))
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .setNegativeButton(str("revanced_copy"), (d, w) -> setClipboard(context, rawTranscription, str("revanced_gemini_copy_success")))
                .setNeutralButton(str("revanced_gemini_transcription_parse_button"), (dialog, which) -> {
                    // Ensure this action is still relevant when the button is clicked
                    ensureMainThread(() -> {
                        assert dialogVideoUrl != null;
                        if (!isOperationRelevant(dialogOpType, dialogVideoUrl)) {
                            Logger.printDebug(() -> "Parse button click ignored, operation no longer relevant.");
                            String busyMsg = (currentOperation != OperationType.NONE)
                                    ? str("revanced_gemini_error_already_running_" + currentOperation.name().toLowerCase())
                                    : str("revanced_gemini_cancelled");
                            showToastShort(busyMsg);
                            try {dialog.dismiss();} catch (Exception ignored) {}
                            return;
                        }
                        Logger.printDebug(() -> "Parse button clicked (Gemini raw). Attempting parse and display overlay.");

                        // Ensure any previous overlay is gone before attempting parse/display
                        hideTranscriptionOverlayInternal();

                        if (attemptParseAndDisplayCachedTranscriptionInternal()) {
                            // Success: Overlay is shown. Dismiss this raw text dialog.
                            dialog.dismiss();
                            // Operation is logically finished (a raw text retrieved), now showing overlay.
                            // Reset flags but keep cache/UI.
                            resetOperationStateInternal(OperationType.TRANSCRIBE, false);
                            showToastShort(str("revanced_gemini_transcription_parse_success"));
                        }
                    });
                })
                .show();
    }

    // endregion UI Methods: Dialogs

    // region UI Methods: Transcription Parsing and Overlay

    /**
     * Attempts to parse the cached raw Gemini transcription data ({@link #cachedRawTranscription})
     * and, if successful, displays it using the subtitle overlay.
     * Ensures any previous overlay is hidden first.
     * Must be called on the Main Thread.
     *
     * @return {@code true} if parsing and displaying were successful, {@code false} otherwise.
     */
    @MainThread
    private boolean attemptParseAndDisplayCachedTranscriptionInternal() {
        Logger.printDebug(() -> "Attempting parse/display cached Gemini transcription...");

        if (TextUtils.isEmpty(cachedRawTranscription)) {
            Logger.printException(() -> "Cannot parse, cached raw Gemini data is empty.", null);
            showToastLong(str("revanced_gemini_error_transcription_no_raw_data"));
            hideTranscriptionOverlayInternal();
            return false;
        }

        TreeMap<Long, Pair<Long, String>> parsedData = null;
        try {
            parsedData = parseGeminiTranscriptionInternal(cachedRawTranscription);
        } catch (Exception e) {
            Logger.printException(() -> "Failed to parse cached Gemini data.", e);
        }

        if (parsedData == null || parsedData.isEmpty()) {
            Logger.printException(() -> "Parsed Gemini data is null or empty after parsing raw text.", null);
            showToastLong(str("revanced_gemini_error_transcription_parse"));
            parsedTranscription = null;
            hideTranscriptionOverlayInternal();
            return false;
        }

        TreeMap<Long, Pair<Long, String>> finalParsedData = parsedData;
        Logger.printDebug(() -> "Successfully parsed " + finalParsedData.size() + " Gemini entries.");

        parsedTranscription = parsedData;

        // Attempt to display using the overlay
        if (!displayTranscriptionOverlayInternal()) {
            // Error toast/logging handled inside displayTranscriptionOverlayInternal
            // Parsed data remains cached, but display failed. Overlay should be hidden.
            return false;
        }

        // Successfully parsed and displayed Gemini data via overlay
        Logger.printInfo(() -> "Successfully parsed and displayed Gemini transcription overlay.");
        return true;
    }

    /**
     * Parses the raw transcription text (expected Gemini format) into a timed map.
     *
     * @param rawText The raw transcription string.
     * @return A TreeMap mapping start times (ms) to Pairs of end times (ms) and subtitle text.
     * Returns an empty map if the input is empty or no valid lines are found.
     * Returns null only if a critical error occurs during parsing setup (unlikely).
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
                        Logger.printDebug(() -> "Skipping invalid Gemini line (timing or empty text): " + trimmedLine);
                    }
                } catch (NumberFormatException e) {
                    Logger.printException(() -> "Number format error parsing Gemini line: " + trimmedLine, e);
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
            }

            long ms = Long.parseLong(cleanedMs);

            if (msString.length() == 1) return ms * 100;
            if (msString.length() == 2) return ms * 10;
            return ms;
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

        if (parsedTranscription == null || parsedTranscription.isEmpty()) {
            Logger.printInfo(() -> "No parsed transcription data available to display overlay.");
            hideTranscriptionOverlayInternal();
            return false;
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
            final View viewToRemove = (overlayInstance != null) ? overlayInstance.getOverlayView() : null;

            if (overlayInstance != null) {
                Logger.printInfo(() -> "Hiding and cleaning up transcription overlay instance.");

                if (viewToRemove != null) {
                    try {
                        WindowManager wm = (WindowManager) overlayInstance.context.getSystemService(Context.WINDOW_SERVICE);
                        // OR pass the manager's context if reliable
                        if (wm != null) {
                            Logger.printDebug(() -> "Attempting removeViewImmediate directly from GeminiManager...");
                            wm.removeViewImmediate(viewToRemove);
                            Logger.printDebug(() -> "View removed successfully by manager's explicit call.");
                        } else {
                            Logger.printException(() -> "WindowManager was null when attempting removal from manager.");
                        }
                    } catch (IllegalArgumentException e) {
                        // This is expected if the view was somehow already removed (e.g., user clicked just before this ran)
                        Logger.printInfo(() -> "Manager's direct removal attempt failed (IllegalArgumentException - view likely already gone or never properly added).");
                    } catch (Exception e) {
                        Logger.printException(() -> "Manager's direct removal attempt failed unexpectedly.", e);
                    }
                } else {
                    Logger.printDebug(() -> "Manager skipping direct removal attempt (view was null in the tracked overlay instance).");
                }

                this.isSubtitleOverlayShowing = false;
                this.subtitleOverlay = null;

                overlayInstance.destroy();
            } else {
                if (isSubtitleOverlayShowing) {
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
        if (!isSubtitleOverlayShowing || parsedTranscription == null || parsedTranscription.isEmpty() || subtitleOverlay == null) {
            Logger.printInfo(() -> "Subtitle updater preconditions not met. Stopping.");
            stopSubtitleUpdaterInternal();
            return;
        }

        Logger.printDebug(() -> "Starting subtitle updater.");
        stopSubtitleUpdaterInternal();

        subtitleUpdateRunnable = new Runnable() {
            private String lastTextSentToOverlay = null; // Track last text to avoid redundant UI updates

            @Override
            public void run() {
                if (!isSubtitleOverlayShowing || parsedTranscription == null || subtitleOverlay == null) {
                    Logger.printDebug(() -> "Stopping updater in runnable: state invalid.");
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
                        subtitleOverlay.updateText(textToShow);
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
        if (parsedTranscription == null || parsedTranscription.isEmpty() || currentTimeMillis < 0) {
            return EMPTY_SUBTITLE_PLACEHOLDER;
        }

        // Find the latest entry whose start time is less than or equal to the current time
        Map.Entry<Long, Pair<Long, String>> entry = parsedTranscription.floorEntry(currentTimeMillis);

        if (entry != null) {
            long startTime = entry.getKey();
            long endTime = entry.getValue().first;
            String text = entry.getValue().second;

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

        Logger.printDebug(() -> "Starting progress timer.");
        stopTimerInternal();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentOperation != OperationType.NONE && !isCancelled && !isProgressDialogMinimized && startTimeMillis > 0 && progressDialog != null && progressDialog.isShowing()) {
                    updateTimerMessageInternal();
                    timerHandler.postDelayed(this, 1000);
                } else {
                    Logger.printDebug(() -> "Stopping timer in runnable: state invalid.");
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
        if (progressDialog != null && progressDialog.isShowing() && !isCancelled && !isProgressDialogMinimized && startTimeMillis > 0) {
            int sec = calculateElapsedTimeSeconds();
            if (sec < 0) return;

            String time = "\n" + sec + "s";
            String base = (baseLoadingMessage != null && !baseLoadingMessage.isEmpty())
                    ? baseLoadingMessage
                    : str("revanced_gemini_loading_default");
            String msg = base + time;

            try {
                // Try direct TextView access (potentially slightly faster)
                TextView tv = progressDialog.findViewById(android.R.id.message);
                if (tv != null) {
                    tv.setText(msg);
                } else {
                    // Fallback if direct access fails
                    progressDialog.setMessage(msg);
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
