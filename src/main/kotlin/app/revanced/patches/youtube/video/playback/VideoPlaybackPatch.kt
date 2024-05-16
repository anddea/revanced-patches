package app.revanced.patches.youtube.video.playback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fingerprints.QualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoEndFingerprint
import app.revanced.patches.youtube.utils.fix.shortsplayback.ShortsPlaybackPatch
import app.revanced.patches.youtube.utils.flyoutmenu.FlyoutMenuHookPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.recyclerview.BottomSheetRecyclerViewPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch.speedSelectionInsertMethod
import app.revanced.patches.youtube.video.playback.fingerprints.AV1CodecFingerprint
import app.revanced.patches.youtube.video.playback.fingerprints.ByteBufferArrayFingerprint
import app.revanced.patches.youtube.video.playback.fingerprints.ByteBufferArrayParentFingerprint
import app.revanced.patches.youtube.video.playback.fingerprints.DeviceDimensionsModelToStringFingerprint
import app.revanced.patches.youtube.video.playback.fingerprints.HDRCapabilityFingerprint
import app.revanced.patches.youtube.video.playback.fingerprints.PlaybackSpeedChangedFromRecyclerViewFingerprint
import app.revanced.patches.youtube.video.playback.fingerprints.PlaybackSpeedInitializeFingerprint
import app.revanced.patches.youtube.video.playback.fingerprints.QualityChangedFromRecyclerViewFingerprint
import app.revanced.patches.youtube.video.playback.fingerprints.QualitySetterFingerprint
import app.revanced.patches.youtube.video.videoid.VideoIdPatch
import app.revanced.util.getReference
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
object VideoPlaybackPatch : BaseBytecodePatch(
    name = "Video playback",
    description = "Adds options to customize settings related to video playback," +
            "such as default video quality and playback speed, etc.",
    dependencies = setOf(
        BottomSheetRecyclerViewPatch::class,
        CustomPlaybackSpeedPatch::class,
        FlyoutMenuHookPatch::class,
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        ShortsPlaybackPatch::class,
        VideoIdPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AV1CodecFingerprint,
        ByteBufferArrayParentFingerprint,
        DeviceDimensionsModelToStringFingerprint,
        HDRCapabilityFingerprint,
        PlaybackSpeedChangedFromRecyclerViewFingerprint,
        QualityChangedFromRecyclerViewFingerprint,
        QualityMenuViewInflateFingerprint,
        QualitySetterFingerprint,
        VideoEndFingerprint
    )
) {
    private const val PLAYBACK_SPEED_MENU_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlaybackSpeedMenuFilter;"
    private const val VIDEO_QUALITY_MENU_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/VideoQualityMenuFilter;"
    private const val INTEGRATIONS_AV1_CODEC_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/AV1CodecPatch;"
    private const val INTEGRATIONS_CUSTOM_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/CustomPlaybackSpeedPatch;"
    private const val INTEGRATIONS_HDR_VIDEO_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/HDRVideoPatch;"
    private const val INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/PlaybackSpeedPatch;"
    private const val INTEGRATIONS_RELOAD_VIDEO_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/ReloadVideoPatch;"
    private const val INTEGRATIONS_RESTORE_OLD_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/RestoreOldVideoQualityMenuPatch;"
    private const val INTEGRATIONS_SPOOF_DEVICE_DIMENSIONS_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/SpoofDeviceDimensionsPatch;"
    private const val INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/VideoQualityPatch;"

    override fun execute(context: BytecodeContext) {

        // region patch for custom playback speed

        BottomSheetRecyclerViewPatch.injectCall("$INTEGRATIONS_CUSTOM_PLAYBACK_SPEED_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")
        LithoFilterPatch.addFilter(PLAYBACK_SPEED_MENU_FILTER_CLASS_DESCRIPTOR)

        // endregion

        // region patch for disable HDR video

        HDRCapabilityFingerprint.resultOrThrow().mutableMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $INTEGRATIONS_HDR_VIDEO_CLASS_DESCRIPTOR->disableHDRVideo()Z
                    move-result v0
                    if-nez v0, :default
                    return v0
                    """, ExternalLabel("default", getInstruction(0))
            )
        }

        // endregion

        // region patch for default playback speed

        PlaybackSpeedChangedFromRecyclerViewFingerprint.resolve(
            context,
            QualityChangedFromRecyclerViewFingerprint.resultOrThrow().classDef
        )

        val newMethod = PlaybackSpeedChangedFromRecyclerViewFingerprint.resultOrThrow().mutableMethod

        arrayOf(
            newMethod,
            speedSelectionInsertMethod
        ).forEach {
            it.apply {
                val speedSelectionValueInstructionIndex = getTargetIndex(Opcode.IGET)
                val speedSelectionValueRegister =
                    getInstruction<TwoRegisterInstruction>(speedSelectionValueInstructionIndex).registerA

                addInstruction(
                    speedSelectionValueInstructionIndex + 1,
                    "invoke-static {v$speedSelectionValueRegister}, " +
                            "$INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->userSelectedPlaybackSpeed(F)V"
                )
            }
        }

        PlaybackSpeedInitializeFingerprint.resolve(
            context,
            VideoEndFingerprint.resultOrThrow().classDef
        )
        PlaybackSpeedInitializeFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeedInShorts(F)F
                        move-result v$insertRegister
                        """
                )
            }
        }

        VideoInformationPatch.hookBackgroundPlay("$INTEGRATIONS_PLAYBACK_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        context.updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "RememberPlaybackSpeed")

        // endregion

        // region patch for default video quality

        QualityChangedFromRecyclerViewFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val index = it.scanResult.patternScanResult!!.startIndex

                addInstruction(
                    index + 1,
                    "invoke-static {}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userSelectedVideoQuality()V"
                )

            }
        }

        QualitySetterFingerprint.resultOrThrow().let {
            val onItemClickMethod =
                it.mutableClass.methods.find { method -> method.name == "onItemClick" }

            onItemClickMethod?.apply {
                addInstruction(
                    0,
                    "invoke-static {}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userSelectedVideoQuality()V"
                )
            } ?: throw PatchException("Failed to find onItemClick method")
        }

        VideoInformationPatch.hookBackgroundPlay("$INTEGRATIONS_RELOAD_VIDEO_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        VideoInformationPatch.hook("$INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        // endregion

        // region patch for restore old video quality menu

        val videoQualityClass = QualitySetterFingerprint.resultOrThrow().mutableMethod.definingClass

        QualityMenuViewInflateFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndex(Opcode.CHECK_CAST)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "invoke-static { v$insertRegister }, " +
                            "$INTEGRATIONS_RESTORE_OLD_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->restoreOldVideoQualityMenu(Landroid/widget/ListView;)V"
                )
            }
            val onItemClickMethod =
                it.mutableClass.methods.find { method -> method.name == "onItemClick" }

            onItemClickMethod?.apply {
                val insertIndex = getTargetIndex(Opcode.IGET_OBJECT)
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                val jumpIndex = indexOfFirstInstruction {
                    opcode == Opcode.IGET_OBJECT
                            && this.getReference<FieldReference>()?.type == videoQualityClass
                }

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $INTEGRATIONS_RESTORE_OLD_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->restoreOldVideoQualityMenu()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :show
                        """, ExternalLabel("show", getInstruction(jumpIndex))
                )
            } ?: throw PatchException("Failed to find onItemClick method")
        }

        BottomSheetRecyclerViewPatch.injectCall("$INTEGRATIONS_RESTORE_OLD_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")
        LithoFilterPatch.addFilter(VIDEO_QUALITY_MENU_FILTER_CLASS_DESCRIPTOR)

        // endregion

        // region patch for spoof device dimensions

        DeviceDimensionsModelToStringFingerprint.resultOrThrow().let { result ->
            result.mutableClass.methods.first { method -> MethodUtil.isConstructor(method) }
                .addInstructions(
                    1, // Add after super call.
                    mapOf(
                        1 to "MinHeightOrWidth", // p1 = min height
                        2 to "MaxHeightOrWidth", // p2 = max height
                        3 to "MinHeightOrWidth", // p3 = min width
                        4 to "MaxHeightOrWidth"  // p4 = max width
                    ).map { (parameter, method) ->
                        """
                            invoke-static { p$parameter }, $INTEGRATIONS_SPOOF_DEVICE_DIMENSIONS_CLASS_DESCRIPTOR->get$method(I)I
                            move-result p$parameter
                            """
                    }.joinToString("\n") { it }
                )
        }

        // endregion

        // region patch for disable AV1 codec

        // replace av1 codec

        AV1CodecFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getStringInstructionIndex("video/av01")
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_AV1_CODEC_CLASS_DESCRIPTOR->replaceCodec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$insertRegister
                        """
                )
            }

            SettingsPatch.addPreference(
                arrayOf(
                    "SETTINGS: REPLACE_AV1_CODEC"
                )
            )
        } // for compatibility with old versions, no exceptions are raised.

        // reject av1 codec response

        ByteBufferArrayParentFingerprint.resultOrThrow().classDef.let { classDef ->
            ByteBufferArrayFingerprint.also { it.resolve(context, classDef) }.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $INTEGRATIONS_AV1_CODEC_CLASS_DESCRIPTOR->rejectResponse(I)I
                            move-result v$insertRegister
                            """
                    )
                }
            }
        }

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: VIDEO"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
