package app.revanced.patches.youtube.general.downloads

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.general.downloads.fingerprints.AccessibilityOfflineButtonSyncFingerprint
import app.revanced.patches.youtube.general.downloads.fingerprints.DownloadPlaylistButtonOnClickFingerprint
import app.revanced.patches.youtube.general.downloads.fingerprints.DownloadPlaylistButtonOnClickFingerprint.indexOfPlaylistDownloadActionInvokeInstruction
import app.revanced.patches.youtube.general.downloads.fingerprints.OfflineVideoEndpointFingerprint
import app.revanced.patches.youtube.general.downloads.fingerprints.SetPlaylistDownloadButtonVisibilityFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.pip.PiPStateHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.alsoResolve
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object DownloadActionsPatch : BaseBytecodePatch(
    name = "Hook download actions",
    description = "Adds support to download videos with an external downloader app using the in-app download button.",
    dependencies = setOf(
        PiPStateHookPatch::class,
        SharedResourceIdPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = Constants.COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AccessibilityOfflineButtonSyncFingerprint,
        DownloadPlaylistButtonOnClickFingerprint,
        OfflineVideoEndpointFingerprint,
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$GENERAL_PATH/DownloadActionsPatch;"

    override fun execute(context: BytecodeContext) {

        // region patch for hook download actions (video action bar and flyout panel)

        OfflineVideoEndpointFingerprint.resultOrThrow().mutableMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static/range {p3 .. p3}, $INTEGRATIONS_CLASS_DESCRIPTOR->inAppVideoDownloadButtonOnClick(Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :show_native_downloader
                    return-void
                    """, ExternalLabel("show_native_downloader", getInstruction(0))
            )
        }

        // endregion

        // region patch for hook download actions (playlist)

        val onClickListenerClass =
            DownloadPlaylistButtonOnClickFingerprint.resultOrThrow().mutableMethod.let {
                val playlistDownloadActionInvokeIndex =
                    indexOfPlaylistDownloadActionInvokeInstruction(it)

                it.getInstructions().subList(
                    playlistDownloadActionInvokeIndex - 10,
                    playlistDownloadActionInvokeIndex,
                ).find { instruction ->
                    instruction.opcode == Opcode.INVOKE_VIRTUAL_RANGE
                            && instruction.getReference<MethodReference>()?.parameterTypes?.first() == "Ljava/lang/String;"
                }?.getReference<MethodReference>()?.returnType
                    ?: throw PatchException("Could not find onClickListenerClass")
            }

        context.findClass(onClickListenerClass)
            ?.mutableClass
            ?.methods
            ?.first { method -> method.name == "onClick" }?.apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC
                            && getReference<MethodReference>()?.name == "isEmpty"
                }
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->inAppPlaylistDownloadButtonOnClick(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$insertRegister
                        """
                )
            } ?: throw PatchException("Could not find class $onClickListenerClass")

        // endregion

        // region patch for show the playlist download button

        SetPlaylistDownloadButtonVisibilityFingerprint
            .alsoResolve(context, AccessibilityOfflineButtonSyncFingerprint).let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex + 2
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->overridePlaylistDownloadButtonVisibility()Z
                            move-result v$insertRegister
                            """
                    )
                }
            }

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: HOOK_DOWNLOAD_ACTIONS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}