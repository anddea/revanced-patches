package app.revanced.extension.youtube.swipecontrols

import android.graphics.Color
import androidx.core.graphics.toColorInt
import app.revanced.extension.shared.settings.StringSetting
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.StringRef.str
import app.revanced.extension.shared.utils.Utils
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.LockModeState
import app.revanced.extension.youtube.shared.PlayerType

/**
 * Provides configuration settings for volume, brightness, speed, and seek swipe controls in the YouTube player.
 * Manages enabling/disabling gestures, overlay appearance, and behavior preferences.
 */
class SwipeControlsConfigurationProvider {
    // region swipe enable

    /**
     * Indicates whether swipe controls are enabled globally.
     * Returns true if any control (volume, brightness, speed, or seek) is enabled and the video is in fullscreen mode.
     */
    val enableSwipeControls: Boolean
        get() = (enableVolumeControls || enableBrightnessControl || enableSpeedControl || enableSeekControl) && isFullscreenVideo

    /**
     * Indicates whether swipe controls for adjusting volume are enabled.
     */
    val enableVolumeControls = Settings.SWIPE_VOLUME.get()

    /**
     * Indicates whether swipe controls for adjusting brightness are enabled.
     */
    val enableBrightnessControl = Settings.SWIPE_BRIGHTNESS.get()

    /**
     * Indicates whether swipe controls for adjusting playback speed are enabled.
     */
    val enableSpeedControl = Settings.SWIPE_SPEED.get()

    /**
     * Indicates whether swipe controls for seeking are enabled.
     */
    val enableSeekControl = Settings.SWIPE_SEEK.get()

    /**
     * Checks if the video player is currently in fullscreen mode.
     */
    private val isFullscreenVideo: Boolean
        get() = PlayerType.current == PlayerType.WATCH_WHILE_FULLSCREEN

    /**
     * Checks if the video player is currently in lock mode.
     */
    val isScreenLocked: Boolean
        get() = LockModeState.current.isLocked()

    /**
     * Indicates whether swipe controls are enabled in lock mode.
     */
    val enableSwipeControlsLockMode = Settings.SWIPE_LOCK_MODE.get()

    // endregion

    // region keys enable

    /**
     * Indicates whether volume key controls should be overridden by swipe controls.
     * Returns true if volume controls are enabled and the video is in fullscreen mode.
     */
    val overwriteVolumeKeyControls: Boolean
        get() = enableVolumeControls && isFullscreenVideo

    // endregion

    // region gesture adjustments

    /**
     * Indicates whether press-to-swipe mode is enabled, requiring a press before swiping to activate controls.
     */
    val shouldEnablePressToSwipe = Settings.SWIPE_PRESS_TO_ENGAGE.get()

    /**
     * The threshold for detecting swipe gestures, in pixels.
     * Loaded once to ensure consistent behavior during rapid scroll events.
     */
    val swipeMagnitudeThreshold = Settings.SWIPE_MAGNITUDE_THRESHOLD.get()

    /**
     * The sensitivity of brightness swipe gestures, determining how much brightness changes per swipe.
     * Range: 1–1000; resets to default if invalid.
     */
    val brightnessDistance: Float by lazy {
        val sensitivity = Settings.SWIPE_BRIGHTNESS_SENSITIVITY.get()
        if (sensitivity < 1 || sensitivity > 1000) {
            Utils.showToastLong(str("revanced_swipe_brightness_sensitivity_invalid_toast"))
            return@lazy Settings.SWIPE_BRIGHTNESS_SENSITIVITY.resetToDefault().toFloat() / 100
        }
        sensitivity.toFloat() / 100
    }

    /**
     * The sensitivity of volume swipe gestures, determining how much volume changes per swipe.
     * Range: 1–1000; resets to default if invalid.
     */
    val volumeDistance: Float by lazy {
        val sensitivity = Settings.SWIPE_VOLUME_SENSITIVITY.get()
        if (sensitivity < 1 || sensitivity > 1000) {
            Utils.showToastLong(str("revanced_swipe_volume_sensitivity_invalid_toast"))
            return@lazy Settings.SWIPE_VOLUME_SENSITIVITY.resetToDefault().toFloat() / 100 * 10
        }
        sensitivity.toFloat() / 100 * 10
    }

    /**
     * The sensitivity of speed swipe gestures, determining how much playback speed changes per swipe.
     * Range: 1–1000; resets to default if invalid.
     */
    val speedDistance: Float by lazy {
        val sensitivity = Settings.SWIPE_SPEED_SENSITIVITY.get()
        if (sensitivity < 1 || sensitivity > 1000) {
            Utils.showToastLong(str("revanced_swipe_speed_sensitivity_invalid_toast"))
            return@lazy Settings.SWIPE_SPEED_SENSITIVITY.resetToDefault().toFloat() / 100 * 10
        }
        sensitivity.toFloat() / 100 * 10
    }

    /**
     * The sensitivity of seek swipe gestures, determining how much seek time changes per swipe.
     * Range: 1–1000; resets to default if invalid.
     */
    val seekDistance: Float by lazy {
        val sensitivity = Settings.SWIPE_SEEK_SENSITIVITY.get()
        if (sensitivity < 1 || sensitivity > 1000) {
            Utils.showToastLong(str("revanced_swipe_seek_sensitivity_invalid_toast"))
            return@lazy Settings.SWIPE_SEEK_SENSITIVITY.resetToDefault().toFloat() / 100 * 10
        }
        sensitivity.toFloat() / 100 * 10
    }

    // endregion

    // region overlay adjustments

