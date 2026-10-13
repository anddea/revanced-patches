/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.playertype

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.literal
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playservice.is_20_29_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_04_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_36_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.findInstructionIndicesReversed
import app.morphe.util.getReference
import app.morphe.util.insertLiteralOverride
import app.morphe.util.forEachInlinedFeatureFlagSite
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerControlsPatch;"

private object NewPlayerTypeEnumFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45698813)
    )
)

private object NewPlayerOverlaysFeatureFlagFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45752335L)
    )
)

// Ported from Morphe c3431a3 (#3649).
private object AutonavEndscreenOverlayFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("player_overlay_player_autonav_endscreen")
)

internal val legacyPlayerStatePatch = bytecodePatch(
    description = "Keep legacy player state callbacks available."
) {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    execute {
        if (is_21_36_or_greater) {
            NewPlayerTypeEnumFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->forcePlayerSeekbar(Z)Z",
                )
            }
        } else if (is_20_29_or_greater) {
            // Client flag that interferes with background playback of some video types.
            // Exact purpose is not clear and it's used in ~ 100 locations.
            // Flag cannot be forced off with 21.36+ or the player seekbar is missing.
            //
            // Edit: This override may not be needed and only 45752335L override might be needed.
            NewPlayerTypeEnumFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(it.instructionMatches.first().index, false)
            }
        }

        // If NewPlayerTypeEnumFeatureFlagFingerprint is overridden then must also
        // force off new player control flags otherwise player has no buttons visible.
        if (is_21_04_or_greater) {
            // Keep the real value for the autonav end screen.
            val flagMethods = NewPlayerOverlaysFeatureFlagFingerprint.matchAll().map {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->disableNewPlayerOverlays(Z)Z"
                )
                it.method
            }

            if (is_21_36_or_greater) {
                // With the flag off, the autonav end screen is laid out over the miniplayer
                // as if it was the full player, so it is given the value the app has.
                var appliedChanges = false
                AutonavEndscreenOverlayFingerprint.classDef.methods.forEach { method ->
                    method.findInstructionIndicesReversed {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_VIRTUAL && reference != null && flagMethods.any {
                            reference.definingClass == it.definingClass &&
                                    reference.name == it.name &&
                                    reference.returnType == "Z" &&
                                    reference.parameterTypes.isEmpty()
                        }
                    }.forEach { index ->
                        val register =
                            method.getInstruction<OneRegisterInstruction>(index + 1).registerA
                        method.addInstructions(
                            index + 2,
                            """
                                invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->getOriginalNewPlayerOverlays(Z)Z
                                move-result v$register
                            """
                        )
                        appliedChanges = true
                    }
                }
                if (!appliedChanges) {
                    throw PatchException("Did not apply autonav end screen changes")
                }
            }

            // YouTube 21.39 also inlines the flag into a method that is not a flag getter.
            // Skip the getters hooked above. A constant override there would land before
            // disableNewPlayerOverlays and hide the real value.
            forEachInlinedFeatureFlagSite(45752335L) { literalIndex ->
                val isHookedGetter = flagMethods.any {
                    it.definingClass == definingClass && MethodUtil.methodSignaturesMatch(it, this)
                }
                if (!isHookedGetter) {
                    insertLiteralOverride(literalIndex, false)
                }
            }
        }
    }
}
