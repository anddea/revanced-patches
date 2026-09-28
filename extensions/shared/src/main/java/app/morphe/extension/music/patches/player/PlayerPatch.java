/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.music.patches.player;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.hideViewByRemovingFromParentUnderCondition;
import static app.morphe.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.morphe.extension.shared.utils.Utils.isSDKAbove;
import static app.morphe.extension.shared.utils.Utils.runOnMainThreadDelayed;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.shared.VideoType;
import app.morphe.extension.music.utils.VideoUtils;
import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class PlayerPatch {
    /** Suppresses trailing post-dismiss updates from restoring the previous album color. */
    private static final long MINIPLAYER_DISMISS_WINDOW_MS = 1500L;

    @Nullable
    private static volatile Integer lastMiniplayerColor;
    @Nullable
    private static volatile Integer initialCapturedMiniplayerColor;
    private static volatile WeakReference<View> navigationBarRef = new WeakReference<>(null);
    @Nullable
    private static volatile Integer defaultNavigationBarColor;
    private static volatile long miniplayerDismissWindowUntilMs;

    private static final boolean ADD_MINIPLAYER_NEXT_BUTTON =
            Settings.ADD_MINIPLAYER_NEXT_BUTTON.get();
    private static final boolean ADD_MINIPLAYER_PREVIOUS_BUTTON =
            Settings.ADD_MINIPLAYER_PREVIOUS_BUTTON.get();
    private static final boolean CHANGE_PLAYER_BACKGROUND_COLOR =
            Settings.CHANGE_PLAYER_BACKGROUND_COLOR.get();
    private static final boolean CHANGE_SEEK_BAR_POSITION =
            Settings.CHANGE_SEEK_BAR_POSITION.get();
    private static final boolean DISABLE_PLAYER_GESTURE =
            Settings.DISABLE_PLAYER_GESTURE.get();
    private static final boolean ENABLE_SMOOTH_TRANSITION_ANIMATION =
            Settings.ENABLE_SMOOTH_TRANSITION_ANIMATION.get();
    private static final boolean ENABLE_SWIPE_TO_DISMISS_MINIPLAYER =
            Settings.ENABLE_SWIPE_TO_DISMISS_MINIPLAYER.get();
    private static final boolean ENABLE_THICK_SEEKBAR =
            Settings.ENABLE_THICK_SEEKBAR.get();
    private static final boolean ENABLE_ZEN_MODE =
            Settings.ENABLE_ZEN_MODE.get();
    private static final boolean ENABLE_ZEN_MODE_PODCAST =
            Settings.ENABLE_ZEN_MODE_PODCAST.get();
    private static final boolean HIDE_DOUBLE_TAP_OVERLAY_FILTER =
            Settings.HIDE_DOUBLE_TAP_OVERLAY_FILTER.get();
    private static final boolean HIDE_FULLSCREEN_SHARE_BUTTON =
            Settings.HIDE_FULLSCREEN_SHARE_BUTTON.get();
    private static final boolean HIDE_SONG_VIDEO_TOGGLE =
            Settings.HIDE_SONG_VIDEO_TOGGLE.get();
    private static final boolean RESTORE_OLD_COMMENTS_POPUP_PANELS =
            Settings.RESTORE_OLD_COMMENTS_POPUP_PANELS.get();
    private static final boolean SETTINGS_INITIALIZED =
            Settings.SETTINGS_INITIALIZED.get();

    private static final StringSetting CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY =
            Settings.CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY;
    private static final StringSetting CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY =
            Settings.CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY;

    private static final int ZEN_MODE_BACKGROUND_COLOR = 0xFF404040;
    private static final int MUSIC_VIDEO_BACKGROUND_COLOR = 0xFF030303;

    private static final int[] MUSIC_VIDEO_GRADIENT_COLORS = {MUSIC_VIDEO_BACKGROUND_COLOR, MUSIC_VIDEO_BACKGROUND_COLOR};
    private static final int[] ZEN_MODE_GRADIENT_COLORS = {ZEN_MODE_BACKGROUND_COLOR, ZEN_MODE_BACKGROUND_COLOR};
    private static final int[] customColorGradient = new int[2];
    private static boolean colorInitalized = false;

    private static WeakReference<View> previousButtonViewRef = new WeakReference<>(null);
    private static WeakReference<View> nextButtonViewRef = new WeakReference<>(null);
    private static int previousButtonId;
    private static int nextButtonId;

    static {
        if (CHANGE_PLAYER_BACKGROUND_COLOR)
            loadPlayerbackgroundColor();
    }

    private static void loadPlayerbackgroundColor() {
        try {
            customColorGradient[0] = Color.parseColor(CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY.get());
            customColorGradient[1] = Color.parseColor(CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY.get());
            colorInitalized = true;
        } catch (Exception ex) {
            Utils.showToastShort(str("revanced_custom_player_background_invalid_toast"));
            Utils.showToastShort(str("revanced_reset_to_default_toast"));
            CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY.resetToDefault();
            CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY.resetToDefault();

            loadPlayerbackgroundColor();
        }
    }

    public static boolean addMiniPlayerNextButton(boolean original) {
        return !ADD_MINIPLAYER_NEXT_BUTTON && original;
    }

    public static boolean changeMiniPlayerColor() {
        return Settings.CHANGE_MINIPLAYER_COLOR.get();
    }

    /** Stores the resolved miniplayer color and immediately applies it to the navigation bar. */
    public static void setLastMiniplayerColor(int color) {
        if (SystemClock.uptimeMillis() < miniplayerDismissWindowUntilMs) return;

        final Integer initial = initialCapturedMiniplayerColor;
        if (initial == null) {
            initialCapturedMiniplayerColor = color;
            return;
        }

        if (color == initial) {
            if (lastMiniplayerColor == null) return;
            lastMiniplayerColor = null;
            final Integer defaultColor = defaultNavigationBarColor;
            if (defaultColor != null) postNavigationBarColor(defaultColor);
            return;
        }

        lastMiniplayerColor = color;
        applyToNavigationBar(color);
    }

    /** Remembers the navigation bar and its theme color for immediate repainting. */
    public static void registerNavigationBar(View view, int defaultColor) {
        navigationBarRef = new WeakReference<>(view);
        defaultNavigationBarColor = defaultColor;
    }

    /** Overrides the navigation bar background while miniplayer color matching is enabled. */
    public static int overrideNavigationBarColor(int defaultColor) {
        final Integer color = lastMiniplayerColor;
        return color != null && matchNavigationBarEnabled() ? color : defaultColor;
    }

    /** Clears the cached tint after the queue/miniplayer is dismissed. */
    public static void onMiniplayerDismissed() {
        lastMiniplayerColor = null;
        miniplayerDismissWindowUntilMs = SystemClock.uptimeMillis() + MINIPLAYER_DISMISS_WINDOW_MS;
        final Integer defaultColor = defaultNavigationBarColor;
        if (defaultColor != null) postNavigationBarColor(defaultColor);
    }

    private static void applyToNavigationBar(int color) {
        if (!Settings.CHANGE_NAVIGATION_BAR_COLOR.get()) return;
        postNavigationBarColor(color);
    }

    private static void postNavigationBarColor(int color) {
        View view = navigationBarRef.get();
        if (view != null) view.post(() -> view.setBackgroundColor(color));
    }

    private static boolean matchNavigationBarEnabled() {
        return Settings.CHANGE_MINIPLAYER_COLOR.get()
                && Settings.CHANGE_NAVIGATION_BAR_COLOR.get();
    }

    /** Returns the selected app theme when dynamic miniplayer color matching is disabled. */
    public static int getMiniPlayerThemeColor() {
        return BaseThemeUtils.getThemeDarkColor();
    }

    public static int[] changePlayerBackgroundColor(int[] colors) {
        if (Arrays.equals(MUSIC_VIDEO_GRADIENT_COLORS, colors)) {
            final VideoType videoType = VideoType.getCurrent();
            final boolean isZenMode = ENABLE_ZEN_MODE &&
                    (videoType.isMusicVideo() || (videoType.isPodCast() && ENABLE_ZEN_MODE_PODCAST));
            if (isZenMode) {
                return ZEN_MODE_GRADIENT_COLORS;
            }
        }
        if (CHANGE_PLAYER_BACKGROUND_COLOR && colorInitalized) {
            return customColorGradient;
        }

        return colors;
    }

    public static boolean changeSeekBarPosition(boolean original) {
        return SETTINGS_INITIALIZED
                ? CHANGE_SEEK_BAR_POSITION
                : original;
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

    /**
     * Registers miniplayer button listeners on 8.51+, where the legacy pending-intent listener
     * fingerprints no longer exist. Media key events match the mechanism used by headsets.
     */
    public static void setPreviousNextButtonOnClickListener(View view) {
        int previousButtonViewId = getPreviousButtonId();
        if (previousButtonViewId != 0) {
            View previousButtonView = view.findViewById(previousButtonViewId);
            hideViewUnderCondition(!ADD_MINIPLAYER_PREVIOUS_BUTTON, previousButtonView);
            previousButtonView.setOnClickListener(v ->
                    dispatchMediaKeyEvent(v.getContext(), KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        }

        int nextButtonViewId = getNextButtonId();
        if (nextButtonViewId != 0) {
            View nextButtonView = view.findViewById(nextButtonViewId);
            hideViewUnderCondition(!ADD_MINIPLAYER_NEXT_BUTTON, nextButtonView);
            nextButtonView.setOnClickListener(v ->
                    dispatchMediaKeyEvent(v.getContext(), KeyEvent.KEYCODE_MEDIA_NEXT));
        }
    }

    /**
     * Extends the miniplayer's managed view array with the injected buttons on 8.51+.
     */
    public static View[] setPreviousNextButton(View view, View[] original) {
        View previousButtonView = null;
        View nextButtonView = null;

        int previousButtonViewId = getPreviousButtonId();
        if (previousButtonViewId != 0) {
            previousButtonView = view.findViewById(previousButtonViewId);
        }
        int nextButtonViewId = getNextButtonId();
        if (nextButtonViewId != 0) {
            nextButtonView = view.findViewById(nextButtonViewId);
        }

        int extraCount = (nextButtonView != null ? 1 : 0) + (previousButtonView != null ? 1 : 0);
        if (extraCount == 0) return original;

        View[] newArray = new View[original.length + extraCount];
        System.arraycopy(original, 0, newArray, 0, original.length);

        int i = original.length;
        if (previousButtonView != null) newArray[i++] = previousButtonView;
        if (nextButtonView != null) newArray[i] = nextButtonView;

        return newArray;
    }

    private static void dispatchMediaKeyEvent(Context context, int keyCode) {
        if (context.getSystemService(Context.AUDIO_SERVICE) instanceof AudioManager audioManager) {
            try {
                long now = SystemClock.uptimeMillis();
                audioManager.dispatchMediaKeyEvent(
                        new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
                audioManager.dispatchMediaKeyEvent(
                        new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
            } catch (Exception ex) {
                Logger.printException(() -> "dispatchMediaKeyEvent failure", ex);
            }
        }
    }

    private static int getPreviousButtonId() {
        if (previousButtonId == 0) {
            previousButtonId = ResourceUtils.getIdentifier(
                    "mini_player_previous_button", ResourceType.ID);
        }
        return previousButtonId;
    }

    private static int getNextButtonId() {
        if (nextButtonId == 0) {
            nextButtonId = ResourceUtils.getIdentifier("mini_player_next_button", ResourceType.ID);
        }
        return nextButtonId;
    }

    // rest of the implementation added by patch.
    private static void previousButtonClicked(View view) {
        // These instructions are ignored by patch.
        Logger.printDebug(() -> "previous button clicked: " + view);
    }

    public static boolean enableSmoothTransitionAnimation(boolean original) {
        return SETTINGS_INITIALIZED
                ? ENABLE_SMOOTH_TRANSITION_ANIMATION
                : original;
    }

    public static boolean enableSmoothTransitionAnimationInverted(boolean original) {
        return SETTINGS_INITIALIZED
                ? !ENABLE_SMOOTH_TRANSITION_ANIMATION
                : original;
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

    public static boolean enableThickSeekBar(boolean original) {
        return SETTINGS_INITIALIZED
                ? ENABLE_THICK_SEEKBAR
                : original;
    }

    public static int enableZenMode(int originalColor) {
        if (ENABLE_ZEN_MODE && originalColor == MUSIC_VIDEO_BACKGROUND_COLOR) {
            final VideoType videoType = VideoType.getCurrent();
            if (videoType.isMusicVideo() || (videoType.isPodCast() && ENABLE_ZEN_MODE_PODCAST)) {
                return ZEN_MODE_BACKGROUND_COLOR;
            }
        }
        return originalColor;
    }

    public static boolean hideSongVideoToggle(boolean original) {
        return HIDE_SONG_VIDEO_TOGGLE && original;
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
        if (Settings.REMEMBER_SHUFFLE_SATE.get()) {
            Settings.ALWAYS_SHUFFLE.save(shuffleState.ordinal() == 1);
        }
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
        return SETTINGS_INITIALIZED
                ? !RESTORE_OLD_COMMENTS_POPUP_PANELS && original
                : original;
    }

    public static boolean restoreOldPlayerBackground(boolean original) {
        if (!SETTINGS_INITIALIZED) {
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
        if (!SETTINGS_INITIALIZED) {
            return original;
        }
        return !Settings.RESTORE_OLD_PLAYER_LAYOUT.get();
    }

}