    /**
     * Indicates whether haptic feedback should be enabled for swipe control interactions.
     */
    val shouldEnableHapticFeedback = Settings.SWIPE_HAPTIC_FEEDBACK.get()

    /**
     * The duration in milliseconds that the overlay should remain visible after a change.
     */
    val overlayShowTimeoutMillis = Settings.SWIPE_OVERLAY_TIMEOUT.get()

    /**
     * The background opacity of the overlay, converted from a percentage (0-100) to an alpha value (0-255).
     * Resets to default and shows a toast if the value is out of range.
     */
    val overlayBackgroundOpacity: Int by lazy {
        var opacity = Settings.SWIPE_OVERLAY_OPACITY.get()

        if (opacity < 0 || opacity > 100) {
            Utils.showToastLong(str("revanced_swipe_overlay_background_opacity_invalid_toast"))
            opacity = Settings.SWIPE_OVERLAY_OPACITY.resetToDefault()
        }

        opacity = opacity * 255 / 100
        Color.argb(opacity, 0, 0, 0)
    }

    /**
     * The color of the progress bar in the overlay for brightness.
     * Resets to default and shows a toast if the color string is invalid or empty.
     */
    val overlayBrightnessProgressColor: Int by lazy {
        getSettingColor(Settings.SWIPE_OVERLAY_BRIGHTNESS_COLOR)
    }

    /**
     * The color of the progress bar in the overlay for volume.
     * Resets to default and shows a toast if the color string is invalid or empty.
     */
    val overlayVolumeProgressColor: Int by lazy {
        getSettingColor(Settings.SWIPE_OVERLAY_VOLUME_COLOR)
    }

    /**
     * The color of the progress bar in the overlay for speed.
     * Resets to default and shows a toast if the color string is invalid or empty.
     */
    val overlaySpeedProgressColor: Int by lazy {
        getSettingColor(Settings.SWIPE_OVERLAY_SPEED_COLOR)
    }

    /**
     * The color of the progress bar in the overlay for seeking.
     * Resets to default and shows a toast if the color string is invalid or empty.
     */
    val overlaySeekProgressColor: Int by lazy {
        getSettingColor(Settings.SWIPE_OVERLAY_SEEK_COLOR)
    }

    private fun getSettingColor(setting: StringSetting): Int {
        try {
            val color = setting.get().toColorInt()
            return (0xBF000000.toInt() or (color and 0x00FFFFFF))
        } catch (ex: IllegalArgumentException) {
            Logger.printDebug({ "Could not parse color: $setting" }, ex)
            Utils.showToastLong(str("revanced_settings_color_invalid"))
            setting.resetToDefault()
            return getSettingColor(setting) // Recursively return
        }
    }

    /**
     * The background color used for the filled portion of the progress bar in the overlay.
     */
    val overlayFillBackgroundPaint = 0x80D3D3D3.toInt()

    /**
     * The color used for text and icons in the overlay.
     */
    val overlayTextColor = Color.WHITE

    /**
     * The text size in the overlay, in density-independent pixels (dp).
     * Must be between 1 and 30 dp; resets to default and shows a toast if invalid.
     */
    val overlayTextSize: Int by lazy {
        val size = Settings.SWIPE_OVERLAY_TEXT_SIZE.get()
        if (size < 1 || size > 30) {
            Utils.showToastLong(str("revanced_swipe_text_overlay_size_invalid_toast"))
            return@lazy Settings.SWIPE_OVERLAY_TEXT_SIZE.resetToDefault()
        }
        size
    }

    /**
     * Defines the style of the swipe controls overlay, determining its layout and appearance.
     *
     * @property isMinimal Indicates whether the style is minimalistic, omitting detailed progress indicators.
     * @property isHorizontalMinimalCenter Indicates whether the style is a minimal horizontal bar centered vertically.
     * @property isCircular Indicates whether the style uses a circular progress bar.
     * @property isVertical Indicates whether the style uses a vertical progress bar.
     */
    @Suppress("unused")
    enum class SwipeOverlayStyle(
        val isMinimal: Boolean = false,
        val isHorizontalMinimalCenter: Boolean = false,
        val isCircular: Boolean = false,
        val isVertical: Boolean = false
    ) {
        HORIZONTAL,
        HORIZONTAL_MINIMAL_TOP(isMinimal = true),
        HORIZONTAL_MINIMAL_CENTER(isMinimal = true, isHorizontalMinimalCenter = true),
        CIRCULAR(isCircular = true),
        CIRCULAR_MINIMAL(isMinimal = true, isCircular = true),
        VERTICAL(isVertical = true),
        VERTICAL_MINIMAL(isMinimal = true, isVertical = true)
    }

    /**
     * The current style of the overlay, determining its layout and appearance.
     */
    val overlayStyle = Settings.SWIPE_OVERLAY_STYLE.get()

    // endregion

    // region behaviour

    /**
     * Indicates whether the brightness level should be saved and restored when entering or exiting fullscreen mode.
     */
    val shouldSaveAndRestoreBrightness = Settings.SWIPE_SAVE_AND_RESTORE_BRIGHTNESS.get()

    /**
     * Indicates whether auto-brightness should be enabled when the brightness gesture reaches its lowest value.
     */
    val shouldLowestValueEnableAutoBrightness = Settings.SWIPE_LOWEST_VALUE_ENABLE_AUTO_BRIGHTNESS.get()

    /**
     * The saved brightness value for the swipe gesture, used to restore brightness in fullscreen mode.
     */
    var savedScreenBrightnessValue: Float
        get() = Settings.SWIPE_BRIGHTNESS_VALUE.get()
        set(value) = Settings.SWIPE_BRIGHTNESS_VALUE.save(value)

    // endregion
}
