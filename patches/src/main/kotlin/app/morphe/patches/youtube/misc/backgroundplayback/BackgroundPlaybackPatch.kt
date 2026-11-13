/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 *
 * Modified by lavinhoque33, 2026-10-21.
 */

package app.morphe.patches.youtube.misc.backgroundplayback

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

import app.morphe.util.matchAllMethodIndicesForEach
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.mapping.resourceMappingPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.MISC_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_19_34_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_51_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_49_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_15_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_30_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.onCreateHook
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getWalkerMethod
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/BackgroundPlaybackPatch;"

private const val EXTENSION_BACKGROUND_PLAYBACK_SETTING_INTERFACE =
    $$"Lapp/morphe/extension/youtube/patches/misc/BackgroundPlaybackPatch$BackgroundPlaybackSetting;"

@Suppress("unused", "DEPRECATION")
val backgroundPlaybackPatch = bytecodePatch(
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.title,
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        playerTypeHookPatch,
        videoInformationPatch,
        settingsPatch,
        resourceMappingPatch,
        versionCheckPatch,
    )

    execute {

        arrayOf(
            BackgroundPlaybackManagerFingerprint to "isBackgroundPlaybackAllowed",
            BackgroundPlaybackManagerShortsFingerprint to "isBackgroundShortsPlaybackAllowed",
        ).forEach { (fingerprint, extensionsMethod) ->
            fingerprint.matchSingle().method.apply {
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
        BackgroundPlaybackSettingsFingerprint.originalMethod.apply {
            val booleanCalls = instructions.withIndex().filter {
                it.value.getReference<MethodReference>()?.returnType == "Z"
            }

            val settingsBooleanIndex = booleanCalls.elementAt(1).index
            val settingsBooleanMethod = BackgroundPlaybackSettingsFingerprint.method.getWalkerMethod(settingsBooleanIndex)

            settingsBooleanMethod.returnEarly(true)
        }

        // Force allowing background play for Shorts.
        ShortsBackgroundPlaybackFeatureFlagFingerprint.matchSingle().method.insertLiteralOverride(
            SHORTS_BACKGROUND_PLAYBACK_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->isBackgroundShortsPlaybackAllowed(Z)Z"
        )

        // Fix PiP mode issue.
        if (is_19_34_or_greater) {
            // 21.39 inlines the flag getter into the two fragment lifecycle methods.
            // Force the flag to false in each of them; otherwise use the legacy fingerprints.
            val inlinedFlagMatches = runCatching {
                BackgroundPlaybackManagerCairoFragmentInlinedFlagFingerprint.matchAll(
                    BackgroundPlaybackManagerCairoFragmentParentFingerprint.originalClassDef
                )
            }.getOrNull().orEmpty()

            if (inlinedFlagMatches.isNotEmpty()) {
                inlinedFlagMatches.forEach {
                    it.method.insertLiteralOverride(
                        it.instructionMatches.last().index,
                        false
                    )
                }
            } else {
                arrayOf(
                    BackgroundPlaybackManagerCairoFragmentPrimaryFingerprint,
                    BackgroundPlaybackManagerCairoFragmentSecondaryFingerprint,
                ).forEach { fingerprint ->
                    fingerprint.match(BackgroundPlaybackManagerCairoFragmentParentFingerprint.originalClassDef)
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
            }

            // 21.39: pip_input_consumer feature flag was removed; skip when absent.
            // There is nothing to override as the flag no longer exists there.
            runCatching {
                PipInputConsumerFeatureFlagFingerprint.matchSingle().method
            }.getOrNull()?.insertLiteralOverride(
                PIP_INPUT_CONSUMER_FEATURE_FLAG,
                false,
            )
        }

        // Prevents playback from resuming if it was interrupted from the notification
        // and the app was subsequently brought to the foreground.
        if (is_21_15_or_greater) {
            // The same flag resumes the video when a Short opened over it is closed, so it is only
            // overridden where the main activity goes to the background and returns to the foreground
            // (https://github.com/MorpheApp/morphe-patches/issues/2506).
            //
            // If the background playback setting of YouTube does not allow background playback,
            // the app pauses the video when it goes to the background, and the flag keeps the value
            // of the account so the video resumes when the app returns to the foreground
            // (https://github.com/MorpheApp/morphe-patches/issues/4639).
            // The extension reads the setting with a method added to the setting class.
            backgroundPlaybackSettingConstructorFingerprint(
                settingClassType = backgroundPlaybackSettingFingerprint().originalClassDef.type,
                settingStoreType = backgroundPlaybackSettingFingerprint().instructionMatches.first()
                    .methodReference.definingClass,
                audioManagerType = BackgroundPlaybackSettingAllowedFingerprint.originalMethod
                    .parameterTypes[1].toString()
            ).let {
                // After the super constructor call, before the register of 'this' can be reused.
                it.method.addInstruction(
                    it.instructionMatches.first().index + 1,
                    "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                            "setBackgroundPlaybackSetting($EXTENSION_BACKGROUND_PLAYBACK_SETTING_INTERFACE)V"
                )
            }

            backgroundPlaybackSettingFingerprint().classDef.apply {
                interfaces.add(EXTENSION_BACKGROUND_PLAYBACK_SETTING_INTERFACE)
                methods.add(
                    ImmutableMethod(
                        type,
                        "patch_isBackgroundPlaybackAllowed",
                        listOf(),
                        "Z",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                invoke-virtual { p0 }, ${backgroundPlaybackSettingFingerprint().method}
                                move-result v0
                                return v0
                            """
                        )
                    }
                )
            }

            val override = "$EXTENSION_CLASS_DESCRIPTOR->isAutomaticForegroundPlaybackAllowed(Z)Z"
            val backgroundedClasses = PlayingWhenBackgroundedFingerprint.matchAll()
                .map { it.classDef }
                .distinctBy { it.type }

            if (is_21_30_or_greater) {
                backgroundedClasses.forEach { classDef ->
                    AutomaticForegroundPlaybackResumeFeatureFlagFingerprint.matchAll(classDef).forEach { match ->
                        match.method.insertLiteralOverride(match.instructionMatches.first().index, override)
                    }
                }
            } else {
                // 21.29 and older read the flag with a single accessor method.
                backgroundedClasses.forEach { classDef ->
                    automaticForegroundPlaybackResumeFlagAccessorCallFingerprint(classDef.type)
                        .matchAllMethodIndicesForEach { index ->
                            val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                            addInstructions(
                                index + 2,
                                """
                                    invoke-static { v$register }, $override
                                    move-result v$register
                                """
                            )
                        }
                }
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

        // The playback media session publishes its state, and the playback notification is shown, once the state
        // has its artwork, or after a short wait without other changes, which a playing video can keep from ending.
        // Without a thumbnail, such as when it fails to load, the missing artwork is not a change of the state,
        // so the notification can be missing and the service is not kept in the foreground, which stops the audio
        // in the background after a while. The artwork is now always a change of the state, also when it is missing.
        if (is_20_51_or_greater) {
            MediaSessionArtworkStateFingerprint.let {
                it.method.apply {
                    val noChangeIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_4)
                    val register = getInstruction<OneRegisterInstruction>(noChangeIndex).registerA
                    val change = it.instructionMatches.last().getInstruction<NarrowLiteralInstruction>().narrowLiteral
                    replaceInstruction(noChangeIndex, "const v$register, $change")
                }
            }
        }


        // Force allowing background play for videos labeled for kids.
        KidsBackgroundPlaybackPolicyControllerFingerprint.method.returnEarly()

        onCreateHook(EXTENSION_CLASS_DESCRIPTOR, "initialize")

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SHORTS",
                "SETTINGS: DISABLE_SHORTS_BACKGROUND_PLAYBACK",
                "SETTINGS: AUTO_PAUSE_ON_SCREEN_LOCK"
            ),
            REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
        )

        // endregion

    }
}
