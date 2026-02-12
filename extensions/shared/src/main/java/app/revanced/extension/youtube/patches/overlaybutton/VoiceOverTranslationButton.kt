package app.revanced.extension.youtube.patches.overlaybutton

import android.view.View
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.youtube.patches.voiceovertranslation.VoiceOverTranslationPatch
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.PlayerControlButton
import app.revanced.extension.youtube.shared.RootView.isAdProgressTextVisible

@Suppress("DEPRECATION", "unused")
object VoiceOverTranslationButton {
    private var instance: PlayerControlButton? = null

    /**
     * Injection point.
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_vot_button",
                buttonVisibility = { isButtonEnabled() },
                onClickListener = { view: View -> onClick(view) },
            )
        } catch (ex: Exception) {
            Logger.printException({ "VoiceOverTranslationButton initializeButton failure" }, ex)
        }
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setVisibilityNegatedImmediate() {
        instance?.setVisibilityNegatedImmediate()
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setVisibilityImmediate(visible: Boolean) {
        instance?.setActivated()
        instance?.setVisibilityImmediate(visible)
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setVisibility(visible: Boolean, animated: Boolean) {
        instance?.setActivated()
        instance?.setVisibility(visible, animated)
    }

    private fun isButtonEnabled(): Boolean {
        return Settings.VOT_ENABLED.get()
                && !isAdProgressTextVisible()
    }

    private fun onClick(view: View) {
        VoiceOverTranslationPatch.toggleTranslation()
        instance?.imageView()?.isActivated = VoiceOverTranslationPatch.isTranslationActive()
    }

    private fun PlayerControlButton.setActivated() {
        imageView()?.isActivated = VoiceOverTranslationPatch.isTranslationActive()
    }
}
