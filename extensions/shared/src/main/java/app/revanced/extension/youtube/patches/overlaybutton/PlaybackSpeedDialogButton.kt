package app.revanced.extension.youtube.patches.overlaybutton

import android.view.View
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.StringRef
import app.revanced.extension.shared.utils.Utils
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.PlayerControlButton
import app.revanced.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.revanced.extension.youtube.shared.VideoInformation
import app.revanced.extension.youtube.utils.VideoUtils

@Suppress("unused")
object PlaybackSpeedDialogButton {
    private var instance: PlayerControlButton? = null

    /**
     * Injection point.
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_playback_speed_dialog_button",
                buttonVisibility = { isButtonEnabled() },
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

    private fun isButtonEnabled(): Boolean {
        return Settings.OVERLAY_BUTTON_SPEED_DIALOG.get()
                && !isAdProgressTextVisible()
    }

    private fun onClick(view: View) {
        VideoUtils.showPlaybackSpeedDialog(view.context, Settings.OVERLAY_BUTTON_SPEED_DIALOG_TYPE)
    }

    private fun onLongClick(view: View) {
        if (!Settings.REMEMBER_PLAYBACK_SPEED_LAST_SELECTED.get() ||
            VideoInformation.getPlaybackSpeed() == Settings.DEFAULT_PLAYBACK_SPEED.get()
        ) {
            VideoInformation.overridePlaybackSpeed(1.0f)
            Utils.showToastShort(
                StringRef.str(
                    "revanced_overlay_button_speed_dialog_reset",
                    "1.0"
                )
            )
        } else {
            val defaultSpeed = Settings.DEFAULT_PLAYBACK_SPEED.get()
            VideoInformation.overridePlaybackSpeed(defaultSpeed)
            Utils.showToastShort(
                StringRef.str(
                    "revanced_overlay_button_speed_dialog_reset",
                    defaultSpeed
                )
            )
        }
    }
}