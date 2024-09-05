package app.revanced.patches.shared.tracking

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.shared.integrations.Constants.PATCHES_PATH
import app.revanced.patches.shared.tracking.fingerprints.CopyTextEndpointFingerprint
import app.revanced.patches.shared.tracking.fingerprints.ShareLinkFormatterFingerprint
import app.revanced.patches.shared.tracking.fingerprints.SystemShareLinkFormatterFingerprint
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

object BaseSanitizeUrlQueryPatch : BytecodePatch(
    setOf(
        CopyTextEndpointFingerprint,
        ShareLinkFormatterFingerprint,
        SystemShareLinkFormatterFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$PATCHES_PATH/SanitizeUrlQueryPatch;"

    override fun execute(context: BytecodeContext) {
        CopyTextEndpointFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 2, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->stripQueryParameters(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        arrayOf(
            ShareLinkFormatterFingerprint,
            SystemShareLinkFormatterFingerprint
        ).forEach { fingerprint ->
            fingerprint.resultOrThrow().let {
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
                                    + "$INTEGRATIONS_CLASS_DESCRIPTOR->stripQueryParameters(Landroid/content/Intent;Ljava/lang/String;Ljava/lang/String;)V"
                        )
                    }
                }
            }
        }
    }
}