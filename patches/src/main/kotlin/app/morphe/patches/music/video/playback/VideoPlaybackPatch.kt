/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
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

package app.morphe.patches.music.video.playback

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.VIDEO_PATH
import app.morphe.patches.music.utils.patch.PatchList.VIDEO_PLAYBACK
import app.morphe.patches.music.utils.playbackSpeedBottomSheetFingerprint
import app.morphe.patches.music.utils.playbackSpeedFingerprint
import app.morphe.patches.music.utils.playbackSpeedParentFingerprint
import app.morphe.patches.music.utils.playservice.is_7_13_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.resourceid.qualityAuto
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addTextPreference
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.music.video.information.onCreateHook
import app.morphe.patches.music.video.information.videoInformationPatch
import app.morphe.patches.shared.FIXED_RESOLUTION_STRING
import app.morphe.patches.shared.customspeed.customPlaybackSpeedPatch
import app.morphe.patches.shared.drc.drcAudioPatch
import app.morphe.patches.shared.opus.baseOpusCodecsPatch
import app.morphe.patches.shared.playbackStartParametersConstructorFingerprint
import app.morphe.patches.shared.playbackStartParametersToStringFingerprint
import app.morphe.util.findFieldFromToString
import app.morphe.util.findMethodOrThrow
import app.morphe.util.findMutableClassOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.fingerprint.originalMethodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/PlaybackSpeedPatch;"
private const val EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VideoQualityPatch;"

@Suppress("unused")
val videoPlaybackPatch = bytecodePatch(
    VIDEO_PLAYBACK.title,
    VIDEO_PLAYBACK.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
        customPlaybackSpeedPatch(
            "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
            5.0f
        ),
        baseOpusCodecsPatch(),
        drcAudioPatch { is_7_13_or_greater },
        sharedResourceIdPatch,
        videoInformationPatch,
    )

    execute {
        // region patch for default playback speed

        playbackSpeedBottomSheetFingerprint.mutableClassOrThrow().let {
            val onItemClickMethod =
                it.methods.find { method -> method.name == "onItemClick" }
                    ?: throw PatchException("Failed to find onItemClick method")

            onItemClickMethod.apply {
                val targetIndex = indexOfFirstInstructionOrThrow(Opcode.IGET)
                val targetRegister =
                    getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->userSelectedPlaybackSpeed(F)V"
                )
            }
        }

        playbackSpeedFingerprint.matchOrThrow(playbackSpeedParentFingerprint).let {
            it.method.apply {
                val startIndex = it.instructionMatches.first().index
                val speedRegister =
                    getInstruction<OneRegisterInstruction>(startIndex + 1).registerA

                addInstructions(
                    startIndex + 2, """
                        invoke-static {v$speedRegister}, $EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeed(F)F
                        move-result v$speedRegister
                        """
                )
            }
        }

        // endregion

        // region patch for default video quality

        val videoQualityClass = videoQualityListFingerprint.matchOrThrow().let {
            with(it.method) {
                // set video quality array
                val listIndex = it.instructionMatches.first().index
                val listRegister = getInstruction<FiveRegisterInstruction>(listIndex).registerD

                addInstruction(
                    listIndex,
                    "invoke-static {v$listRegister}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQualities([Ljava/lang/Object;)V"
                )

                val literalIndex = indexOfFirstLiteralInstructionOrThrow(qualityAuto)
                val qualityClassIndex =
                    indexOfFirstInstructionReversedOrThrow(literalIndex + 1, Opcode.NEW_INSTANCE)
                getInstruction<ReferenceInstruction>(qualityClassIndex).reference.toString()
            }
        }

        fun indexOfVideoQualityNameFieldInstruction(method: Method) =
            method.indexOfFirstInstruction {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IPUT_OBJECT &&
                        reference?.type == "Ljava/lang/String;" &&
                        reference.definingClass == method.definingClass
            }

        fun indexOfVideoQualityResolutionFieldInstruction(method: Method) =
            method.indexOfFirstInstruction {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IPUT &&
                        reference?.type == "I" &&
                        reference.definingClass == method.definingClass
            }

        findMutableClassOrThrow(videoQualityClass).let {
            it.methods.first { method ->
                MethodUtil.isConstructor(method) &&
                        method.parameterTypes.size > 3 &&
                        indexOfVideoQualityNameFieldInstruction(method) >= 0 &&
                        indexOfVideoQualityResolutionFieldInstruction(method) >= 0
            }.apply {
                val qualityNameIndex = indexOfVideoQualityNameFieldInstruction(this)
                val resolutionIndex = indexOfVideoQualityResolutionFieldInstruction(this)
                val resolutionField =
                    getInstruction<ReferenceInstruction>(resolutionIndex).reference
                val qualityNameRegister =
                    getInstruction<TwoRegisterInstruction>(qualityNameIndex).registerA
                val resolutionRegister =
                    getInstruction<TwoRegisterInstruction>(resolutionIndex).registerA

                addInstructions(
                    0, """
                        invoke-static { v$resolutionRegister, v$qualityNameRegister }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->fixVideoQualityResolution(ILjava/lang/String;)I
                        move-result v$resolutionRegister
                        """
                )

                findMethodOrThrow(EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR) {
                    name == "getVideoQualityResolution"
                }.addInstructions(
                    0, """
                        check-cast p0, $videoQualityClass
                        iget p0, p0, $resolutionField
                        return p0
                        """
                )
            }
        }

        val initialResolutionField =
            playbackStartParametersToStringFingerprint.originalMethodOrThrow()
                .findFieldFromToString(FIXED_RESOLUTION_STRING)

        playbackStartParametersConstructorFingerprint
            .methodOrThrow(playbackStartParametersToStringFingerprint)
            .apply {
                val index = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>() == initialResolutionField
                }
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructions(
                    index, $$"""
                        invoke-static {v$$register}, $$EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->getInitialVideoQuality(Lj$/util/Optional;)Lj$/util/Optional;
                        move-result-object v$$register
                        """
                )
            }

        onCreateHook(EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR, "newVideoStarted")

        userQualityChangeFingerprint.matchOrThrow().let {
            it.method.apply {
                val endIndex = it.instructionMatches.last().index
                val qualityChangedClass =
                    getInstruction<ReferenceInstruction>(endIndex).reference.toString()

                findMethodOrThrow(qualityChangedClass) {
                    name == "onItemClick"
                }.addInstruction(
                    0,
                    "invoke-static { p3 }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->userSelectedVideoQuality(I)V"
                )
            }
        }

        // endregion

        addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_disable_drc_audio",
            "false"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_enable_opus_codec",
            "true"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_remember_playback_speed_last_selected",
            "true"
        )
        addTextPreference(
            CategoryType.VIDEO,
            "revanced_custom_playback_speeds"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_remember_playback_speed_last_selected_toast",
            "true",
            "revanced_remember_playback_speed_last_selected"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_remember_video_quality_last_selected",
            "true"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_remember_video_quality_last_selected_toast",
            "true",
            "revanced_remember_video_quality_last_selected",
            true,
            titleKey = "revanced_remember_playback_speed_last_selected_toast_title",
        )

        updatePatchStatus(VIDEO_PLAYBACK)

    }
}
