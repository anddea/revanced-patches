package app.revanced.extension.music.patches.player;

import static app.revanced.extension.shared.utils.Utils.hideViewByRemovingFromParentUnderCondition;
import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Objects;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.shared.VideoType;
import app.revanced.extension.music.utils.VideoUtils;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings({"unused"})
public class PlayerPatch {
    private static final int MUSIC_VIDEO_GREY_BACKGROUND_COLOR = -12566464;
    private static final int MUSIC_VIDEO_ORIGINAL_BACKGROUND_COLOR = -16579837;

    @SuppressLint("StaticFieldLeak")
    public static View previousButton;
    @SuppressLint("StaticFieldLeak")
    public static View nextButton;

    public static boolean disableMiniPlayerGesture() {
        return Settings.DISABLE_MINI_PLAYER_GESTURE.get();
    }

    public static boolean disablePlayerGesture() {
        return Settings.DISABLE_PLAYER_GESTURE.get();
    }

    public static boolean enableColorMatchPlayer() {
        return Settings.ENABLE_COLOR_MATCH_PLAYER.get();
    }

    public static int enableBlackPlayerBackground(int originalColor) {
        return Settings.ENABLE_BLACK_PLAYER_BACKGROUND.get()
                && originalColor != MUSIC_VIDEO_GREY_BACKGROUND_COLOR
                ? Color.BLACK
                : originalColor;
    }

    public static boolean enableForceMinimizedPlayer(boolean original) {
        return Settings.ENABLE_FORCE_MINIMIZED_PLAYER.get() || original;
    }

    public static boolean enableMiniPlayerNextButton(boolean original) {
        return !Settings.ENABLE_MINI_PLAYER_NEXT_BUTTON.get() && original;
    }

    public static View[] getViewArray(View[] oldViewArray) {
        if (previousButton != null) {
            if (nextButton != null) {
                return getViewArray(getViewArray(oldViewArray, previousButton), nextButton);
            } else {
                return getViewArray(oldViewArray, previousButton);
            }
        } else {
            return oldViewArray;
        }
    }

    private static View[] getViewArray(View[] oldViewArray, View newView) {
        final int oldViewArrayLength = oldViewArray.length;

        View[] newViewArray = Arrays.copyOf(oldViewArray, oldViewArrayLength + 1);
        newViewArray[oldViewArrayLength] = newView;
        return newViewArray;
    }

    public static void setNextButton(View nextButtonView) {
        if (nextButtonView == null)
            return;

        hideViewUnderCondition(
                !Settings.ENABLE_MINI_PLAYER_NEXT_BUTTON.get(),
                nextButtonView
        );

        nextButtonView.setOnClickListener(PlayerPatch::setNextButtonOnClickListener);
    }

    // rest of the implementation added by patch.
    private static void setNextButtonOnClickListener(View view) {
        if (Settings.ENABLE_MINI_PLAYER_NEXT_BUTTON.get())
            view.getClass();
    }

    public static void setPreviousButton(View previousButtonView) {
        if (previousButtonView == null)
            return;

        hideViewUnderCondition(
                !Settings.ENABLE_MINI_PLAYER_PREVIOUS_BUTTON.get(),
                previousButtonView
        );

        previousButtonView.setOnClickListener(PlayerPatch::setPreviousButtonOnClickListener);
    }

    // rest of the implementation added by patch.
    private static void setPreviousButtonOnClickListener(View view) {
        if (Settings.ENABLE_MINI_PLAYER_PREVIOUS_BUTTON.get())
            view.getClass();
    }

    public static boolean enableSwipeToDismissMiniPlayer() {
        return Settings.ENABLE_SWIPE_TO_DISMISS_MINI_PLAYER.get();
    }

    public static boolean enableSwipeToDismissMiniPlayer(boolean original) {
        return !Settings.ENABLE_SWIPE_TO_DISMISS_MINI_PLAYER.get() && original;
    }

    public static Object enableSwipeToDismissMiniPlayer(Object object) {
        return Settings.ENABLE_SWIPE_TO_DISMISS_MINI_PLAYER.get() ? null : object;
    }

    public static int enableZenMode(int originalColor) {
        if (Settings.ENABLE_ZEN_MODE.get() && originalColor == MUSIC_VIDEO_ORIGINAL_BACKGROUND_COLOR) {
            if (Settings.ENABLE_ZEN_MODE_PODCAST.get() || !VideoType.getCurrent().isPodCast()) {
                return MUSIC_VIDEO_GREY_BACKGROUND_COLOR;
            }
        }
        return originalColor;
    }

    public static void hideAudioVideoSwitchToggle(View view, int originalVisibility) {
        if (Settings.HIDE_AUDIO_VIDEO_SWITCH_TOGGLE.get()) {
            originalVisibility = View.GONE;
        }
        view.setVisibility(originalVisibility);
    }

    public static void hideDoubleTapOverlayFilter(View view) {
        hideViewByRemovingFromParentUnderCondition(Settings.HIDE_DOUBLE_TAP_OVERLAY_FILTER, view);
    }

    public static int hideFullscreenShareButton(int original) {
        return Settings.HIDE_FULLSCREEN_SHARE_BUTTON.get() ? 0 : original;
    }

    public static void setShuffleState(Enum<?> shuffleState) {
        if (!Settings.REMEMBER_SHUFFLE_SATE.get())
            return;
        Settings.ALWAYS_SHUFFLE.save(shuffleState.ordinal() == 1);
    }

    public static void shuffleTracks() {
        shuffleTracks(false);
    }

    public static void shuffleTracksWithDelay() {
        shuffleTracks(true);
    }

    private static void shuffleTracks(boolean needDelay) {
        if (!Settings.ALWAYS_SHUFFLE.get())
            return;

        if (needDelay) {
            Utils.runOnMainThreadDelayed(VideoUtils::shuffleTracks, 1000);
        } else {
            VideoUtils.shuffleTracks();
        }
    }

    public static boolean rememberRepeatState(boolean original) {
        return Settings.REMEMBER_REPEAT_SATE.get() || original;
    }

    public static boolean rememberShuffleState() {
        return Settings.REMEMBER_SHUFFLE_SATE.get();
    }

    public static boolean restoreOldCommentsPopUpPanels() {
        return restoreOldCommentsPopUpPanels(true);
    }

    public static boolean restoreOldCommentsPopUpPanels(boolean original) {
        if (!Settings.SETTINGS_INITIALIZED.get()) {
            return original;
        }
        return !Settings.RESTORE_OLD_COMMENTS_POPUP_PANELS.get() && original;
    }

    public static boolean restoreOldPlayerBackground(boolean original) {
        if (!Settings.SETTINGS_INITIALIZED.get()) {
            return original;
        }
        if (!isSDKAbove(23)) {
            // Disable this patch on Android 5.0 / 5.1 to fix a black play button.
            // Android 5.x have a different design for play button,
            // and if the new background is applied forcibly, the play button turns black.
            // 6.20.51 uses the old background from the beginning, so there is no impact.
            return original;
        }
        return !Settings.RESTORE_OLD_PLAYER_BACKGROUND.get();
    }

    public static boolean restoreOldPlayerLayout(boolean original) {
        if (!Settings.SETTINGS_INITIALIZED.get()) {
            return original;
        }
        return !Settings.RESTORE_OLD_PLAYER_LAYOUT.get();
    }

}
