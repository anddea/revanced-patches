package app.revanced.patches.youtube.video.playback

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.customspeed.customPlaybackSpeedPatch
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.dismiss.dismissPlayerHookPatch
import app.revanced.patches.youtube.utils.dismiss.hookDismissObserver
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.revanced.patches.youtube.utils.fix.shortsplayback.shortsPlaybackPatch
import app.revanced.patches.youtube.utils.flyoutmenu.flyoutMenuHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList.VIDEO_PLAYBACK
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.playservice.is_20_14_or_greater
import app.revanced.patches.youtube.utils.qualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import app.revanced.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.video.information.*
import app.revanced.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.revanced.patches.youtube.video.videoid.videoIdPatch
import app.revanced.util.fingerprint.*
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.updatePatchStatus
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
private const val EXTENSION_HDR_VIDEO_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/HDRVideoPatch;"
private const val EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/PlaybackSpeedPatch;"
private const val EXTENSION_RELOAD_VIDEO_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/ReloadVideoPatch;"
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
        customPlaybackSpeedPatch(
            "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
            8.0f
        ),
        flyoutMenuHookPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        dismissPlayerHookPatch,
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

        // region patch for disable HDR video

        hdrCapabilityFingerprint.methodOrThrow().apply {
            val stringIndex =
                indexOfFirstStringInstructionOrThrow("av1_profile_main_10_hdr_10_plus_supported")
            val walkerIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                val reference = getReference<MethodReference>()
                reference?.parameterTypes == listOf("I", "Landroid/view/Display;") &&
                        reference.returnType == "Z"
            }

            val walkerMethod = getWalkerMethod(walkerIndex)
            walkerMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $EXTENSION_HDR_VIDEO_CLASS_DESCRIPTOR->disableHDRVideo()Z
                        move-result v0
                        if-nez v0, :default
                        return v0
                        """, ExternalLabel("default", getInstruction(0))
                )
            }
        }

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
                        ?: throw PatchException("Method returning playback speed not found in class $it.")

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
                    val targetIndex = it.patternMatch!!.endIndex
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
        hookPlayerResponseVideoId("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->fetchMusicRequest(Ljava/lang/String;Z)V")
        hookDismissObserver("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->onDismiss()V")

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "RememberPlaybackSpeed")

        // endregion

        // region patch for default video quality

        qualityChangedFromRecyclerViewFingerprint.matchOrThrow().let {
            it.method.apply {
                val newInstanceIndex = implementation?.instructions!!.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.NEW_INSTANCE &&
                            (instruction as? ReferenceInstruction)?.reference?.toString() == "Lcom/google/android/libraries/youtube/innertube/model/media/VideoQuality;"
                }
                if (newInstanceIndex == -1) throw IllegalStateException("VideoQuality new-instance not found")

                addInstruction(
                    newInstanceIndex + 1,
                    "invoke-static {}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->userSelectedVideoQuality()V"
                )
            }
        }

        qualitySetterFingerprint.matchOrThrow().let {
            val onItemClickMethod =
                it.classDef.methods.find { method -> method.name == "onItemClick" }

            onItemClickMethod?.apply {
                addInstruction(
                    0,
                    "invoke-static {}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->userSelectedVideoQuality()V"
                )
            } ?: throw PatchException("Failed to find onItemClick method")
        }

        hookBackgroundPlayVideoInformation("$EXTENSION_RELOAD_VIDEO_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        hookVideoInformation("$EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        onCreateHook(
            EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR,
            "newVideoStarted"
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
