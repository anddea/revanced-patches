package app.morphe.extension.youtube.patches.spoof.ui

import android.view.View
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.youtube.patches.spoof.ReloadVideoPatch
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlButton
import app.morphe.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.morphe.extension.youtube.utils.VideoUtils


@Suppress("unused")
object ReloadVideoButton {
    private var instance: PlayerControlButton? = null

    /**
     * injection point
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_reload_video_button",
                hasPlaceholder = false,
                buttonVisibility = { isButtonEnabled() },
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

    private fun isButtonEnabled(): Boolean {
        return Settings.SPOOF_STREAMING_DATA.get()
                && Settings.SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON.get()
                && !isAdProgressTextVisible()
                && (Settings.SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON_ALWAYS_SHOW.get() || ReloadVideoPatch.isProgressBarVisible())
    }

    private fun onClick(view: View) {
        VideoUtils.reloadVideo()
    }
}
