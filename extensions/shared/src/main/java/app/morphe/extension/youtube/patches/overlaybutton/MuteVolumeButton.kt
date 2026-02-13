package app.morphe.extension.youtube.patches.overlaybutton

import android.content.Context
import android.media.AudioManager
import android.view.View
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlButton
import app.morphe.extension.youtube.shared.RootView.isAdProgressTextVisible

@Suppress("DEPRECATION", "unused")
object MuteVolumeButton {
    private var instance: PlayerControlButton? = null
    private var audioManager: AudioManager? = null
    private var stream: Int = AudioManager.STREAM_MUSIC

    /**
     * Injection point.
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_mute_volume_button",
                buttonVisibility = { isButtonEnabled() },
                onClickListener = { view: View -> onClick(view) },
            )
            audioManager =
                controlsView.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
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
        instance?.setActivated()
        instance?.setVisibilityImmediate(visible)
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibility(visible: Boolean, animated: Boolean) {
        instance?.setActivated()
        instance?.setVisibility(visible, animated)
    }

    private fun isButtonEnabled(): Boolean {
        return Settings.OVERLAY_BUTTON_MUTE_VOLUME.get()
                && !isAdProgressTextVisible()
    }

    private fun onClick(view: View) {
        if (instance != null && audioManager != null) {
            val unMuted = !audioManager!!.isStreamMute(stream)
            audioManager?.setStreamMute(stream, unMuted)
            instance?.imageView()?.isActivated = unMuted
        }
    }

    private fun PlayerControlButton.setActivated() {
        if (audioManager != null) {
            val muted = audioManager!!.isStreamMute(stream)
            imageView()?.isActivated = muted
        }
    }
}