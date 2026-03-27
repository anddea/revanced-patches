package app.morphe.patches.shared.opus

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/OpusCodecPatch;"

fun baseOpusCodecsPatch() = bytecodePatch(
    description = "baseOpusCodecsPatch"
) {
    execute {
        val opusCodecReference = with(codecReferenceFingerprint.methodOrThrow()) {
            val codecIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.returnType == "Ljava/util/Set;"
            }
            getInstruction<ReferenceInstruction>(codecIndex).reference
        }

        codecSelectorFingerprint.matchOrThrow().let {
            it.method.apply {
                val freeRegister = implementation!!.registerCount - parameters.size - 2
                val targetIndex = it.instructionMatches.last().index
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructionsWithLabels(
                    targetIndex + 1, """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->enableOpusCodec()Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :mp4a
                        invoke-static {}, $opusCodecReference
                        move-result-object v$targetRegister
                        """, ExternalLabel("mp4a", getInstruction(targetIndex + 1))
                )
            }
        }
    }
}

