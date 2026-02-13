package app.morphe.patches.youtube.video.playback

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.customspeed.customPlaybackSpeedPatch
import app.morphe.patches.shared.drc.drcAudioPatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.opus.baseOpusCodecsPatch
import app.morphe.patches.youtube.utils.auth.authHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.VIDEO_PATH
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.fix.shortsplayback.shortsPlaybackPatch
import app.morphe.patches.youtube.utils.flyoutmenu.flyoutMenuHookPatch
import app.morphe.patches.youtube.utils.patch.PatchList.VIDEO_PLAYBACK
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_20_14_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_30_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.qualityMenuViewInflateFingerprint
import app.morphe.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import app.morphe.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.hookBackgroundPlayVideoInformation
import app.morphe.patches.youtube.video.information.hookVideoInformation
import app.morphe.patches.youtube.video.information.speedSelectionInsertMethod
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import app.morphe.util.fingerprint.*
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.definingClassOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val PLAYBACK_SPEED_MENU_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlaybackSpeedMenuFilter;"
private const val VIDEO_QUALITY_MENU_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/VideoQualityMenuFilter;"
private const val EXTENSION_ADVANCED_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/AdvancedVideoQualityMenuPatch;"
private const val EXTENSION_VP9_CODEC_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VP9CodecPatch;"
private const val EXTENSION_CUSTOM_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/CustomPlaybackSpeedPatch;"
private const val EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/PlaybackSpeedPatch;"
private const val EXTENSION_SPOOF_DEVICE_DIMENSIONS_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/SpoofDeviceDimensionsPatch;"
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
            8.0f
        ),
        authHookPatch,
        baseOpusCodecsPatch(),
        drcAudioPatch { is_19_30_or_greater },
        flyoutMenuHookPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        disableHdrPatch,
        playerTypeHookPatch,
        recyclerViewTreeObserverPatch,
        shortsPlaybackPatch,
        videoIdPatch,
        videoInformationPatch,
        sharedResourceIdPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: VIDEO"
        )

        // region patch for custom playback speed

        recyclerViewTreeObserverHook("$EXTENSION_CUSTOM_PLAYBACK_SPEED_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")
        addLithoFilter(PLAYBACK_SPEED_MENU_FILTER_CLASS_DESCRIPTOR)

        // endregion

        // region patch for default playback speed

        val newMethod =
            playbackSpeedChangedFromRecyclerViewFingerprint.methodOrThrow(
                qualityChangedFromRecyclerViewFingerprint
            )

        arrayOf(
            newMethod,
            speedSelectionInsertMethod
        ).forEach {
            it.apply {
                val speedSelectionValueInstructionIndex =
                    indexOfFirstInstructionOrThrow(Opcode.IGET)
                val speedSelectionValueRegister =
                    getInstruction<TwoRegisterInstruction>(speedSelectionValueInstructionIndex).registerA

                addInstruction(
                    speedSelectionValueInstructionIndex + 1,
                    "invoke-static {v$speedSelectionValueRegister}, " +
                            "$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->userSelectedPlaybackSpeed(F)V"
                )
            }
        }

        if (is_20_14_or_greater) {
            pcmGetterMethodFingerprint.mutableClassOrThrow().let {
                val targetMethod =
                    it.methods.find { method -> method.returnType == "F" && method.parameters.isEmpty() }
                        ?: throw PatchException("Method returning playback speed not found in class $it.") as Throwable

                targetMethod.apply {
                    val insertIndex = implementation!!.instructions.lastIndex
                    val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                        invoke-static {v$insertRegister}, $EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeed(F)F
                        move-result v$insertRegister
                        """
                    )
                }
            }
        } else {
            loadVideoParamsFingerprint.matchOrThrow(loadVideoParamsParentFingerprint).let {
                it.method.apply {
                    val targetIndex = it.instructionMatches.last().index
                    val targetReference =
                        getInstruction<ReferenceInstruction>(targetIndex).reference as MethodReference

                    findMethodOrThrow(definingClass) {
                        name == targetReference.name
                    }.apply {
                        val insertIndex = implementation!!.instructions.lastIndex
                        val insertRegister =
                            getInstruction<OneRegisterInstruction>(insertIndex).registerA

                        addInstructions(
                            insertIndex, """
                            invoke-static {v$insertRegister}, $EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeed(F)F
                            move-result v$insertRegister
                            """
                        )
                    }
                }
            }
        }

        hookBackgroundPlayVideoInformation("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        hookVideoInformation("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        hookPlayerResponseVideoId("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Z)V")

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "VideoPlayback")

        // endregion

        // region patch for default video quality

        qualityChangedFromRecyclerViewFingerprint.matchOrThrow().let {
            it.method.apply {
                val instructions = implementation?.instructions ?: throw IllegalStateException("Method implementation not found")
                val newInstanceIndex = instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.NEW_INSTANCE &&
                            (instruction as? ReferenceInstruction)?.reference?.toString() == "Lcom/google/android/libraries/youtube/innertube/model/media/VideoQuality;"
                }
                if (newInstanceIndex == -1) throw IllegalStateException("VideoQuality new-instance not found")

                val igetIndex = instructions.subList(newInstanceIndex, instructions.size).indexOfFirst { instruction ->
                    instruction.opcode == Opcode.IGET &&
                            (instruction as? ReferenceInstruction)?.reference is FieldReference &&
                            (instruction.reference as FieldReference).type == "I"
                }.let { index -> if (index == -1) -1 else index + newInstanceIndex }
                if (igetIndex == -1) throw IllegalStateException("IGET instruction for integer field not found")

                val register = getInstruction<TwoRegisterInstruction>(igetIndex).registerA
                addInstruction(
                    igetIndex + 1,
                    "invoke-static { v$register }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQualityInNewFlyout(I)V"
                )
            }
        }

        videoQualityItemOnClickFingerprint.methodOrThrow(
            videoQualityItemOnClickParentFingerprint
        ).addInstruction(
            0,
            "invoke-static { p3 }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQualityInOldFlyout(I)V"
        )

        // endregion

        // region patch for show advanced video quality menu

        qualityMenuViewInflateFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstInstructionOrThrow(Opcode.CHECK_CAST)
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex + 1,
                "invoke-static { v$insertRegister }, " +
                        "$EXTENSION_ADVANCED_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->showAdvancedVideoQualityMenu(Landroid/widget/ListView;)V"
            )
        }

        qualityMenuViewInflateOnItemClickFingerprint
            .methodOrThrow(qualityMenuViewInflateFingerprint)
            .apply {
                val contextIndex = indexOfContextInstruction(this)
                val contextField =
                    getInstruction<ReferenceInstruction>(contextIndex).reference as FieldReference
                val castIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.CHECK_CAST &&
                            getReference<TypeReference>()?.type == contextField.definingClass
                }
                val castRegister = getInstruction<OneRegisterInstruction>(castIndex).registerA

                val insertIndex = indexOfFirstInstructionOrThrow(castIndex, Opcode.IGET_OBJECT)
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "dismiss"
                }

                addInstructionsWithLabels(
                    insertIndex, """
                        iget-object v$insertRegister, v$castRegister, $contextField
                        invoke-static {v$insertRegister}, $EXTENSION_ADVANCED_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->showAdvancedVideoQualityMenu(Landroid/content/Context;)Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :dismiss
                        """, ExternalLabel("dismiss", getInstruction(jumpIndex))
                )
            }


        recyclerViewTreeObserverHook("$EXTENSION_ADVANCED_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")
        addLithoFilter(VIDEO_QUALITY_MENU_FILTER_CLASS_DESCRIPTOR)

        // endregion

        // region patch for spoof device dimensions

        findMethodOrThrow(
            deviceDimensionsModelToStringFingerprint.definingClassOrThrow()
        ).addInstructions(
            1, // Add after super call.
            mapOf(
                1 to "MinHeightOrWidth", // p1 = min height
                2 to "MaxHeightOrWidth", // p2 = max height
                3 to "MinHeightOrWidth", // p3 = min width
                4 to "MaxHeightOrWidth"  // p4 = max width
            ).map { (parameter, method) ->
                """
                    invoke-static { p$parameter }, $EXTENSION_SPOOF_DEVICE_DIMENSIONS_CLASS_DESCRIPTOR->get$method(I)I
                    move-result p$parameter
                    """
            }.joinToString("\n") { it }
        )

        // endregion

        // region patch for disable VP9 codec

        vp9CapabilityFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_VP9_CODEC_CLASS_DESCRIPTOR->disableVP9Codec()Z
                    move-result v0
                    if-nez v0, :default
                    return v0
                    """, ExternalLabel("default", getInstruction(0))
            )
        }

        // endregion

        // region add settings

        addPreference(settingArray, VIDEO_PLAYBACK)

        // endregion
    }
}
