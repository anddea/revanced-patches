package app.revanced.patches.youtube.utils.dismiss

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

private lateinit var dismissMethod: MutableMethod

val dismissPlayerHookPatch = bytecodePatch(
    description = "dismissPlayerHookPatch"
) {
    dependsOn(sharedExtensionPatch)

    execute {
        dismissPlayerOnClickListenerFingerprint.methodOrThrow().apply {
            val literalIndex =
                indexOfFirstLiteralInstructionOrThrow(DISMISS_PLAYER_LITERAL)
            val dismissPlayerIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_VIRTUAL &&
                        reference?.returnType == "V" &&
                        reference.parameterTypes.isEmpty()
            }

            getWalkerMethod(dismissPlayerIndex).apply {
                val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.returnType == "V"
                }
                getWalkerMethod(jumpIndex).apply {
                    val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.returnType == "V"
                    }
                    dismissMethod = getWalkerMethod(jumpIndex)
                }
            }

            val dismissPlayerReference =
                getInstruction<ReferenceInstruction>(dismissPlayerIndex).reference as MethodReference
            val dismissPlayerClass = dismissPlayerReference.definingClass

            val fieldIndex =
                indexOfFirstInstructionReversedOrThrow(dismissPlayerIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == dismissPlayerClass
                }
            val fieldReference =
                getInstruction<ReferenceInstruction>(fieldIndex).reference as FieldReference

            findMethodOrThrow(fieldReference.definingClass).apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>() == fieldReference
                }
                val insertRegister =
                    getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "sput-object v$insertRegister, $EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR->dismissPlayerClass:$dismissPlayerClass"
                )

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0}, $dismissPlayerReference
                        :ignore
                        return-void
                        """

                addStaticFieldToExtension(
                    EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                    "dismissPlayer",
                    "dismissPlayerClass",
                    dismissPlayerClass,
                    smaliInstructions,
                    false
                )
            }
        }
    }
}

/**
 * This method is called when the video is closed.
 */
internal fun hookDismissObserver(descriptor: String) =
    dismissMethod.addInstruction(
        0,
        "invoke-static {}, $descriptor"
    )