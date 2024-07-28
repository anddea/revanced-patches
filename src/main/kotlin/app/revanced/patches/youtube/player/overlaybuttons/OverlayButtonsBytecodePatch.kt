package app.revanced.patches.youtube.player.overlaybuttons

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.overlaybuttons.fingerprints.*
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.OfflineButton
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.util.*
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    dependencies = [
        MainActivityResolvePatch::class,
        SharedResourceIdPatch::class,
        VideoInformationPatch::class
    ]
)
object OverlayButtonsBytecodePatch : BytecodePatch(
    setOf(
        OfflineVideoEndpointFingerprint,
        PiPPlaybackFingerprint,
        PlayerButtonConstructorFingerprint,
        PlaylistOfflineDownloadOnClickFingerprint,
        PlaylistOfflineDownloadOnClickV1Fingerprint,
        SetVisibilityOfflineArrowViewFingerprint,
        ActivateDownloadButtonFingerprint
    )
) {
    private const val INTEGRATIONS_ALWAYS_REPEAT_CLASS_DESCRIPTOR =
        "$UTILS_PATH/AlwaysRepeatPatch;"

    private const val INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    private const val INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR =
        "$MISC_PATH/DownloadPlaylistButton;"

    override fun execute(context: BytecodeContext) {

        // region patch for hook download button

        OfflineVideoEndpointFingerprint.resultOrThrow().mutableMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static/range {p3 .. p3}, $INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR->inAppDownloadButtonOnClick(Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :show_native_downloader
                    return-void
                    """, ExternalLabel("show_native_downloader", getInstruction(0))
            )
        }

        // Get playlistId and startPlaylistDownloadActivity
        ActivateDownloadButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow { opcode == Opcode.IGET_OBJECT }

                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 2,
                    """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR->enablePlaylistDownloadButton(I)I
                        move-result v$insertRegister
                    """.trimIndent()
                )
            }
        }

        // set download button visible
        setOf(
            PlaylistOfflineDownloadOnClickFingerprint,
            PlaylistOfflineDownloadOnClickV1Fingerprint
        ).forEach { fingerPrint ->
            fingerPrint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = getWideLiteralInstructionIndex(OfflineButton) + 3
                    val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex,
                        "invoke-static {v$insertRegister}, $INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR->setPlaylistDownloadButtonVisibility(Landroid/view/View;)V"
                    )
                }
            }
        }

        SetVisibilityOfflineArrowViewFingerprint.resultOrThrow().let {
            val targetMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)
            targetMethod.apply {
                val insertIndex = getTargetIndexWithMethodReferenceNameOrThrow("setVisibility")
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                replaceInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR->setPlaylistDownloadButtonVisibility(Landroid/view/View;)V"
                )
            }
        }



        PiPPlaybackFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR->getExternalDownloaderLaunchedState(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }

        // endregion

        // region patch for always repeat and pause

        PlayerButtonConstructorFingerprint.resultOrThrow().mutableMethod.apply {
            val registerResolver = implementation!!.registerCount - parameters.size - 1 + 6 // p6

            var invokerObjectIndex = indexOfFirstInstruction {
                opcode == Opcode.IPUT_OBJECT
                        && getReference<FieldReference>()?.definingClass == definingClass
                        && (this as TwoRegisterInstruction).registerA == registerResolver
            }
            if (invokerObjectIndex < 0) {
                val moveObjectIndex = indexOfFirstInstructionOrThrow {
                    (this as? TwoRegisterInstruction)?.registerB == registerResolver
                }
                val moveObjectRegister =
                    getInstruction<TwoRegisterInstruction>(moveObjectIndex).registerA
                invokerObjectIndex = indexOfFirstInstructionOrThrow(moveObjectIndex) {
                    opcode == Opcode.IPUT_OBJECT
                            && getReference<FieldReference>()?.definingClass == definingClass
                            && (this as TwoRegisterInstruction).registerA == moveObjectRegister
                }
            }
            val invokerObjectReference =
                getInstruction<ReferenceInstruction>(invokerObjectIndex).reference

            val onClickListenerReferenceIndex =
                getTargetIndexWithReferenceOrThrow("<init>(Ljava/lang/Object;I[B)V")
            val onClickListenerReference =
                getInstruction<ReferenceInstruction>(onClickListenerReferenceIndex).reference
            val onClickListenerClass =
                context.findClass((onClickListenerReference as MethodReference).definingClass)!!.mutableClass

            var invokeInterfaceReference = ""
            onClickListenerClass.methods.find { method -> method.name == "onClick" }
                ?.apply {
                    val invokeInterfaceIndex =
                        getTargetIndexWithReferenceOrThrow(invokerObjectReference.toString()) + 1
                    if (getInstruction(invokeInterfaceIndex).opcode != Opcode.INVOKE_INTERFACE)
                        throw PatchException("Opcode does not match")
                    invokeInterfaceReference =
                        getInstruction<ReferenceInstruction>(invokeInterfaceIndex).reference.toString()
                } ?: throw PatchException("Could not find onClick method")

            val alwaysRepeatMutableClass =
                context.findClass(INTEGRATIONS_ALWAYS_REPEAT_CLASS_DESCRIPTOR)!!.mutableClass

            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    iget-object v1, v0, $invokerObjectReference
                    if-eqz v1, :ignore
                    invoke-interface {v1}, $invokeInterfaceReference
                    :ignore
                    return-void
                    """

            alwaysRepeatMutableClass.addFieldAndInstructions(
                context,
                "pauseVideo",
                "pauseButtonClass",
                definingClass,
                smaliInstructions,
                true
            )
        }

        VideoInformationPatch.videoEndMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $INTEGRATIONS_ALWAYS_REPEAT_CLASS_DESCRIPTOR->alwaysRepeat()Z
                    move-result v0
                    if-eqz v0, :end
                    return-void
                    """, ExternalLabel("end", getInstruction(0))
            )
        }

        // endregion

    }
}
