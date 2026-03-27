package app.morphe.extension.youtube.patches.swipe;

import java.util.List;

import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "deprecation"})
public class SwipeControlsPatch {
    /**
     * Injection point.
     */
    public static boolean disableHDRAutoBrightness() {
        return Settings.DISABLE_HDR_AUTO_BRIGHTNESS.get();
    }

    /**
     * Injection point.
     */
    public static boolean disableSwipeToEnterFullscreenModeBelowThePlayer() {
        return !Settings.DISABLE_SWIPE_TO_ENTER_FULLSCREEN_MODE_BELOW_THE_PLAYER.get();
    }

    /**
     * Injection point.
     */
    public static boolean disableSwipeToEnterFullscreenModeInThePlayer(boolean original) {
        return !Settings.DISABLE_SWIPE_TO_ENTER_FULLSCREEN_MODE_IN_THE_PLAYER.get() && original;
    }

    /**
     * Injection point.
     */
    public static boolean disableSwipeToExitFullscreenMode(boolean original) {
        return !Settings.DISABLE_SWIPE_TO_EXIT_FULLSCREEN_MODE.get() && original;
    }

    /**
     * Injection point.
     */
    public static boolean enableSwipeToSwitchVideo() {
        return Settings.ENABLE_SWIPE_TO_SWITCH_VIDEO.get();
    }

    public static final class SwipeOverlayBrightnessColorAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return Settings.SWIPE_BRIGHTNESS.get() &&
                    !Settings.SWIPE_OVERLAY_STYLE.get().isLegacy();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(Settings.SWIPE_BRIGHTNESS, Settings.SWIPE_OVERLAY_STYLE);
        }
    }

    public static final class SwipeOverlayVolumeColorAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return Settings.SWIPE_VOLUME.get() &&
                    !Settings.SWIPE_OVERLAY_STYLE.get().isLegacy();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(Settings.SWIPE_VOLUME, Settings.SWIPE_OVERLAY_STYLE);
        }
    }

    public static final class SwipeOverlaySpeedColorAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return Settings.SWIPE_SPEED.get() &&
                    !Settings.SWIPE_OVERLAY_STYLE.get().isLegacy();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(Settings.SWIPE_SPEED, Settings.SWIPE_OVERLAY_STYLE);
        }
    }

    public static final class SwipeOverlaySeekColorAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return Settings.SWIPE_SEEK.get() &&
                    !Settings.SWIPE_OVERLAY_STYLE.get().isLegacy();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(Settings.SWIPE_SEEK, Settings.SWIPE_OVERLAY_STYLE);
        }
    }
}
