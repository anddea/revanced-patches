package app.revanced.extension.youtube.patches.spoof.ui

import android.view.View
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.youtube.patches.spoof.ReloadVideoPatch
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.PlayerControlButton
import app.revanced.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.revanced.extension.youtube.utils.VideoUtils


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
