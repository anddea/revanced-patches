/*
 * Copyright (C) 2025-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Jav1x (https://github.com/Jav1x)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
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
        if (VoiceOverTranslationPatch.isTranslationRequestInProgress()) {
            showMenu(view)
            return
        }
        VoiceOverTranslationPatch.toggleTranslation()
        instance?.imageView()?.isActivated = VoiceOverTranslationPatch.isTranslationActive()
    }

    private fun onLongClick(view: View): Boolean {
        return showMenu(view)
    }

    private fun showMenu(view: View): Boolean {
        val context = RootView.getContext() ?: view.context
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
