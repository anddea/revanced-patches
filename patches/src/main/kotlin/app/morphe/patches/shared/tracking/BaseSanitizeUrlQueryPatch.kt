package app.morphe.patches.shared.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/SanitizeUrlQueryPatch;"

internal fun MutableMethod.hookQueryParameters(index: Int) {
    val invokeInstruction = getInstruction(index) as FiveRegisterInstruction

    replaceInstruction(
        index,
        "invoke-static {v${invokeInstruction.registerC}, v${invokeInstruction.registerD}, v${invokeInstruction.registerE}}, " +
                "$EXTENSION_CLASS_DESCRIPTOR->stripQueryParameters(Landroid/content/Intent;Ljava/lang/String;Ljava/lang/String;)V"
    )
}

val baseSanitizeUrlQueryPatch = bytecodePatch(
    description = "baseSanitizeUrlQueryPatch"
) {
    execute {
        copyTextEndpointFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.instructionMatches.first().index
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 2, """
                        invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->stripQueryParameters(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        setOf(
            shareLinkFormatterFingerprint,
            systemShareLinkFormatterFingerprint
        ).forEach { fingerprint ->
            fingerprint.methodOrThrow().apply {
                for ((index, instruction) in implementation!!.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL)
                        continue

                    if ((instruction as ReferenceInstruction).reference.toString() != "Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;")
                        continue

                    if (getInstruction(index + 1).opcode != Opcode.GOTO)
                        continue

                    hookQueryParameters(index)
                }
            }
        }
    }
}
