package app.revanced.extension.youtube.patches.shorts;

import static app.revanced.extension.shared.utils.ResourceUtils.getRawIdentifier;
import static app.revanced.extension.youtube.patches.shorts.AnimationFeedbackPatch.AnimationType.ORIGINAL;

import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;

import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.youtube.patches.utils.LottieAnimationViewPatch;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class AnimationFeedbackPatch {

    public enum AnimationType {
        /**
         * Unmodified type, and same as un-patched.
         */
        ORIGINAL(null),
        THUMBS_UP("like_tap_feedback"),
        THUMBS_UP_CAIRO("like_tap_feedback_cairo"),
        HEART("like_tap_feedback_heart"),
        HEART_TINT("like_tap_feedback_heart_tint"),
        HIDDEN("like_tap_feedback_hidden");

        /**
         * Animation id.
         */
        final int rawRes;

        AnimationType(@Nullable String jsonName) {
            this.rawRes = jsonName != null
                    ? getRawIdentifier(jsonName)
                    : 0;
        }
    }

    private static final AnimationType CURRENT_TYPE = Settings.ANIMATION_TYPE.get();

    private static final boolean HIDE_PLAY_PAUSE_FEEDBACK = Settings.HIDE_SHORTS_PLAY_PAUSE_BUTTON_BACKGROUND.get();

    private static final int PAUSE_TAP_FEEDBACK_HIDDEN
            = ResourceUtils.getRawIdentifier("pause_tap_feedback_hidden");

    private static final int PLAY_TAP_FEEDBACK_HIDDEN
            = ResourceUtils.getRawIdentifier("play_tap_feedback_hidden");


    /**
     * Injection point.
     */
    public static void setShortsLikeFeedback(LottieAnimationView lottieAnimationView) {
        if (CURRENT_TYPE == ORIGINAL) {
            return;
        }

        LottieAnimationViewPatch.setLottieAnimationRawResources(lottieAnimationView, CURRENT_TYPE.rawRes);
    }

    /**
     * Injection point.
     */
    public static void setShortsPauseFeedback(LottieAnimationView lottieAnimationView) {
        if (!HIDE_PLAY_PAUSE_FEEDBACK) {
            return;
        }

        LottieAnimationViewPatch.setLottieAnimationRawResources(lottieAnimationView, PAUSE_TAP_FEEDBACK_HIDDEN);
    }

    /**
     * Injection point.
     */
    public static void setShortsPlayFeedback(LottieAnimationView lottieAnimationView) {
        if (!HIDE_PLAY_PAUSE_FEEDBACK) {
            return;
        }

        LottieAnimationViewPatch.setLottieAnimationRawResources(lottieAnimationView, PLAY_TAP_FEEDBACK_HIDDEN);
    }

}
