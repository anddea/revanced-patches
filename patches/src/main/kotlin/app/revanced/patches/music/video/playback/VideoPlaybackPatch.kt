package app.revanced.patches.music.video.playback

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.VIDEO_PATH
import app.revanced.patches.music.utils.patch.PatchList.VIDEO_PLAYBACK
import app.revanced.patches.music.utils.playbackSpeedBottomSheetFingerprint
import app.revanced.patches.music.utils.playbackSpeedFingerprint
import app.revanced.patches.music.utils.playbackSpeedParentFingerprint
import app.revanced.patches.music.utils.playservice.is_8_20_or_greater
import app.revanced.patches.music.utils.resourceid.qualityAuto
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.music.video.information.onCreateHook
import app.revanced.patches.music.video.information.videoInformationPatch
import app.revanced.patches.shared.customspeed.customPlaybackSpeedPatch
import app.revanced.util.findFreeRegister
import app.revanced.util.findMethodOrThrow
import app.revanced.util.findMutableClassOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.parametersEqual
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
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
        customPlaybackSpeedPatch(
            "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
            5.0f
        ),
        settingsPatch,
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
                val startIndex = it.patternMatch!!.startIndex
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
            with (it.method) {
                // set video quality array
                val listIndex = it.patternMatch!!.startIndex
                val listRegister = getInstruction<FiveRegisterInstruction>(listIndex).registerD

                addInstruction(
                    listIndex,
                    "invoke-static {v$listRegister}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQualities([Ljava/lang/Object;)V"
                )

                val literalIndex = indexOfFirstLiteralInstructionOrThrow(qualityAuto)
                val qualityClassIndex = indexOfFirstInstructionReversedOrThrow(literalIndex + 1, Opcode.NEW_INSTANCE)
                getInstruction<ReferenceInstruction>(qualityClassIndex).reference.toString()
            }
        }

        val constructorParams = if (is_8_20_or_greater) {
            listOf("I", "L", "Ljava/lang/String;", "Z", "L")
        } else {
            listOf("I", "Ljava/lang/String;", "Z", "L")
        }

        val invokeParams = if (is_8_20_or_greater) {
            "p1, p3"
        } else {
            "p1, p2"
        }

        findMutableClassOrThrow(videoQualityClass).apply {
            methods.first { method ->
                MethodUtil.isConstructor(method) &&
                        parametersEqual(
                            constructorParams,
                            method.parameterTypes
                        )
            }.addInstructions(
                0, """
                    invoke-static { $invokeParams }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->fixVideoQualityResolution(ILjava/lang/String;)I
                    move-result p1
                    """
            )

            val resolutionField = fields.single { field ->
                field.type == "I"
            }

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

        val (formatStreamModelClass, formatStreamResolutionReference) =
            availableVideoFormatsFingerprint.matchOrThrow(
                formatStreamModelBuilderFingerprint
            ).let {
                with (it.method) {
                    val formatStreamIndex = it.patternMatch!!.startIndex + 1
                    val formatStreamResolutionReference =
                        getInstruction<ReferenceInstruction>(formatStreamIndex).reference as MethodReference

                    addInstructions(
                        0,
                        "invoke-static { p0 }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoFormat(Ljava/util/List;)V"
                    )

                    val formatStreamModelClass = formatStreamResolutionReference.definingClass

                    Pair(formatStreamModelClass, formatStreamResolutionReference)
                }
            }

        findMethodOrThrow(EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR) {
            name == "getFormatStreamResolution"
        }.addInstructions(
            0, """
                    check-cast p0, $formatStreamModelClass
                    invoke-virtual { p0 }, $formatStreamResolutionReference
                    move-result p0
                    return p0
                    """
        )

        initFormatStreamFingerprint.methodOrThrow(initFormatStreamParentFingerprint)
            .apply {
                val preferredFormatStreamIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == formatStreamModelClass
                }
                val preferredFormatStreamReference =
                    getInstruction<ReferenceInstruction>(preferredFormatStreamIndex).reference
                val preferredFormatStreamInstruction =
                    getInstruction<TwoRegisterInstruction>(preferredFormatStreamIndex)
                val preferredFormatStreamRegister =
                    preferredFormatStreamInstruction.registerA
                val definingClassRegister =
                    preferredFormatStreamInstruction.registerB
                val freeRegister =
                    findFreeRegister(preferredFormatStreamIndex, false, preferredFormatStreamRegister, definingClassRegister)

                addInstructionsWithLabels(
                    preferredFormatStreamIndex + 1, """
                        invoke-static { v$preferredFormatStreamRegister }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->getVideoFormat(Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$preferredFormatStreamRegister
                        instance-of v$freeRegister, v$preferredFormatStreamRegister, $formatStreamModelClass
                        if-eqz v$freeRegister, :ignore
                        check-cast v$preferredFormatStreamRegister, $formatStreamModelClass
                        iput-object v$preferredFormatStreamRegister, v$definingClassRegister, $preferredFormatStreamReference
                        :ignore
                        iget-object v$preferredFormatStreamRegister, v$definingClassRegister, $preferredFormatStreamReference
                        """
                )
            }

        onCreateHook(EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR, "newVideoStarted")

        userQualityChangeFingerprint.matchOrThrow().let {
            it.method.apply {
                val endIndex = it.patternMatch!!.endIndex
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

        addPreferenceWithIntent(
            CategoryType.VIDEO,
            "revanced_custom_playback_speeds"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "revanced_remember_playback_speed_last_selected",
            "true"
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
