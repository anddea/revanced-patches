package app.revanced.extension.youtube.utils;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.showToastLong;
import static app.revanced.extension.shared.utils.Utils.showToastShort;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;

@SuppressWarnings("RegExpRedundantEscape")
public class GeminiManager {
    private static volatile GeminiManager instance;

    private static final long SUBTITLE_UPDATE_INTERVAL_MS = 250;

    private enum OperationType { SUMMARIZE, TRANSCRIBE, NONE }

    @Nullable private AlertDialog progressDialog;
    private boolean isCancelled = false;
    private boolean isProgressDialogMinimized = false;
    private OperationType currentOperation = OperationType.NONE;
    @Nullable private String currentVideoUrl = null;

    @Nullable private String cachedSummaryVideoUrl = null;
    @Nullable private String cachedSummaryResult = null;
    private int totalSummarizationTimeSeconds = -1;

    @Nullable private String cachedTranscriptionVideoUrl = null;
    @Nullable private String cachedRawTranscription = null;
    @Nullable private TreeMap<Long, Pair<Long, String>> parsedTranscription = null;
    private int totalTranscriptionTimeSeconds = -1;
    @Nullable private SubtitleOverlay subtitleOverlay;
    private boolean isSubtitleOverlayShowing = false;
    @Nullable private String lastDisplayedSubtitleText = null;

    private final Handler timerHandler;
    private Runnable timerRunnable;
    private long startTimeMillis = -1;
    private String baseLoadingMessage;

    private final Handler subtitleUpdateHandler;
    private Runnable subtitleUpdateRunnable;

    private static final Pattern TRANSCRIPTION_PATTERN = Pattern.compile(
            "\\[(?:(\\d{2}):)?(\\d{2}):(\\d{2}).(\\d{3})\\s-\\s(?:(\\d{2}):)?(\\d{2}):(\\d{2}).(\\d{3})\\]:?\\s(.*)"
    );

    private GeminiManager() {
        timerHandler = new Handler(Looper.getMainLooper());
        subtitleUpdateHandler = new Handler(Looper.getMainLooper());
    }

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


