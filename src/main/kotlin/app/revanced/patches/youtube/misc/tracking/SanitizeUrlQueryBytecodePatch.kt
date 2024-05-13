package app.revanced.patches.youtube.misc.tracking

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.shared.tracking.BaseSanitizeUrlQueryPatch
import app.revanced.patches.shared.tracking.fingerprints.CopyTextEndpointFingerprint
import app.revanced.patches.youtube.misc.tracking.fingerprints.ShareLinkFormatterFingerprint
import app.revanced.patches.youtube.misc.tracking.fingerprints.SystemShareLinkFormatterFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

object SanitizeUrlQueryBytecodePatch : BaseSanitizeUrlQueryPatch(
    "$MISC_PATH/SanitizeUrlQueryPatch;",
    listOf(CopyTextEndpointFingerprint),
    listOf(
        ShareLinkFormatterFingerprint,
        SystemShareLinkFormatterFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/SanitizeUrlQueryPatch;"

    override fun execute(context: BytecodeContext) {
        super.execute(context)

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
