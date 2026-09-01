/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.misc.backgroundplayback

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.MISC_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_19_34_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_49_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_15_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.originalMethodOrThrow
import app.morphe.util.getReference
import app.morphe.util.insertLiteralOverride
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/BackgroundPlaybackPatch;"

@Suppress("unused")
val backgroundPlaybackPatch = bytecodePatch(
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.title,
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        playerTypeHookPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        arrayOf(
            backgroundPlaybackManagerFingerprint to "isBackgroundPlaybackAllowed",
            backgroundPlaybackManagerShortsFingerprint to "isBackgroundShortsPlaybackAllowed",
        ).forEach { (fingerprint, extensionsMethod) ->
            fingerprint.methodOrThrow().apply {
                findInstructionIndicesReversedOrThrow(Opcode.RETURN).forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructionsAtControlFlowLabel(
                        index,
                        """
                            invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->$extensionsMethod(Z)Z
                            move-result v$register 
                        """,
                    )
                }
            }
        }

        // Enable background playback option in YouTube settings
        backgroundPlaybackSettingsFingerprint.originalMethodOrThrow().apply {
            val booleanCalls = instructions.withIndex().filter {
                it.value.getReference<MethodReference>()?.returnType == "Z"
            }

            val settingsBooleanIndex = booleanCalls.elementAt(1).index
            val settingsBooleanMethod by navigate(this).to(settingsBooleanIndex)

            settingsBooleanMethod.returnEarly(true)
        }

        // Force allowing background play for Shorts.
        shortsBackgroundPlaybackFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
            SHORTS_BACKGROUND_PLAYBACK_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->isBackgroundShortsPlaybackAllowed(Z)Z"
        )

        // Fix PiP mode issue.
        if (is_19_34_or_greater) {
            arrayOf(
                backgroundPlaybackManagerCairoFragmentPrimaryFingerprint,
                backgroundPlaybackManagerCairoFragmentSecondaryFingerprint
            ).forEach { fingerprint ->
                fingerprint.matchOrThrow(backgroundPlaybackManagerCairoFragmentParentFingerprint)
                    .let {
                        it.method.apply {
                            val insertIndex = it.instructionMatches.first().index + 4
                            val insertRegister =
                                getInstruction<OneRegisterInstruction>(insertIndex).registerA

                            addInstruction(
                                insertIndex,
                                "const/4 v$insertRegister, 0x0"
                            )
                        }
                    }
            }

            pipInputConsumerFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                PIP_INPUT_CONSUMER_FEATURE_FLAG,
                "0x0"
            )
        }

        // Prevents playback from resuming if it was interrupted from the notification
        // and the app was subsequently brought to the foreground.
        if (is_21_15_or_greater) {
            AutomaticForegroundPlaybackResumeFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->isAutomaticForegroundPlaybackAllowed(Z)Z"
                )
            }
        }

        // Prevents playback from pausing when the overlay video settings is invoked.
        if (is_20_49_or_greater) {
            AutomaticPlaybackPausedInFlyoutFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->isAutomaticPlaybackPauseInFlyout(Z)Z"
                )
            }
        }

        // Force allowing background play for videos labeled for kids.
        KidsBackgroundPlaybackPolicyControllerFingerprint.method.returnEarly()

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SHORTS",
                "SETTINGS: DISABLE_SHORTS_BACKGROUND_PLAYBACK"
            ),
            REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
        )

        // endregion

    }
}
