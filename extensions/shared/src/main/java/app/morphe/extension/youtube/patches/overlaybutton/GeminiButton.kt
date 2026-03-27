/*
 * Copyright (C) 2025 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Attribution Notice
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Attribution (Section 7(b)): This specific copyright notice and the
 *    list of original authors above must be preserved in any copy or
 *    derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin (Section 7(c)): Modified versions must be clearly marked as
 *    such (e.g., by adding a "Modified by" line or a new copyright notice).
 *    They must not be misrepresented as the original work.
 *
 * ------------------------------------------------------------------------
 * Version Control Acknowledgement (Non-binding Request)
 * ------------------------------------------------------------------------
 *
 * While not a legal requirement of the GPLv3, the original author(s)
 * respectfully request that ports or substantial modifications retain
 * historical authorship credit in version control systems (e.g., Git),
 * listing original author(s) appropriately and modifiers as committers
 * or co-authors.
 */

package app.morphe.extension.youtube.patches.overlaybutton

import android.view.View
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlButton
import app.morphe.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.morphe.extension.youtube.utils.GeminiManager
import app.morphe.extension.youtube.utils.VideoUtils

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
        return Settings.OVERLAY_BUTTON_GEMINI.get()
                && !isAdProgressTextVisible()
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
