package app.revanced.extension.youtube.swipecontrols

import android.content.Context
import android.graphics.Color
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.LockModeState
import app.revanced.extension.youtube.shared.PlayerType
import app.revanced.extension.youtube.utils.ExtendedUtils.validateValue

/**
 * provider for configuration for volume and brightness swipe controls
 *
 * @param context the context to create in
 */
class SwipeControlsConfigurationProvider(
    private val context: Context,
) {
    // region swipe enable

    /**
     * should swipe controls be enabled? (global setting)
     */
    val enableSwipeControls: Boolean
        get() = isFullscreenVideo && (enableVolumeControls || enableBrightnessControl)

    /**
     * should swipe controls for volume be enabled?
     */
    val enableVolumeControls: Boolean
        get() = Settings.ENABLE_SWIPE_VOLUME.get()

    /**
     * should swipe controls for volume be enabled?
     */
    val enableBrightnessControl: Boolean
        get() = Settings.ENABLE_SWIPE_BRIGHTNESS.get()

    /**
     * is the video player currently in fullscreen mode?
     */
    private val isFullscreenVideo: Boolean
        get() = PlayerType.current == PlayerType.WATCH_WHILE_FULLSCREEN

    /**
     * is the video player currently in lock mode?
     */
    val isScreenLocked: Boolean
        get() = LockModeState.current.isLocked()

    val enableSwipeControlsLockMode: Boolean
        get() = Settings.SWIPE_LOCK_MODE.get()

    // endregion

    // region keys enable

    /**
     * should volume key controls be overwritten? (global setting)
     */
    val overwriteVolumeKeyControls: Boolean
        get() = isFullscreenVideo && enableVolumeControls

    // endregion

    // region gesture adjustments

    /**
     * should press-to-swipe be enabled?
     */
    val shouldEnablePressToSwipe: Boolean
        get() = Settings.ENABLE_SWIPE_PRESS_TO_ENGAGE.get()

    /**
     * threshold for swipe detection
     * this may be called rapidly in onScroll, so we have to load it once and then leave it constant
     */
    val swipeMagnitudeThreshold: Int
        get() = Settings.SWIPE_MAGNITUDE_THRESHOLD.get()

    /**
     * swipe distances for brightness
     */
    val brightnessDistance: Float
        get() = validateValue(
            Settings.SWIPE_BRIGHTNESS_SENSITIVITY,
            1,
            1000,
            "revanced_swipe_brightness_sensitivity_invalid_toast"
        ).toFloat() / 100 // 1f

    /**
     * swipe distances for volume
     */
    val volumeDistance: Float
        get() = validateValue(
            Settings.SWIPE_VOLUME_SENSITIVITY,
            1,
            1000,
            "revanced_swipe_volume_sensitivity_invalid_toast"
        ).toFloat() / 100 * 10 // 10f

    // endregion

    // region overlay adjustments

    /**
     * should the overlay enable haptic feedback?
     */
    val shouldEnableHapticFeedback: Boolean
        get() = Settings.ENABLE_SWIPE_HAPTIC_FEEDBACK.get()

    /**
     * how long the overlay should be shown on changes
     */
    val overlayShowTimeoutMillis: Long
        get() = Settings.SWIPE_OVERLAY_TIMEOUT.get()

    /**
     * text size for the overlay, in sp
     */
    val overlayTextSize: Int
        get() = Settings.SWIPE_OVERLAY_TEXT_SIZE.get()

    /**
     * get the background color for text on the overlay, as a color int
     */
    val overlayTextBackgroundColor: Int
        get() = Color.argb(Settings.SWIPE_OVERLAY_BACKGROUND_ALPHA.get(), 0, 0, 0)

    /**
     * get the foreground color for text on the overlay, as a color int
     */
    val overlayForegroundColor: Int
        get() = Color.WHITE

    // endregion

    // region behaviour

    /**
     * should the brightness be saved and restored when exiting or entering fullscreen
     */
    val shouldSaveAndRestoreBrightness: Boolean
        get() = Settings.ENABLE_SAVE_AND_RESTORE_BRIGHTNESS.get()

    /**
     * should auto-brightness be enabled at the lowest value of the brightness gesture
     */
    val shouldLowestValueEnableAutoBrightness: Boolean
        get() = Settings.ENABLE_SWIPE_LOWEST_VALUE_AUTO_BRIGHTNESS.get()

    /**
     * variable that stores the brightness gesture value in the settings
     */
    var savedScreenBrightnessValue: Float
        get() = Settings.SWIPE_BRIGHTNESS_VALUE.get()
        set(value) = Settings.SWIPE_BRIGHTNESS_VALUE.save(value)

    // endregion

}