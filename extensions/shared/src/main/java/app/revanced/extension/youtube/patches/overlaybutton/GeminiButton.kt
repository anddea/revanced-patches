package app.revanced.extension.youtube.patches.overlaybutton

import android.view.View
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.PlayerControlButton
import app.revanced.extension.youtube.utils.GeminiManager
import app.revanced.extension.youtube.utils.VideoUtils

@Suppress("unused")
object GeminiButton {
    private var instance: PlayerControlButton? = null

    /**
     * Injection point.
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_gemini_button",
                buttonVisibility = { Settings.OVERLAY_BUTTON_GEMINI.get() },
                onClickListener = { view: View -> onClick(view) },
                onLongClickListener = { view: View ->
                    onLongClick(view)
                    true
                }
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
        val videoUrl = VideoUtils.getVideoUrl(false)
        GeminiManager.getInstance().startSummarization(view.context, videoUrl)
    }

    private fun onLongClick(view: View) {
        val videoUrl = VideoUtils.getVideoUrl(false)
        GeminiManager.getInstance().startTranscription(view.context, videoUrl)
    }
}
