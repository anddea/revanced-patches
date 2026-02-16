/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of https://github.com/anddea/revanced-patches/.
 *
 * The original author: https://github.com/Jav1x.
 *
 * IMPORTANT: This file is the proprietary work of https://github.com/Jav1x.
 * Any modifications, derivatives, or substantial rewrites of this file
 * must retain this copyright notice and the original author attribution
 * in the source code and version control history.
 */

package app.morphe.extension.youtube.patches.overlaybutton

import android.view.View
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.youtube.patches.utils.PatchStatus
import app.morphe.extension.youtube.patches.voiceovertranslation.VoiceOverTranslationPatch
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlButton
import app.morphe.extension.youtube.shared.RootView
import app.morphe.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.morphe.extension.youtube.utils.VideoUtils

@Suppress("DEPRECATION", "unused")
object VoiceOverTranslationButton {
    private var instance: PlayerControlButton? = null

    /**
     * Injection point.
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            VoiceOverTranslationPatch.setOnTranslationStateChangeCallback { refreshActivatedState() }
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_vot_button",
                buttonVisibility = { isButtonEnabled() },
                onClickListener = { view: View -> onClick(view) },
                onLongClickListener = { view: View -> onLongClick(view) },
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
                && PatchStatus.VoiceOverTranslation()
    }

    private fun onClick(view: View) {
        VoiceOverTranslationPatch.toggleTranslation()
        instance?.imageView()?.isActivated = VoiceOverTranslationPatch.isTranslationActive()
    }

    private fun onLongClick(view: View): Boolean {
        val context = RootView.getContext() ?: return false
        VideoUtils.showVotBottomSheetDialog(context)
        return true
    }

    private fun refreshActivatedState() {
        instance?.setActivated()
    }

    private fun PlayerControlButton.setActivated() {
        imageView()?.isActivated = VoiceOverTranslationPatch.isTranslationActive()
    }
}
