/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2582
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.scrolling

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_SCROLLING_SPEED_LIMIT
import app.morphe.patches.youtube.utils.playservice.is_20_35_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/DisableScrollSpeedLimitPatch;"

@Suppress("unused")
val disableScrollSpeedLimitPatch = bytecodePatch(
    DISABLE_SCROLLING_SPEED_LIMIT.title,
    DISABLE_SCROLLING_SPEED_LIMIT.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        if (!is_20_35_or_greater) {
            return@execute
        }

        addPreference(
            arrayOf(
                "SETTINGS: DISABLE_SCROLLING_SPEED_LIMIT"
            ),
            DISABLE_SCROLLING_SPEED_LIMIT
        )

        SnappyRecyclerViewSetFlingLimitFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.last().index
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructionsAtControlFlowLabel(
                    index,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS->disableSpeedScrolling(Z)Z
                        move-result v$register
                    """
                )
            }
        }
    }
}
