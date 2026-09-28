/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public class OpenVideosFullscreenHookPatch {

    public enum OpenFullscreenMode {
        DISABLED,
        PORTRAIT,
        LANDSCAPE,
    }

    private static volatile boolean isForeground = true;

    @NonNull
    private static String videoId = "";

    public interface FullscreenInterface {
        void patch_exitFullscreen();
        void patch_enterFullscreen();
    }

    private static volatile WeakReference<FullscreenInterface> fullscreenInterfaceRef = new WeakReference<>(null);

    @Nullable
    private static volatile Boolean openNextVideoFullscreen;

    public static void setFullscreenInterface(FullscreenInterface fullscreenInterface) {
        fullscreenInterfaceRef = new WeakReference<>(fullscreenInterface);
    }

    public static void setOpenNextVideoFullscreen(@Nullable Boolean forceFullScreen) {
        openNextVideoFullscreen = forceFullScreen;
    }

    public static void exitFullscreenMode() {
        FullscreenInterface screenInterface = fullscreenInterfaceRef.get();
        if (screenInterface == null) {
            Logger.printException(() -> "Cannot exit fullscreen mode (interface is null)");
            return;
        }

        Logger.printDebug(() -> "Exiting fullscreen mode");
        screenInterface.patch_exitFullscreen();
    }

    /**
     * Changed during patching since this class is also
     * used by {@link OpenVideosFullscreenHookPatch}.
     */
    private static boolean isPatchIncluded() {
        return false;  // Modified during patching.
    }

    /**
     * Injection point.
     * <p>
     * Returns negated value.
     */
    public static boolean doNotOpenVideoFullscreenPortrait(boolean original) {
        Boolean openFullscreen = openNextVideoFullscreen;
        if (openFullscreen != null) {
            openNextVideoFullscreen = null;
            return !openFullscreen;
        }

        if (!isPatchIncluded()) {
            return original;
        }

        return Settings.OPEN_VIDEOS_FULLSCREEN.get() != OpenFullscreenMode.PORTRAIT;
    }

    private static boolean shouldEnterFullscreen;

    /**
     * Injection point.
     */
    public static void initialize() {
        shouldEnterFullscreen = Settings.OPEN_VIDEOS_FULLSCREEN.get() == OpenFullscreenMode.LANDSCAPE;
    }

    /**
     * Injection point.
     */
    public static void onAppBackgrounded() {
        isForeground = false;
    }

    /**
     * Injection point.
     */
    public static void onAppForegrounded() {
        isForeground = true;
    }

    /**
     * Injection point for targets below 21.13.
     */
    public static void enterFullscreen(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        try {
            if (!shouldEnterFullscreen) {
                return;
            }
            PlayerType playerType = PlayerType.getCurrent();
            // 1. The user opened the video while playing a video in the feed.
            // 2. This is a valid request, so the videoId is not saved.
            if (playerType == PlayerType.INLINE_MINIMAL) {
                return;
            }
            if (videoId.equals(newlyLoadedVideoId)) {
                return;
            }
            videoId = newlyLoadedVideoId;
            shouldEnterFullscreen = false;

            // 1. User clicks home button in [PlayerType.WATCH_WHILE_MAXIMIZED], thus entering audio only mode.
            // 2. PlayerType is still [PlayerType.WATCH_WHILE_MAXIMIZED].
            // 3. Next video starts in audio only mode, then returns to foreground mode.
            // 4. Enters fullscreen for a moment and then returns.
            // We can prevent this by checking if the app is in the foreground.
            if (playerType == PlayerType.WATCH_WHILE_MAXIMIZED && isForeground) {
                // It works without delay, but in this case sometimes portrait videos have landscape orientation.
                Utils.runOnMainThreadDelayed(VideoUtils::enterFullscreenMode, 250L);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "enterFullscreen failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void playerStatusChanged(Enum<?> status) {
        try {
            if (!shouldEnterFullscreen) return;
            if (status == null || !"VIDEO_PLAYING".equals(status.name())) return;

            if (PlayerType.getCurrent() == PlayerType.WATCH_WHILE_FULLSCREEN) {
                shouldEnterFullscreen = false;
                return;
            }
            shouldEnterFullscreen = false;

            FullscreenInterface screenInterface = fullscreenInterfaceRef.get();
            if (screenInterface == null) {
                Logger.printException(() -> "Cannot enter fullscreen (interface is null)");
                return;
            }

            Logger.printDebug(() -> "Opening video fullscreen");
            Utils.verifyOnMainThread();
            screenInterface.patch_enterFullscreen();
        } catch (Exception ex) {
            Logger.printException(() -> "playerStatusChanged failure", ex);
        }
    }

}