    public void startSummarization(Context context, final String videoUrl) {
        if (isValidVideoUrl(videoUrl)) return;

        if (Objects.equals(videoUrl, cachedSummaryVideoUrl) && cachedSummaryResult != null) {
            Logger.printDebug(() -> "Displaying cached summary for: " + videoUrl);
            showSummaryDialog(context, cachedSummaryResult, totalSummarizationTimeSeconds);
            return;
        }

        if (isBusy(context, videoUrl, OperationType.SUMMARIZE)) return;

        final String apiKey = Settings.GEMINI_API_KEY.get();
        if (isValidApiKey(apiKey)) return;

        Logger.printDebug(() -> "Starting new summarization for: " + videoUrl);
        prepareForNewOperation(OperationType.SUMMARIZE, videoUrl);
        showProgressDialog(context, OperationType.SUMMARIZE);

        GeminiUtils.getVideoSummary(videoUrl, apiKey, new GeminiUtils.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onSuccess(String result) {
                handleApiResponse(context, OperationType.SUMMARIZE, videoUrl, result, null);
            }

            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onFailure(String error) {
                handleApiResponse(context, OperationType.SUMMARIZE, videoUrl, null, error);
            }
        });
    }

    public void startTranscription(Context context, final String videoUrl) {
        if (isValidVideoUrl(videoUrl)) return;

        if (Objects.equals(videoUrl, cachedTranscriptionVideoUrl) && cachedRawTranscription != null) {
            Logger.printDebug(() -> "Displaying cached transcription result dialog for: " + videoUrl);
            showTranscriptionResultDialog(context, cachedRawTranscription, totalTranscriptionTimeSeconds);
            return;
        }

        if (isBusy(context, videoUrl, OperationType.TRANSCRIBE)) return;

        final String apiKey = Settings.GEMINI_API_KEY.get();
        if (isValidApiKey(apiKey)) return;

        Logger.printDebug(() -> "Starting new transcription for: " + videoUrl);
        prepareForNewOperation(OperationType.TRANSCRIBE, videoUrl);
        showProgressDialog(context, OperationType.TRANSCRIBE);

        GeminiUtils.getVideoTranscription(videoUrl, apiKey, new GeminiUtils.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onSuccess(String result) {
                handleApiResponse(context, OperationType.TRANSCRIBE, videoUrl, result, null);
            }

            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onFailure(String error) {
                handleApiResponse(context, OperationType.TRANSCRIBE, videoUrl, null, error);
            }
        });
    }

    private boolean isBusy(Context context, String requestedUrl, OperationType requestedOp) {
        if (currentOperation == OperationType.NONE) {
            return false;
        }

        if (currentOperation == requestedOp && Objects.equals(currentVideoUrl, requestedUrl)) {
            if (isProgressDialogMinimized) {
                Logger.printDebug(() -> "Re-showing minimized progress dialog for: " + requestedUrl);
                isProgressDialogMinimized = false;
                showProgressDialog(context, requestedOp);
            } else {
                Logger.printDebug(() -> requestedOp + " already in progress and dialog visible for: " + requestedUrl);
            }
        } else {
            showToastShort(str("revanced_gemini_error_already_running_" + currentOperation.name().toLowerCase()));
        }
        return true;
    }

    private void prepareForNewOperation(OperationType operationType, String videoUrl) {
        hideTranscriptionOverlay();

        if (currentOperation == OperationType.NONE) {
            resetOperationState(OperationType.NONE, false);
        } else {
            stopTimer();
            dismissProgressDialog();
            // Don't necessarily clear cache here, let resetOperationState handle it based on type
        }

        isCancelled = false;
        isProgressDialogMinimized = false;
        currentOperation = operationType;
        currentVideoUrl = videoUrl;
        startTimeMillis = System.currentTimeMillis();

        if (operationType == OperationType.SUMMARIZE) {
            totalSummarizationTimeSeconds = -1;
            // Clear transcription cache if starting summarize? Maybe not, user might want to switch back.
            // Let resetOperationState handle clearing based on what *was* running if needed.
        } else if (operationType == OperationType.TRANSCRIBE) {
            totalTranscriptionTimeSeconds = -1;
            parsedTranscription = null; // Clear parsed data for new transcription
            // Don't clear cachedRawTranscription here, wait for success/failure
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void handleApiResponse(Context context, OperationType opType, String videoUrl, @Nullable String result, @Nullable String error) {
        context.getMainExecutor().execute(() -> {
            if (currentOperation != opType || !Objects.equals(currentVideoUrl, videoUrl)) {
                handleIrrelevantResponse(opType, videoUrl, result != null);
                return;
            }

            dismissProgressDialog();

            if (error != null) {
                Logger.printException(() -> opType + " API failure for " + videoUrl + ": " + error);
                showToastLong(str("revanced_gemini_error_api_failed", error));
                resetOperationState(opType, true);
                return;
            }

            if (result != null) {
                Logger.printDebug(() -> opType + " API success for " + videoUrl);
                long endTimeMillis = System.currentTimeMillis();
                int timeTaken = -1;
                if (startTimeMillis > 0) {
                    timeTaken = (int) ((endTimeMillis - startTimeMillis) / 1000);
                }

                if (opType == OperationType.SUMMARIZE) {
                    totalSummarizationTimeSeconds = timeTaken;
                    cachedSummaryVideoUrl = videoUrl;
                    cachedSummaryResult = result;
                    showSummaryDialog(context, result, totalSummarizationTimeSeconds);
                    resetOperationState(opType, false);

                } else if (opType == OperationType.TRANSCRIBE) {
                    totalTranscriptionTimeSeconds = timeTaken;
                    cachedTranscriptionVideoUrl = videoUrl;
                    cachedRawTranscription = result;
                    showTranscriptionResultDialog(context, result, totalTranscriptionTimeSeconds);
                    resetOperationState(opType, false);
                }

            } else {
                Logger.printException(() -> opType + " API response was null without error for " + videoUrl);
                showToastLong(str("revanced_gemini_error_unknown"));
                resetOperationState(opType, true);
            }
        });
    }


    private void handleIrrelevantResponse(OperationType opType, String videoUrl, boolean wasSuccess) {
        String status = wasSuccess ? "succeeded" : "failed";
        if (isCancelled) {
            Logger.printDebug(() -> "Ignoring Gemini " + opType + " response for " + videoUrl + " (" + status + ") because operation was cancelled.");
        } else {
            Logger.printDebug(() -> "Ignoring Gemini " + opType + " response for " + videoUrl + " (" + status + ") because it's no longer relevant (current op: " + currentOperation + " for URL " + currentVideoUrl + ").");
        }
    }

    private void resetOperationState(OperationType opBeingReset, boolean clearCache) {
        stopTimer();

        if (currentOperation == opBeingReset || opBeingReset == OperationType.NONE) {
            isCancelled = false;
            isProgressDialogMinimized = false;
            currentOperation = OperationType.NONE;
            currentVideoUrl = null;
            startTimeMillis = -1;
            baseLoadingMessage = null;
            Logger.printDebug(() -> "General operation state reset.");
        }

        // Cache clearing for Summarize
        if (opBeingReset == OperationType.SUMMARIZE || opBeingReset == OperationType.NONE) {
            if (clearCache) {
                totalSummarizationTimeSeconds = -1;
                cachedSummaryVideoUrl = null;
                cachedSummaryResult = null;
                Logger.printDebug(() -> "Summary cache cleared.");
            }
        }

        // Cache and overlay clearing for Transcribe
        if (opBeingReset == OperationType.TRANSCRIBE || opBeingReset == OperationType.NONE) {
            hideTranscriptionOverlay();
            parsedTranscription = null;
            if (clearCache) {
                totalTranscriptionTimeSeconds = -1;
                cachedTranscriptionVideoUrl = null;
                cachedRawTranscription = null;
                Logger.printDebug(() -> "Transcription cache (raw and parsed) cleared.");
            } else {
                Logger.printDebug(() -> "Parsed transcription data cleared (raw cache preserved).");
            }
        }
    }

    private boolean isValidVideoUrl(String videoUrl) {
        if (TextUtils.isEmpty(videoUrl) || videoUrl.equals(VideoUtils.VIDEO_URL)) {
            showToastShort(str("revanced_gemini_error_no_video"));
            return true;
        }
        return false;
    }

    private boolean isValidApiKey(String apiKey) {
        if (TextUtils.isEmpty(apiKey)) {
            showToastLong(str("revanced_gemini_error_no_api_key"));
            return true;
        }
        return false;
    }

    private void showProgressDialog(Context context, OperationType operationType) {
        dismissProgressDialog();

        baseLoadingMessage = str("revanced_gemini_loading_" + operationType.name().toLowerCase());
        String initialMessage = baseLoadingMessage;

        if (currentOperation == operationType && startTimeMillis > 0 && !isProgressDialogMinimized) {
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            if (elapsedMillis > 0) {
                int elapsedSeconds = (int) (elapsedMillis / 1000);
                initialMessage += "\n" + elapsedSeconds + "s";
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(initialMessage);
        builder.setCancelable(false);

        // Cancel Button
        builder.setNegativeButton(str("revanced_cancel"), (dialog, which) -> {
            if (currentOperation == operationType) {
                Logger.printDebug(() -> "Gemini " + currentOperation + " cancelled by user for " + currentVideoUrl);
                isCancelled = true;
                GeminiUtils.cancelCurrentTask();
                resetOperationState(currentOperation, true);
                dismissProgressDialog();
                showToastShort(str("revanced_gemini_cancelled"));
            } else {
                Logger.printDebug(() -> "Cancel button clicked, but operation state has changed. Ignoring.");
                try { dialog.dismiss(); } catch (Exception ignored) {}
            }
        });

        // Minimize Button
        builder.setNeutralButton(str("revanced_minimize"), (dialog, which) -> {
            if (currentOperation == operationType) {
                if (!isProgressDialogMinimized) {
                    isProgressDialogMinimized = true;
                    stopTimer();
                    dismissProgressDialog();
                    Logger.printDebug(() -> "Gemini " + currentOperation + " progress dialog minimized.");
                    // State remains active, task continues in background
                }
            } else {
                Logger.printDebug(() -> "Minimize button clicked, but operation state has changed. Ignoring.");
                try { dialog.dismiss(); } catch (Exception ignored) {}
            }
        });

        progressDialog = builder.create();
        try {
            progressDialog.show();
            if (currentOperation == operationType && !isCancelled && !isProgressDialogMinimized) {
                startTimer();
            }
        } catch (Exception e) {
            Logger.printException(() -> "Error showing progress dialog for " + operationType, e);
            if (currentOperation == operationType) {
                resetOperationState(operationType, true);
            }
            progressDialog = null;
        }
    }

    private void updateTimerMessage() {
        if (progressDialog != null && progressDialog.isShowing() && currentOperation != OperationType.NONE && !isCancelled && !isProgressDialogMinimized && startTimeMillis > 0) {
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            int elapsedSeconds = (int) (elapsedMillis / 1000);
            String timeString = elapsedSeconds + "s";
            String currentBaseMessage = str("revanced_gemini_loading_" + currentOperation.name().toLowerCase());
            String message = currentBaseMessage + "\n" + timeString;

            try {
                TextView messageView = progressDialog.findViewById(android.R.id.message);
                if (messageView != null) {
                    messageView.setText(message);
                } else {
                    progressDialog.setMessage(message);
                }
            } catch (Exception e) {
                Logger.printException(() -> "Error updating progress dialog timer message", e);
                stopTimer();
            }
        } else {
            stopTimer();
        }
    }

    private void dismissProgressDialog() {
        stopTimer();
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                Logger.printException(() -> "Error dismissing progress dialog", e);
            }
        }
        progressDialog = null;
    }

    private void showSummaryDialog(Context context, String summary, int secondsTaken) {
        String finalMessage = summary;
        if (secondsTaken >= 0) {
            finalMessage += "\n\n" + str("revanced_gemini_time_taken", secondsTaken);
        }

        new AlertDialog.Builder(context)
                .setTitle(str("revanced_gemini_summary_title"))
                .setMessage(finalMessage)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setNeutralButton(str("revanced_copy"), (dialog, which) -> setClipboard(context, summary, str("revanced_gemini_copy_success")))
                .show();
    }

    private void showTranscriptionResultDialog(Context context, String rawTranscription, int secondsTaken) {
        String finalMessage = rawTranscription;
        if (secondsTaken >= 0) {
            finalMessage += "\n\n" + str("revanced_gemini_time_taken", secondsTaken);
        }

        String title = str("revanced_gemini_transcription_result_title");
        String parseButtonText = str("revanced_gemini_transcription_parse_button");
        String copyButtonText = str("revanced_copy");

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(finalMessage)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());

        builder.setNegativeButton(copyButtonText, (dialog, which) -> setClipboard(context, rawTranscription, str("revanced_gemini_copy_success")));

        // Parse Button (Neutral position)
        builder.setNeutralButton(parseButtonText, (dialog, which) -> {
            boolean parseSuccess = attemptParseAndDisplayCachedTranscription(context);
            if (parseSuccess) {
                showToastShort(str("revanced_gemini_transcription_parse_success"));
                dialog.dismiss();
            }
        });

        builder.show();
    }


    private boolean attemptParseAndDisplayCachedTranscription(Context context) {
        if (TextUtils.isEmpty(cachedRawTranscription)) {
            Logger.printException(() -> "Attempted to parse transcription, but cached raw data is empty.");
            showToastLong(str("revanced_gemini_error_transcription_no_raw_data"));
            return false;
        }

        Logger.printDebug(() -> "Attempting to parse cached raw transcription...");
        parsedTranscription = parseTranscription(cachedRawTranscription);

        if (parsedTranscription == null || parsedTranscription.isEmpty()) {
            Logger.printException(() -> "Failed to parse cached transcription data.");
            showToastLong(str("revanced_gemini_error_transcription_parse"));
            parsedTranscription = null;
            hideTranscriptionOverlay();
            return false;
        }

        Logger.printDebug(() -> "Successfully parsed " + parsedTranscription.size() + " transcription entries from cache.");

        boolean displaySuccess = displayTranscriptionOverlay(context);
        if (!displaySuccess) {
            showToastLong(str("revanced_gemini_error_overlay_display"));
            parsedTranscription = null;
            hideTranscriptionOverlay();
            return false;
        }

        return true;
    }


    @Nullable
    private TreeMap<Long, Pair<Long, String>> parseTranscription(String rawText) {
        if (TextUtils.isEmpty(rawText)) {
            Logger.printException(() -> "parseTranscription called with empty input.");
            return null;
        }
        TreeMap<Long, Pair<Long, String>> map = new TreeMap<>();
        Matcher matcher = TRANSCRIPTION_PATTERN.matcher(rawText);
        int lineCount = 0;
        int parseErrors = 0;

        String[] lines = rawText.split("\\r?\\n");

        for (String line : lines) {
            if (TextUtils.isEmpty(line.trim())) continue;

            lineCount++;
            matcher.reset(line);

            if (matcher.find()) {
                try {
                    long startH = (matcher.group(1) != null) ? Long.parseLong(matcher.group(1)) : 0;
                    long startM = Long.parseLong(matcher.group(2));
                    long startS = Long.parseLong(matcher.group(3));
                    long startMsPart = (matcher.group(4) != null) ? Long.parseLong(padMilliseconds(matcher.group(4))) : 0;
                    long startTimeMillis = TimeUnit.HOURS.toMillis(startH) +
                            TimeUnit.MINUTES.toMillis(startM) +
                            TimeUnit.SECONDS.toMillis(startS) +
                            startMsPart;

                    long endH = (matcher.group(5) != null) ? Long.parseLong(matcher.group(5)) : 0;
                    long endM = Long.parseLong(matcher.group(6));
                    long endS = Long.parseLong(matcher.group(7));
                    long endMsPart = (matcher.group(8) != null) ? Long.parseLong(padMilliseconds(matcher.group(8))) : 0;
                    long endTimeMillis = TimeUnit.HOURS.toMillis(endH) +
                            TimeUnit.MINUTES.toMillis(endM) +
                            TimeUnit.SECONDS.toMillis(endS) +
                            endMsPart;

                    String text = Objects.requireNonNull(matcher.group(9)).trim();

                    // Basic validation
                    if (endTimeMillis < startTimeMillis) {
                        Logger.printInfo(() -> "Skipping transcription line due to end time < start time: " + line);
                        parseErrors++;
                        continue;
                    }

                    if (!TextUtils.isEmpty(text)) {
                        if (map.containsKey(startTimeMillis)) {
                            // Handle duplicate start times if necessary (e.g., log, overwrite, append)
                            Logger.printInfo(() -> "Duplicate start timestamp detected ("+startTimeMillis+"ms). Overwriting previous entry: " + line);
                        }
                        map.put(startTimeMillis, new Pair<>(endTimeMillis, text));
                    } else {
                        Logger.printInfo(() -> "Skipping transcription line due to empty text: " + line);
                    }

                } catch (NumberFormatException | NullPointerException e) {
                    Logger.printException(() -> "Error parsing transcription timestamp/text on line: " + line, e);
                    parseErrors++;
                } catch (IndexOutOfBoundsException iobe) {
                    // Should not happen with matches() if pattern is correct, but good to have
                    Logger.printException(() -> "Regex group index out of bounds on line (Pattern requires 9 groups): " + line, iobe);
                    parseErrors++;
                }
            } else {
                // Line didn't match the expected format
                Logger.printInfo(() -> "Skipping line, does not match transcription pattern: " + line);
            }
        }

        int finalLineCount = lineCount;
        int finalParseErrors = parseErrors;
        Logger.printDebug(() -> "Transcription parsing finished. Input lines processed: " + finalLineCount + ", Entries added: " + map.size() + ", Timestamp parse errors: " + finalParseErrors);

        return map.isEmpty() ? null : map;
    }

    @NonNull
    private String padMilliseconds(@Nullable String ms) {
        if (ms == null || ms.isEmpty()) return "000";
        String cleanedMs = ms.replaceAll("[^0-9]", "");
        if (cleanedMs.isEmpty()) return "000";

        if (cleanedMs.length() == 1) return cleanedMs + "00";
        if (cleanedMs.length() == 2) return cleanedMs + "0";
        return cleanedMs.substring(0, 3);
    }

    private boolean displayTranscriptionOverlay(Context context) {
        if (parsedTranscription == null || parsedTranscription.isEmpty()) {
            Logger.printInfo(() -> "No parsed transcription data to display.");
            hideTranscriptionOverlay();
            return false;
        }
        if (context == null) {
            Logger.printException(() -> "Context is null, cannot create SubtitleOverlay.");
            return false;
        }

        try {
            // If an overlay exists but isn't showing, destroy it first to be safe
            if (subtitleOverlay != null && !isSubtitleOverlayShowing) {
                hideTranscriptionOverlay();
                subtitleOverlay = null;
            }

            if (subtitleOverlay == null) {
                subtitleOverlay = new SubtitleOverlay(context);
                Logger.printDebug(() -> "New SubtitleOverlay created.");
            }

            long currentTimeMillis = VideoInformation.getVideoTime();
            String initialText = findSubtitleTextForTime(currentTimeMillis);
            subtitleOverlay.updateText(initialText);
            lastDisplayedSubtitleText = initialText;

            subtitleOverlay.show();
            isSubtitleOverlayShowing = true;
            startSubtitleUpdater();
            Logger.printDebug(() -> "Subtitle overlay displayed.");
            return true;
        } catch (Exception e) {
            Logger.printException(() -> "Failed to create or show SubtitleOverlay", e);
            isSubtitleOverlayShowing = false;
            if (subtitleOverlay != null) {
                try { subtitleOverlay.hide(); } catch (Exception ignored) {}
                try { subtitleOverlay.destroy(); } catch (Exception ignored) {}
                subtitleOverlay = null;
            }
            return false;
        }
    }

    private void hideTranscriptionOverlay() {
        stopSubtitleUpdater();
        if (subtitleOverlay != null) {
            try {
                subtitleOverlay.hide();
                Logger.printDebug(() -> "Subtitle overlay hidden.");
                try {
                    subtitleOverlay.destroy();
                    Logger.printDebug(() -> "Subtitle overlay destroyed.");
                } catch (Exception e) {
                    Logger.printException(() -> "Error destroying SubtitleOverlay", e);
                }
                subtitleOverlay = null;

            } catch (Exception e) {
                Logger.printException(() -> "Error hiding SubtitleOverlay", e);
                subtitleOverlay = null;
            }
        }
        // Reset state variables related to the overlay
        isSubtitleOverlayShowing = false;
        lastDisplayedSubtitleText = null;
    }

    // Starts the periodic task to update subtitle text based on video time
    private void startSubtitleUpdater() {
        stopSubtitleUpdater();
        if (!isSubtitleOverlayShowing || parsedTranscription == null || parsedTranscription.isEmpty() || subtitleOverlay == null) {
            Logger.printDebug(() -> "Subtitle updater not started (overlay not showing or no data/instance).");
            return;
        }

        subtitleUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // Double-check state within the runnable
                if (!isSubtitleOverlayShowing || parsedTranscription == null || subtitleOverlay == null) {
                    Logger.printDebug(() -> "Stopping subtitle updater because state became invalid.");
                    stopSubtitleUpdater();
                    return;
                }

                long currentTimeMillis = VideoInformation.getVideoTime();
                if (currentTimeMillis < 0) {
                    subtitleUpdateHandler.postDelayed(this, SUBTITLE_UPDATE_INTERVAL_MS);
                    return;
                }

                // Find the appropriate subtitle text for the current time
                String textToShow = findSubtitleTextForTime(currentTimeMillis);

                // Update the overlay only if the text has changed
                if (!Objects.equals(textToShow, lastDisplayedSubtitleText)) {
                    try {
                        subtitleOverlay.updateText(textToShow);
                        lastDisplayedSubtitleText = textToShow;
                    } catch (Exception e) {
                        Logger.printException(() -> "Error updating subtitle text in overlay", e);
                        hideTranscriptionOverlay();
                        return;
                    }
                }

                // Schedule the next run if the overlay is still supposed to be showing
                if (isSubtitleOverlayShowing) {
                    subtitleUpdateHandler.postDelayed(this, SUBTITLE_UPDATE_INTERVAL_MS);
                } else {
                    Logger.printDebug(() -> "Subtitle updater loop ending as overlay is no longer showing.");
                    stopSubtitleUpdater(); // Explicitly stop just in case
                }
            }
        };
        // Post the first run
        subtitleUpdateHandler.post(subtitleUpdateRunnable);
        Logger.printDebug(() -> "Subtitle updater started.");
    }

    // Stops the periodic subtitle update task
    private void stopSubtitleUpdater() {
        if (subtitleUpdateHandler != null && subtitleUpdateRunnable != null) {
            subtitleUpdateHandler.removeCallbacks(subtitleUpdateRunnable);
            // subtitleUpdateRunnable = null; // Optionally nullify runnable
            Logger.printDebug(() -> "Subtitle updater stopped.");
        }
    }

    // Finds the correct subtitle text for a given time from the parsed map
    @NonNull
    private String findSubtitleTextForTime(long currentTimeMillis) {
        if (parsedTranscription == null || parsedTranscription.isEmpty() || currentTimeMillis < 0) {
            return "";
        }

        // Find the latest entry whose start time is less than or equal to the current time
        Map.Entry<Long, Pair<Long, String>> potentialMatch = parsedTranscription.floorEntry(currentTimeMillis);

        // If an entry is found, check if the current time falls within its duration
        if (potentialMatch != null) {
            // long startTime = potentialMatch.getKey();
            long endTime = potentialMatch.getValue().first;

            if (currentTimeMillis < endTime) {
                return potentialMatch.getValue().second;
            }
        }

        // No segment is active at the current time
        return "";
    }


    private void startTimer() {
        stopTimer();
        if (currentOperation == OperationType.NONE || isCancelled || isProgressDialogMinimized || startTimeMillis <= 0 || progressDialog == null || !progressDialog.isShowing()) {
            return;
        }

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentOperation != OperationType.NONE && !isCancelled && !isProgressDialogMinimized && startTimeMillis > 0 && progressDialog != null && progressDialog.isShowing()) {
                    updateTimerMessage();
                    timerHandler.postDelayed(this, 1000);
                } else {
                    Logger.printDebug(() -> "Timer runnable stopping condition met.");
                    stopTimer(); // Explicitly stop if conditions fail
                }
            }
        };
        // Run the first update immediately (or almost immediately)
        updateTimerMessage();
        // Schedule the first delayed run
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            // timerRunnable = null; // Optionally nullify
        }
    }

    private static void setClipboard(Context context, String text, String toastMessage) {
        if (context == null) {
            Logger.printException(() -> "Context is null, cannot set clipboard.");
            return;
        }
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            showToastShort(toastMessage);
        } else {
            Logger.printException(() -> "ClipboardManager service not available.");
            showToastShort(str("revanced_copy_error"));
        }
    }
}
