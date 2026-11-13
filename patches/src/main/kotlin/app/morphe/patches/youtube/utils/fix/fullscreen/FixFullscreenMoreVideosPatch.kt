/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.fix.fullscreen

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/** Keeps the fullscreen related-videos button working regardless of the panel's current state. */
internal val fixFullscreenMoreVideosPatch = bytecodePatch(
    description = "Makes the fullscreen More videos button always open the related videos."
) {
    execute {
        // The "More videos" button of the fullscreen player opens the related videos only if their panel peeks.
        // The panel can be left in another state, such as hidden while the time bar is scrubbed,
        // and the button then does nothing until the device is rotated. The panel is opened from any state.
        val match = FullscreenMoreVideosButtonFingerprint.matchOrNull()
        if (match == null) {
            // Verify the old unconditional action rather than skipping an unknown implementation.
            FullscreenMoreVideosUnconditionalFingerprint.method
            return@execute
        }
        match.let {
            it.method.apply {
                val index = it.instructionMatches[2].index
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                replaceInstruction(index, "const/4 v$register, 0x1")
            }
        }
    }
}
