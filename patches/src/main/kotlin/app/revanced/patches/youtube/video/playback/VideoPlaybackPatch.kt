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
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.fix.shortsplayback.shortsPlaybackPatch
import app.revanced.patches.youtube.utils.flyoutmenu.flyoutMenuHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList.VIDEO_PLAYBACK
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.qualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import app.revanced.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.videoEndFingerprint
import app.revanced.patches.youtube.video.information.hookBackgroundPlayVideoInformation
import app.revanced.patches.youtube.video.information.hookVideoInformation
import app.revanced.patches.youtube.video.information.onCreateHook
import app.revanced.patches.youtube.video.information.speedSelectionInsertMethod
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.revanced.patches.youtube.video.videoid.videoIdPatch
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.definingClassOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val PLAYBACK_SPEED_MENU_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlaybackSpeedMenuFilter;"
private const val VIDEO_QUALITY_MENU_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/VideoQualityMenuFilter;"
private const val EXTENSION_AV1_CODEC_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/AV1CodecPatch;"
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
private const val EXTENSION_RESTORE_OLD_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/RestoreOldVideoQualityMenuPatch;"
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

        playbackSpeedInitializeFingerprint.matchOrThrow(videoEndFingerprint).let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeedInShorts(F)F
                        move-result v$insertRegister
                        """
                )
            }
        }

        hookBackgroundPlayVideoInformation("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        hookPlayerResponseVideoId("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->fetchMusicRequest(Ljava/lang/String;Z)V")

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "RememberPlaybackSpeed")

        // endregion

        // region patch for default video quality

        qualityChangedFromRecyclerViewFingerprint.matchOrThrow().let {
            it.method.apply {
                val index = it.patternMatch!!.startIndex

                addInstruction(
                    index + 1,
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

        // region patch for restore old video quality menu

        qualityMenuViewInflateFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.CHECK_CAST)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "invoke-static { v$insertRegister }, " +
                            "$EXTENSION_RESTORE_OLD_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->restoreOldVideoQualityMenu(Landroid/widget/ListView;)V"
                )
            }
            val onItemClickMethod =
                it.classDef.methods.find { method -> method.name == "onItemClick" }

            onItemClickMethod?.apply {
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IGET_OBJECT)
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                val jumpIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_OBJECT
                            && this.getReference<FieldReference>()?.type == qualitySetterFingerprint.definingClassOrThrow()
                }

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $EXTENSION_RESTORE_OLD_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->restoreOldVideoQualityMenu()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :show
                        """, ExternalLabel("show", getInstruction(jumpIndex))
                )
            } ?: throw PatchException("Failed to find onItemClick method")
        }

        recyclerViewTreeObserverHook("$EXTENSION_RESTORE_OLD_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")
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

        // region patch for disable AV1 codec

        // replace av1 codec

        if (av1CodecFingerprint.resolvable()) {
            av1CodecFingerprint.methodOrThrow().apply {
                val insertIndex = indexOfFirstStringInstructionOrThrow("video/av01")
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1, """
                        invoke-static/range {v$insertRegister .. v$insertRegister}, $EXTENSION_AV1_CODEC_CLASS_DESCRIPTOR->replaceCodec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$insertRegister
                        """
                )
            }
            settingArray += "SETTINGS: REPLACE_AV1_CODEC"
        }

        // reject av1 codec response

        byteBufferArrayFingerprint.matchOrThrow(byteBufferArrayParentFingerprint).let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $EXTENSION_AV1_CODEC_CLASS_DESCRIPTOR->rejectResponse(I)I
                        move-result v$insertRegister
                        """
                )
            }
        }

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
