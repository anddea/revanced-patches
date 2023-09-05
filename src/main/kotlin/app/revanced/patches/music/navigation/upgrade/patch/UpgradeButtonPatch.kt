package app.revanced.patches.music.navigation.upgrade.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.navigation.upgrade.fingerprints.NotifierShelfFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fingerprints.TabLayoutTextFingerprint
import app.revanced.patches.music.utils.integrations.patch.IntegrationsPatch
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.MUSIC_NAVIGATION
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch
@Name("Hide upgrade button")
@Description("Hides upgrade button from navigation bar and hide upgrade banner from homepage.")
@DependsOn(
    [
        IntegrationsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class UpgradeButtonPatch : BytecodePatch(
    listOf(
        NotifierShelfFingerprint,
        TabLayoutTextFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        TabLayoutTextFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex + 3
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.AND_INT_LIT8
                } - 2

                for ((index, instruction) in implementation!!.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.INVOKE_INTERFACE) continue

                    if ((getInstruction<Instruction35c>(index).reference as MethodReference).name != "hasNext") continue

                    addInstructionsWithLabels(
                        insertIndex, """
                            invoke-static {v$targetRegister}, $MUSIC_NAVIGATION->hideUpgradeButton(Ljava/lang/Enum;)Z
                            move-result v$targetRegister
                            if-nez v$targetRegister, :hide
                            """, ExternalLabel("hide", getInstruction(index))
                    )
                    break
                }
            }
        } ?: throw TabLayoutTextFingerprint.exception

        NotifierShelfFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA
                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, Lapp/revanced/music/utils/ReVancedUtils;->hideViewByLayoutParams(Landroid/view/View;)V"
                )
            }
        } ?: throw NotifierShelfFingerprint.exception

    }
}
