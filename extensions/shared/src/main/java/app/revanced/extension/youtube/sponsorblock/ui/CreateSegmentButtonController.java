package app.revanced.extension.youtube.sponsorblock.ui;

import static app.revanced.extension.shared.utils.Utils.getChildView;

import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.Objects;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.overlaybutton.BottomControlButton;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public class CreateSegmentButtonController {
    private static WeakReference<ImageView> buttonReference = new WeakReference<>(null);
    private static boolean isVisible;


    /**
     * injection point
     */
    public static void initialize(View youtubeControlsLayout) {
        try {
            ImageView imageView = Objects.requireNonNull(getChildView(youtubeControlsLayout, "revanced_sb_create_segment_button"));
            imageView.setVisibility(View.GONE);
            imageView.setOnClickListener(v -> SponsorBlockViewController.toggleNewSegmentLayoutVisibility());
            buttonReference = new WeakReference<>(imageView);
        } catch (Exception ex) {
            Logger.printException(() -> "Unable to set RelativeLayout", ex);
        }
    }

    public static void changeVisibility(boolean visible, boolean animation) {
        ImageView imageView = buttonReference.get();
        if (imageView == null || isVisible == visible) return;
        isVisible = visible;

        if (visible) {
            imageView.clearAnimation();
            if (!shouldBeShown()) {
                return;
            }
            if (animation) {
                imageView.startAnimation(BottomControlButton.getButtonFadeIn());
            }
            imageView.setVisibility(View.VISIBLE);
            return;
        }
        if (imageView.getVisibility() == View.VISIBLE) {
            imageView.clearAnimation();
            if (animation) {
                imageView.startAnimation(BottomControlButton.getButtonFadeOut());
            }
            imageView.setVisibility(View.GONE);
        }
    }

    public static void changeVisibilityNegatedImmediate() {
        ImageView imageView = buttonReference.get();
        if (imageView == null) return;
        if (!shouldBeShown()) return;

        imageView.clearAnimation();
        imageView.startAnimation(BottomControlButton.getButtonFadeOutImmediate());
        imageView.setVisibility(View.GONE);
    }

    private static boolean shouldBeShown() {
        return Settings.SB_ENABLED.get() && Settings.SB_CREATE_NEW_SEGMENT.get()
                && !VideoInformation.isAtEndOfVideo();
    }

    public static void hide() {
        if (!isVisible) {
            return;
        }
        Utils.verifyOnMainThread();
        View v = buttonReference.get();
        if (v == null) {
            return;
        }
        v.setVisibility(View.GONE);
        isVisible = false;
    }
}
