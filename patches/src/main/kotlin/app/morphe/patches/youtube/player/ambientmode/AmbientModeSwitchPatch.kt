/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.ambientmode

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.patch.PatchList.AMBIENT_MODE_CONTROL
import app.morphe.patches.youtube.utils.playservice.is_19_34_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_41_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findMethodOrThrow
import app.morphe.util.insertLiteralOverride
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val ambientModeSwitchPatch = bytecodePatch(
    AMBIENT_MODE_CONTROL.title,
    AMBIENT_MODE_CONTROL.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        // region patch for bypass ambient mode restrictions

        var syntheticClassList = emptyArray<String>()

        mapOf(
            powerSaveModeBroadcastReceiverFingerprint to false,
            powerSaveModeSyntheticFingerprint to true
        ).forEach { (fingerprint, reversed) ->
            fingerprint.method.apply {
                val stringIndex =
                    indexOfFirstStringInstructionOrThrow("android.os.action.POWER_SAVE_MODE_CHANGED")
                val targetIndex =
                    if (reversed)
                        indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.INVOKE_DIRECT)
                    else
                        indexOfFirstInstructionOrThrow(stringIndex, Opcode.INVOKE_DIRECT)
                val targetClass =
                    (getInstruction<ReferenceInstruction>(targetIndex).reference as MethodReference).definingClass

                syntheticClassList += targetClass
            }
        }

        syntheticClassList.distinct().forEach { className ->
            findMethodOrThrow(className) {
                name == "accept"
            }.apply {
                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        val reference = (instruction as? ReferenceInstruction)?.reference
                        instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                                reference is MethodReference &&
                                reference.name == "isPowerSaveMode"
                    }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                        addInstructions(
                            index + 2, """
                                invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->bypassAmbientModeRestrictions(Z)Z
                                move-result v$register
                                """
                        )
                    }
            }
        }

        // endregion

        //
        // Disable ambient mode.
        //
        AmbientModeFeatureFlagFingerprint.matchAll().forEach {
            // 21.30+ inlines the flag lookup and must patch ~5 places.
            it.method.insertLiteralOverride(
                it.instructionMatches.first().index,
                "$PLAYER_CLASS_DESCRIPTOR->disableAmbientMode(Z)Z"
            )
        }

        // region patch for disable ambient mode in fullscreen

        if (!is_19_41_or_greater) {
            ambientModeInFullscreenFingerprint.method.apply {
                val literalIndex = ambientModeInFullscreenFingerprint.instructionMatches.first().index
                val index = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableAmbientModeInFullscreen()Z
                        move-result v$register
                    """
                )
            }
        }

        if (is_19_34_or_greater) {
            setFullScreenBackgroundColorFingerprint.method.apply {
                val insertIndex = indexOfFirstInstructionReversedOrThrow {
                    getReference<MethodReference>()?.name == "setBackgroundColor"
                }
                val register = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstructions(
                    insertIndex,
                    """
                        invoke-static { v$register }, $PLAYER_CLASS_DESCRIPTOR->getFullScreenBackgroundColor(I)I
                        move-result v$register
                        """,
                )
            }
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: AMBIENT_MODE_CONTROLS"
            ),
            AMBIENT_MODE_CONTROL
        )

        // endregion

    }
}
