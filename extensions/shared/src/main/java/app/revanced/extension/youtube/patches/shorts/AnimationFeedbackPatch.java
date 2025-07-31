package app.revanced.extension.youtube.patches.shorts;

import static app.revanced.extension.shared.utils.ResourceUtils.getRawIdentifier;
import static app.revanced.extension.youtube.patches.shorts.AnimationFeedbackPatch.AnimationType.ORIGINAL;
import static app.revanced.extension.youtube.patches.utils.LottieAnimationViewPatch.setLottieAnimationRawResources;

import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;

import app.revanced.extension.shared.utils.ResourceUtils;
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

    private static final AnimationType CURRENT_TYPE =
            Settings.ANIMATION_TYPE.get();

    /**
     * Injection point.
     */
    public static int getShortsLikeFeedbackId(int originalId) {
        if (CURRENT_TYPE == ORIGINAL || CURRENT_TYPE.rawRes == 0) {
            return originalId;
        }

        return CURRENT_TYPE.rawRes;
    }

    /**
     * Injection point.
     */
    public static void setShortsLikeFeedback(LottieAnimationView lottieAnimationView) {
        if (CURRENT_TYPE == ORIGINAL) {
            return;
        }

        setLottieAnimationRawResources(lottieAnimationView, CURRENT_TYPE.rawRes);
    }

    /**
     * Injection point.
     */
    public static void setShortsPauseFeedback(LottieAnimationView lottieAnimationView) {
        if (!Settings.HIDE_SHORTS_PLAY_PAUSE_BUTTON_BACKGROUND.get()) {
            return;
        }
        int pauseTapFeedbackHidden =
                ResourceUtils.getRawIdentifier("pause_tap_feedback_hidden");
        if (pauseTapFeedbackHidden != 0) {
            setLottieAnimationRawResources(
                    lottieAnimationView,
                    pauseTapFeedbackHidden
            );
        }
    }

    /**
     * Injection point.
     */
    public static void setShortsPlayFeedback(LottieAnimationView lottieAnimationView) {
        if (!Settings.HIDE_SHORTS_PLAY_PAUSE_BUTTON_BACKGROUND.get()) {
            return;
        }
        int playTapFeedbackHidden =
                ResourceUtils.getRawIdentifier("play_tap_feedback_hidden");
        if (playTapFeedbackHidden != 0) {
            setLottieAnimationRawResources(
                    lottieAnimationView,
                    playTapFeedbackHidden
            );
        }
    }

}
