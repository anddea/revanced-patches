package app.revanced.patches.music.misc.upgradebutton.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.misc.upgradebutton.fingerprints.*
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.integrations.Constants.INTEGRATIONS_PATH
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-upgrade-button")
@Description("Remove upgrade tab from pivot bar, hide upgrade banner from homepage.")
@DependsOn(
    [
        MusicIntegrationsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class RemoveUpgradeButtonPatch : BytecodePatch(
    listOf(
        PivotBarConstructorFingerprint,
        NotifierShelfFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        PivotBarConstructorFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegisterA = instruction<TwoRegisterInstruction>(targetIndex).registerA
                val targetRegisterB = instruction<TwoRegisterInstruction>(targetIndex).registerB

                val replaceReference = instruction<ReferenceInstruction>(targetIndex).reference.toString()

                replaceInstruction(
                    targetIndex,
                    "invoke-interface {v$targetRegisterA}, Ljava/util/List;->size()I"
                )
                addInstructions(
                    targetIndex + 1,"""
                        move-result v1
                        const/4 v2, 0x3
                        if-le v1, v2, :dismiss
                        invoke-interface {v$targetRegisterA, v2}, Ljava/util/List;->remove(I)Ljava/lang/Object;
                        :dismiss
                        iput-object v$targetRegisterA, v$targetRegisterB, $replaceReference
                        """
                )
            }
        } ?: return PivotBarConstructorFingerprint.toErrorResult()

        NotifierShelfFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = instruction<OneRegisterInstruction>(targetIndex).registerA
                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_PATH/adremover/AdRemoverAPI;->HideViewWithLayout1dp(Landroid/view/View;)V"
                )
            }
        } ?: return NotifierShelfFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
