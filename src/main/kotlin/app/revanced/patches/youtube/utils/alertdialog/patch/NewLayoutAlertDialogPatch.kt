package app.revanced.patches.youtube.utils.alertdialog.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.misc.spoofappversion.patch.SpoofAppVersionPatch
import app.revanced.patches.youtube.utils.alertdialog.fingerprints.BottomSheetRecyclerViewBuilderFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("new-layout-alert-dialog")
@DependsOn(
    [
        SharedResourceIdPatch::class,
        SpoofAppVersionPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class NewLayoutAlertDialogPatch : BytecodePatch(
    listOf(BottomSheetRecyclerViewBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        BottomSheetRecyclerViewBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val contextIndex = it.scanResult.patternScanResult!!.startIndex + 3
                val contextRegister = getInstruction<OneRegisterInstruction>(contextIndex).registerA

                val insertIndex = it.scanResult.patternScanResult!!.endIndex

                addInstruction(
                    insertIndex,
                    "invoke-static {v$contextRegister}, $UTILS_PATH/NewPlayerFlyoutPanelsDetectPatch;->showAlertDialog(Landroid/content/Context;)V"
                )
            }
        } ?: return BottomSheetRecyclerViewBuilderFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}