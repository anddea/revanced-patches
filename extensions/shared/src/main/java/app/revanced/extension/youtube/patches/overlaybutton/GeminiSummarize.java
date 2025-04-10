package app.revanced.extension.youtube.patches.overlaybutton;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.GeminiManager;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public class GeminiSummarize extends BottomControlButton {
    @Nullable
    private static GeminiSummarize instance;

    public GeminiSummarize(ViewGroup bottomControlsViewGroup) {
        super(
                bottomControlsViewGroup,
                "gemini_summarize_button",
                Settings.OVERLAY_BUTTON_GEMINI_SUMMARIZE,
                view -> handleSummarizeClick(view.getContext()),
                view -> {
                    handleTranscribeClick(view.getContext());
                    return true;
                }
        );
    }

    public static void handleSummarizeClick(Context context) {
        if (instance == null) {
            Logger.printException(() -> "GeminiSummarize button instance is null, cannot proceed with summarize.");
            return;
        }
        final String videoUrl = VideoUtils.getVideoUrl(false);
        GeminiManager.getInstance().startSummarization(context, videoUrl);
    }

    public static void handleTranscribeClick(Context context) {
        if (instance == null) {
            Logger.printException(() -> "GeminiSummarize button instance is null, cannot proceed with transcribe.");
            return;
        }
        final String videoUrl = VideoUtils.getVideoUrl(false);

        GeminiManager.getInstance().startTranscription(context, videoUrl);
    }

    public static void initialize(View bottomControlsViewGroup) {
        try {
            if (bottomControlsViewGroup instanceof ViewGroup viewGroup) {
                if (instance == null) {
                    instance = new GeminiSummarize(viewGroup);
                    Logger.printDebug(() -> "GeminiSummarize button initialized (with long click).");
                } else {
                    Logger.printInfo(() -> "GeminiSummarize initialize called multiple times.");
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "GeminiSummarize.initialize failure", ex);
            instance = null;
        }
    }

    public static void changeVisibility(boolean showing, boolean animation) {
        if (instance != null) instance.setVisibility(showing, animation);
    }

    public static void changeVisibilityNegatedImmediate() {
        if (instance != null) instance.setVisibilityNegatedImmediate();
    }
}
