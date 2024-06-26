package app.revanced.patches.shared.customspeed

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.shared.customspeed.fingerprints.SpeedArrayGeneratorFingerprint
import app.revanced.patches.shared.customspeed.fingerprints.SpeedLimiterFallBackFingerprint
import app.revanced.patches.shared.customspeed.fingerprints.SpeedLimiterFingerprint
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getTargetIndexWithFieldReferenceTypeOrThrow
import app.revanced.util.getTargetIndexWithMethodReferenceNameOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

abstract class BaseCustomPlaybackSpeedPatch(
    private val descriptor: String,
    private val maxSpeed: Float
) : BytecodePatch(
    setOf(
        SpeedArrayGeneratorFingerprint,
        SpeedLimiterFallBackFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        SpeedArrayGeneratorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $descriptor->getLength(I)I
                        move-result v$targetRegister
                        """
                )

                val sizeIndex = getTargetIndexWithMethodReferenceNameOrThrow("size") + 1
                val sizeRegister = getInstruction<OneRegisterInstruction>(sizeIndex).registerA

                addInstructions(
                    sizeIndex + 1, """
                        invoke-static {v$sizeRegister}, $descriptor->getSize(I)I
                        move-result v$sizeRegister
                        """
                )

                val arrayIndex = getTargetIndexWithFieldReferenceTypeOrThrow("[F")
                val arrayRegister = getInstruction<OneRegisterInstruction>(arrayIndex).registerA

                addInstructions(
                    arrayIndex + 1, """
                        invoke-static {v$arrayRegister}, $descriptor->getArray([F)[F
                        move-result-object v$arrayRegister
                        """
                )
            }
        }

        val speedLimiterParentResult = SpeedLimiterFallBackFingerprint.resultOrThrow()
        SpeedLimiterFingerprint.resolve(context, speedLimiterParentResult.classDef)
        val speedLimiterResult = SpeedLimiterFingerprint.resultOrThrow()

        arrayOf(
            speedLimiterParentResult,
            speedLimiterResult
        ).forEach {
            it.mutableMethod.apply {
                val limiterMinConstIndex =
                    indexOfFirstInstructionOrThrow { (this as? NarrowLiteralInstruction)?.narrowLiteral == 0.25f.toRawBits() }
                val limiterMaxConstIndex =
                    getTargetIndexOrThrow(limiterMinConstIndex + 1, Opcode.CONST_HIGH16)

                val limiterMinConstDestination =
                    getInstruction<OneRegisterInstruction>(limiterMinConstIndex).registerA
                val limiterMaxConstDestination =
                    getInstruction<OneRegisterInstruction>(limiterMaxConstIndex).registerA

                replaceInstruction(
                    limiterMinConstIndex,
                    "const/high16 v$limiterMinConstDestination, 0x0"
                )
                replaceInstruction(
                    limiterMaxConstIndex,
                    "const/high16 v$limiterMaxConstDestination, ${maxSpeed.toRawBits()}"
                )
            }
        }

    }
}
