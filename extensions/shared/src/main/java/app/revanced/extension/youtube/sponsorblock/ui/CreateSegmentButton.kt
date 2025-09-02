package app.revanced.extension.youtube.sponsorblock.ui

import android.view.View
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.PlayerControlButton

object CreateSegmentButton {
    private var instance: PlayerControlButton? = null

    /**
     * injection point
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_sb_create_segment_button",
                hasPlaceholder = false,
                buttonVisibility = { shouldBeShown() },
                onClickListener = { view: View -> onClick(view) }
            )
        } catch (ex: Exception) {
            Logger.printException({ "initializeButton failure" }, ex)
        }
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibilityNegatedImmediate() {
        instance?.setVisibilityNegatedImmediate()
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibilityImmediate(visible: Boolean) {
        instance?.setVisibilityImmediate(visible)
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibility(visible: Boolean, animated: Boolean) {
        instance?.setVisibility(visible, animated)
    }

    private fun onClick(view: View) {
        SponsorBlockViewController.toggleNewSegmentLayoutVisibility()
    }

    private fun shouldBeShown(): Boolean {
        return Settings.SB_ENABLED.get() && Settings.SB_CREATE_NEW_SEGMENT.get()
    }

    @JvmStatic
    fun hideControls() {
        instance?.hide()
    }
}
