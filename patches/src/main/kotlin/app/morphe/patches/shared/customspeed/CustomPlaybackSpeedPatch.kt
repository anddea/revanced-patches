package app.morphe.patches.shared.customspeed

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.playservice.is_20_34_or_greater
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private var patchIncluded = false

fun customPlaybackSpeedPatch(
    descriptor: String,
    maxSpeed: Float
) = bytecodePatch(
    description = "customPlaybackSpeedPatch"
) {
    execute {
        if (patchIncluded) {
            return@execute
        }

        arrayGeneratorFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.instructionMatches.first().index
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $descriptor->getLength(I)I
                        move-result v$targetRegister
                        """
                )

                val sizeIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "size"
                } + 1
                val sizeRegister = getInstruction<OneRegisterInstruction>(sizeIndex).registerA

                addInstructions(
                    sizeIndex + 1, """
                        invoke-static {v$sizeRegister}, $descriptor->getSize(I)I
                        move-result v$sizeRegister
                        """
                )

                val arrayIndex = indexOfFirstInstructionOrThrow {
                    getReference<FieldReference>()?.type == "[F"
                }
                val arrayRegister = getInstruction<OneRegisterInstruction>(arrayIndex).registerA

                addInstructions(
                    arrayIndex + 1, """
                        invoke-static {v$arrayRegister}, $descriptor->getArray([F)[F
                        move-result-object v$arrayRegister
                        """
                )
            }
        }

        val limiterMethods = if (is_20_34_or_greater) {
            setOf(limiterFingerprint.methodOrThrow())
        } else {
            setOf(
                limiterFallBackFingerprint.methodOrThrow(),
                limiterLegacyFingerprint.methodOrThrow(limiterFallBackFingerprint)
            )
        }

        limiterMethods.forEach { method ->
            method.apply {
                val limitMinIndex = indexOfFirstLiteralInstructionOrThrow(0.25f.toRawBits().toLong())

                val limitMaxIndex = if (is_20_34_or_greater) {
                    indexOfFirstLiteralInstructionOrThrow(4.0f.toRawBits().toLong())
                } else {
                    indexOfFirstInstructionOrThrow(limitMinIndex + 1, Opcode.CONST_HIGH16)
                }

                val limitMinRegister = getInstruction<OneRegisterInstruction>(limitMinIndex).registerA
                val limitMaxRegister = getInstruction<OneRegisterInstruction>(limitMaxIndex).registerA

                replaceInstruction(
                    limitMinIndex,
                    "const/high16 v$limitMinRegister, 0x0"
                )
                replaceInstruction(
                    limitMaxIndex,
                    "const/high16 v$limitMaxRegister, ${maxSpeed.toRawBits()}"
                )
            }
        }

        patchIncluded = true

    }
}
