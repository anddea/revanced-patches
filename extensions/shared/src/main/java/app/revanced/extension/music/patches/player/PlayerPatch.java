package app.revanced.extension.music.patches.player;

import static app.revanced.extension.shared.utils.Utils.hideViewByRemovingFromParentUnderCondition;
import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;
import static app.revanced.extension.shared.utils.Utils.runOnMainThreadDelayed;

import android.graphics.Color;
import android.view.View;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.shared.VideoType;
import app.revanced.extension.music.utils.VideoUtils;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class PlayerPatch {
    private static final boolean ADD_MINIPLAYER_NEXT_BUTTON =
            Settings.ADD_MINIPLAYER_NEXT_BUTTON.get();
    private static final boolean ADD_MINIPLAYER_PREVIOUS_BUTTON =
            Settings.ADD_MINIPLAYER_PREVIOUS_BUTTON.get();
    private static final boolean CHANGE_PLAYER_BACKGROUND_COLOR =
            Settings.CHANGE_PLAYER_BACKGROUND_COLOR.get();
    private static final boolean DISABLE_PLAYER_GESTURE =
            Settings.DISABLE_PLAYER_GESTURE.get();
    private static final boolean ENABLE_SWIPE_TO_DISMISS_MINIPLAYER =
            Settings.ENABLE_SWIPE_TO_DISMISS_MINIPLAYER.get();
    private static final boolean HIDE_DOUBLE_TAP_OVERLAY_FILTER =
            Settings.HIDE_DOUBLE_TAP_OVERLAY_FILTER.get();
    private static final boolean HIDE_FULLSCREEN_SHARE_BUTTON =
            Settings.HIDE_FULLSCREEN_SHARE_BUTTON.get();
    private static final boolean HIDE_SONG_VIDEO_TOGGLE =
            Settings.HIDE_SONG_VIDEO_TOGGLE.get();
    private static final boolean RESTORE_OLD_COMMENTS_POPUP_PANELS =
            Settings.RESTORE_OLD_COMMENTS_POPUP_PANELS.get();

    private static final int MUSIC_VIDEO_GREY_BACKGROUND_COLOR = 0xFF404040;
    private static final int MUSIC_VIDEO_ORIGINAL_BACKGROUND_COLOR = 0xFF030303;

    private static WeakReference<View> previousButtonViewRef = new WeakReference<>(null);
    private static WeakReference<View> nextButtonViewRef = new WeakReference<>(null);

    public static boolean addMiniPlayerNextButton(boolean original) {
        return !ADD_MINIPLAYER_NEXT_BUTTON && original;
    }

    public static boolean changeMiniPlayerColor() {
        return Settings.CHANGE_MINIPLAYER_COLOR.get();
    }

    public static int changePlayerBackgroundColor(int originalColor) {
        return CHANGE_PLAYER_BACKGROUND_COLOR && originalColor != MUSIC_VIDEO_GREY_BACKGROUND_COLOR
                ? Color.BLACK
                : originalColor;
    }

    public static boolean disableMiniPlayerGesture() {
        return Settings.DISABLE_MINIPLAYER_GESTURE.get();
    }

    public static boolean disablePlayerGesture() {
        return DISABLE_PLAYER_GESTURE;
    }

    public static boolean enableForcedMiniPlayer(boolean original) {
        return Settings.ENABLE_FORCED_MINIPLAYER.get() || original;
    }

    public static View[] getViewArray(View[] oldViewArray) {
        View previousButtonView = previousButtonViewRef.get();
        if (previousButtonView != null) {
            oldViewArray = ArrayUtils.add(oldViewArray, previousButtonView);
            View nextButtonView = nextButtonViewRef.get();
            if (nextButtonView != null) {
                oldViewArray = ArrayUtils.add(oldViewArray, nextButtonView);
            }
        }
        return oldViewArray;
    }

    public static void setNextButtonView(View nextButtonView) {
        nextButtonViewRef = new WeakReference<>(nextButtonView);
    }

    public static void setNextButtonOnClickListener(View nextButtonView) {
        if (nextButtonView != null) {
            hideViewUnderCondition(
                    !ADD_MINIPLAYER_NEXT_BUTTON,
                    nextButtonView
            );

            nextButtonView.setOnClickListener(v -> nextButtonClicked(nextButtonView));
        }
    }

    // rest of the implementation added by patch.
    private static void nextButtonClicked(View view) {
        // These instructions are ignored by patch.
        Logger.printDebug(() -> "next button clicked: " + view);
    }

    public static void setPreviousButtonView(View previousButtonView) {
        previousButtonViewRef = new WeakReference<>(previousButtonView);
    }

    public static void setPreviousButtonOnClickListener(View previousButtonView) {
        if (previousButtonView != null) {
            hideViewUnderCondition(
                    !ADD_MINIPLAYER_PREVIOUS_BUTTON,
                    previousButtonView
            );

            previousButtonView.setOnClickListener(v -> previousButtonClicked(previousButtonView));
        }
    }

    // rest of the implementation added by patch.
    private static void previousButtonClicked(View view) {
        // These instructions are ignored by patch.
        Logger.printDebug(() -> "previous button clicked: " + view);
    }

    public static boolean enableSwipeToDismissMiniPlayer() {
        return ENABLE_SWIPE_TO_DISMISS_MINIPLAYER;
    }

    public static boolean enableSwipeToDismissMiniPlayer(boolean original) {
        return !ENABLE_SWIPE_TO_DISMISS_MINIPLAYER && original;
    }

    public static Object enableSwipeToDismissMiniPlayer(Object object) {
        return ENABLE_SWIPE_TO_DISMISS_MINIPLAYER ? null : object;
    }

    public static int enableZenMode(int originalColor) {
        if (Settings.ENABLE_ZEN_MODE.get() && originalColor == MUSIC_VIDEO_ORIGINAL_BACKGROUND_COLOR) {
            if (Settings.ENABLE_ZEN_MODE_PODCAST.get() || !VideoType.getCurrent().isPodCast()) {
                return MUSIC_VIDEO_GREY_BACKGROUND_COLOR;
            }
        }
        return originalColor;
    }

    public static void hideSongVideoToggle(View view, int originalVisibility) {
        view.setVisibility(
                HIDE_SONG_VIDEO_TOGGLE
                        ? View.GONE
                        : originalVisibility
        );
    }

    public static void hideDoubleTapOverlayFilter(View view) {
        hideViewByRemovingFromParentUnderCondition(HIDE_DOUBLE_TAP_OVERLAY_FILTER, view);
    }

    public static int hideFullscreenShareButton(int original) {
        return HIDE_FULLSCREEN_SHARE_BUTTON ? 0 : original;
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
            runOnMainThreadDelayed(VideoUtils::shuffleTracks, 1000);
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
        return !RESTORE_OLD_COMMENTS_POPUP_PANELS && original;
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
