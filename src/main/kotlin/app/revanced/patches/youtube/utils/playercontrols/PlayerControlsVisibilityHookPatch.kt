package app.revanced.patches.youtube.utils.playercontrols

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityEntityModelFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object PlayerControlsVisibilityHookPatch : BytecodePatch(
    setOf(PlayerControlsVisibilityEntityModelFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$UTILS_PATH/PlayerControlsVisibilityHookPatch;"

    override fun execute(context: BytecodeContext) {

        PlayerControlsVisibilityEntityModelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val iGetReference = getInstruction<ReferenceInstruction>(startIndex).reference
                val staticReference = getInstruction<ReferenceInstruction>(startIndex + 1).reference

                it.mutableClass.methods.find { method -> method.name == "<init>" }?.apply {
                    val targetIndex = getTargetIndex(Opcode.IPUT_OBJECT)
                    val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            iget v$targetRegister, v$targetRegister, $iGetReference
                            invoke-static {v$targetRegister}, $staticReference
                            move-result-object v$targetRegister
                            invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setPlayerControlsVisibility(Ljava/lang/Enum;)V
                            """
                    )
                } ?: throw PatchException("Constructor method not found")
            }
        }
    }
}
