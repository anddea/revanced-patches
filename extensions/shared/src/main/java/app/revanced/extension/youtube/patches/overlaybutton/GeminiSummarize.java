package app.revanced.extension.youtube.patches.overlaybutton;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.showToastLong;
import static app.revanced.extension.shared.utils.Utils.showToastShort;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.GeminiUtils;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public class GeminiSummarize extends BottomControlButton {
    @Nullable
    private static GeminiSummarize instance;
    @Nullable
    private AlertDialog progressDialog;
    private boolean isCancelled = false;

    // Timer related variables
    private final Handler timerHandler;
    private Runnable timerRunnable;
    private long startTimeMillis;
    private String baseLoadingMessage;
    private int totalSummarizationTimeSeconds = -1;

    public GeminiSummarize(ViewGroup bottomControlsViewGroup) {
        super(
                bottomControlsViewGroup,
                "gemini_summarize_button",
                Settings.OVERLAY_BUTTON_GEMINI_SUMMARIZE,
                view -> handleSummarizeClick(view.getContext()),
                null
        );
        timerHandler = new Handler(Looper.getMainLooper());
    }

    // region Generate Summary
    private static void handleSummarizeClick(Context context) {
        if (instance == null) return;

        final String apiKey = Settings.GEMINI_API_KEY.get();
        if (TextUtils.isEmpty(apiKey)) {
            showToastLong(str("revanced_gemini_error_no_api_key"));
            return;
        }

        final String videoUrl = VideoUtils.getVideoUrl(false);
        if (TextUtils.isEmpty(videoUrl) || videoUrl.equals(VideoUtils.VIDEO_URL)) {
            showToastShort(str("revanced_gemini_error_no_video"));
            return;
        }

        instance.isCancelled = false;
        instance.totalSummarizationTimeSeconds = -1;
        instance.showProgressDialog(context);

        GeminiUtils.getVideoSummary(videoUrl, apiKey, new GeminiUtils.SummaryCallback() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onSuccess(String summary) {
                context.getMainExecutor().execute(() -> {
                    if (instance == null || instance.isCancelled) {
                        Logger.printDebug(() -> "Gemini request succeeded but was cancelled by user.");
                        instance.dismissProgressDialog();
                        return;
                    }
                    // Calculate total time *before* dismissing progress dialog
                    long endTimeMillis = System.currentTimeMillis();
                    if (instance.startTimeMillis > 0) {
                        long elapsedMillis = endTimeMillis - instance.startTimeMillis;
                        instance.totalSummarizationTimeSeconds = (int) (elapsedMillis / 1000);
                    }

                    instance.dismissProgressDialog();
                    instance.showSummaryDialog(context, summary, instance.totalSummarizationTimeSeconds);
                });
            }

            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onFailure(String error) {
                context.getMainExecutor().execute(() -> {
                    if (instance == null || instance.isCancelled) {
                        Logger.printDebug(() -> "Gemini request failed but was cancelled by user.");
                        instance.dismissProgressDialog();
                        return;
                    }
                    instance.dismissProgressDialog();
                    showToastLong(str("revanced_gemini_error_api_failed", error));
                });
            }
        });
    }

    private void showProgressDialog(Context context) {
        stopTimer();
        dismissProgressDialog();

        baseLoadingMessage = str("revanced_gemini_loading");
        startTimeMillis = -1; // Reset start time until timer actually starts

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Set initial message without time
        builder.setMessage(baseLoadingMessage);
        builder.setCancelable(false);

        builder.setNegativeButton(str("revanced_cancel"), (dialog, which) -> {
            isCancelled = true;
            stopTimer();
            dismissProgressDialog();
            showToastShort(str("revanced_gemini_cancelled"));
            Logger.printDebug(() -> "Gemini summarization cancelled by user.");
        });

        progressDialog = builder.create();
        progressDialog.show();

        // Start the timer which will update the message
        startTimer();
    }

    private void dismissProgressDialog() {
        stopTimer();
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException e) {
                Logger.printException(() -> "Error dismissing progress dialog", e);
            }
        }
        progressDialog = null;
    }

    private void showSummaryDialog(Context context, String summary, int secondsTaken) {
        dismissProgressDialog();

        String finalMessage = summary;
        if (secondsTaken >= 0) {
            finalMessage += "\n\n" + str("revanced_gemini_time_taken", secondsTaken);
        }

        new AlertDialog.Builder(context)
                .setTitle(str("revanced_gemini_summary_title"))
                .setMessage(finalMessage)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setNeutralButton(str("revanced_copy"), (dialog, which) -> {
                    // Copy only the summary part, not the time taken text
                    VideoUtils.setClipboard(summary, str("revanced_gemini_copy_success"));
                })
                .show();
    }
    // endregion Generate Summary

    // region Timer
    private void startTimer() {
        startTimeMillis = System.currentTimeMillis();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null && progressDialog.isShowing() && !isCancelled) {
                    long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
                    int elapsedSeconds = (int) (elapsedMillis / 1000);

                    String timeString = elapsedSeconds + "s";
                    progressDialog.setMessage(baseLoadingMessage + "\n" + timeString);

                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        // Start the first update slightly delayed to ensure dialog is visible
        timerHandler.postDelayed(timerRunnable, 500);
    }

    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        // Don't reset startTimeMillis here, we need it in onSuccess
    }
    // endregion Timer

    // region Lifecycle and Visibility
    public static void initialize(View bottomControlsViewGroup) {
        try {
            if (bottomControlsViewGroup instanceof ViewGroup viewGroup) {
                instance = new GeminiSummarize(viewGroup);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    public static void changeVisibility(boolean showing, boolean animation) {
        if (instance != null) instance.setVisibility(showing, animation);
    }

    public static void changeVisibilityNegatedImmediate() {
        if (instance != null) instance.setVisibilityNegatedImmediate();
    }
    // endregion Lifecycle and Visibility
}
