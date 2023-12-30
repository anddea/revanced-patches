package app.revanced.patches.shared.patch.tracking

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.shared.fingerprints.tracking.CopyTextEndpointFingerprint
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

abstract class AbstractSanitizeUrlQueryPatch(
    private val descriptor: String,
    private val additionalFingerprints: Set<MethodFingerprint> = emptySet()
) : BytecodePatch(
    buildSet {
        add(CopyTextEndpointFingerprint)
        additionalFingerprints.let(::addAll)
    }
) {
    private fun MethodFingerprint.additionalInvoke() {
        result?.let {
            it.mutableMethod.apply {
                for ((index, instruction) in implementation!!.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL)
                        continue

                    if ((instruction as ReferenceInstruction).reference.toString() != "Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;")
                        continue

                    if (getInstruction(index + 1).opcode != Opcode.GOTO)
                        continue

                    val invokeInstruction = instruction as FiveRegisterInstruction

                    replaceInstruction(
                        index,
                        "invoke-static {v${invokeInstruction.registerC}, v${invokeInstruction.registerD}, v${invokeInstruction.registerE}}, "
                                + "$descriptor->stripQueryParameters(Landroid/content/Intent;Ljava/lang/String;Ljava/lang/String;)V"
                    )
                }
            }
        } ?: throw exception
    }

    private fun MethodFingerprint.invoke() {
        result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 2, """
                        invoke-static {v$targetRegister}, $descriptor->stripQueryParameters(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        } ?: throw exception
    }

    override fun execute(context: BytecodeContext) {
        CopyTextEndpointFingerprint.invoke()

        if (additionalFingerprints.isNotEmpty()) {
            additionalFingerprints.forEach { fingerprint ->
                fingerprint.additionalInvoke()
            }
        }
    }
}