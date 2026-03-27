package app.morphe.patches.music.video.playback

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
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
import app.morphe.patches.music.utils.settings.addPreferenceWithIntent
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
    compatibleWith(COMPATIBLE_PACKAGE)

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
                    index, """
                        invoke-static {v$register}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->getInitialVideoQuality(Lj${'$'}/util/Optional;)Lj${'$'}/util/Optional;
                        move-result-object v$register
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
        addPreferenceWithIntent(
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
            "revanced_remember_video_quality_last_selected"
        )

        updatePatchStatus(VIDEO_PLAYBACK)

    }
}
