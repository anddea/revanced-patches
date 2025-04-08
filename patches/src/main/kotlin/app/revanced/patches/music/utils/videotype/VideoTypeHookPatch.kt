package app.revanced.patches.music.utils.videotype

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.extension.Constants.UTILS_PATH
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/VideoTypeHookPatch;"

@Suppress("unused")
val videoTypeHookPatch = bytecodePatch(
    description = "videoTypeHookPatch"
) {

    execute {

        videoTypeFingerprint.methodOrThrow(videoTypeParentFingerprint).apply {
            val getEnumIndex = indexOfGetEnumInstruction(this)
            val enumClass =
                (getInstruction<ReferenceInstruction>(getEnumIndex).reference as MethodReference).definingClass
            val referenceIndex = indexOfFirstInstructionOrThrow(getEnumIndex) {
                opcode == Opcode.SGET_OBJECT &&
                        getReference<FieldReference>()?.type == enumClass
            }
            val referenceInstruction =
                getInstruction<ReferenceInstruction>(referenceIndex).reference

            val insertIndex = indexOfFirstInstructionOrThrow(getEnumIndex, Opcode.IF_NEZ)
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructionsWithLabels(
                insertIndex, """
                    if-nez v$insertRegister, :dismiss
                    sget-object v$insertRegister, $referenceInstruction
                    :dismiss
                    invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->setVideoType(Ljava/lang/Enum;)V
                    """
            )
        }
    }
}
