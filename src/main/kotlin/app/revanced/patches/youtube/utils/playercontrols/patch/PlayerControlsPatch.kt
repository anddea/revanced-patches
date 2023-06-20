package app.revanced.patches.youtube.utils.playercontrols.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.BottomControlsInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.ControlsLayoutInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityModelFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.SeekEDUVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.SpeedEduVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.SpeedEduVisibleParentFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.UserScrubbingFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.bytecode.getStringIndex
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.reference.Reference

@Name("player-controls-bytecode-patch")
@DependsOn([SharedResourceIdPatch::class])
@Description("Manages the code for the player controls of the YouTube player.")
@YouTubeCompatibility
@Version("0.0.1")
class PlayerControlsPatch : BytecodePatch(
    listOf(
        BottomControlsInflateFingerprint,
        ControlsLayoutInflateFingerprint,
        PlayerControlsVisibilityFingerprint,
        PlayerControlsVisibilityModelFingerprint,
        SpeedEduVisibleParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        val playerControlsVisibilityModelClassDef =
            PlayerControlsVisibilityModelFingerprint.result?.classDef
                ?: return PlayerControlsVisibilityModelFingerprint.toErrorResult()

        SeekEDUVisibleFingerprint.resolve(context, playerControlsVisibilityModelClassDef)
        seekEDUVisibleResult =
            SeekEDUVisibleFingerprint.result ?: return SeekEDUVisibleFingerprint.toErrorResult()

        UserScrubbingFingerprint.resolve(context, playerControlsVisibilityModelClassDef)
        userScrubbingResult =
            UserScrubbingFingerprint.result ?: return UserScrubbingFingerprint.toErrorResult()

        playerControlsVisibilityResult = PlayerControlsVisibilityFingerprint.result
            ?: return PlayerControlsVisibilityFingerprint.toErrorResult()
        controlsLayoutInflateResult = ControlsLayoutInflateFingerprint.result
            ?: return ControlsLayoutInflateFingerprint.toErrorResult()
        inflateResult = BottomControlsInflateFingerprint.result
            ?: return BottomControlsInflateFingerprint.toErrorResult()

        SpeedEduVisibleParentFingerprint.result?.let { parentResult ->
            var speedIndex = 0
            parentResult.mutableMethod.apply {
                val targetIndex = getStringIndex(", isSpeedmasterEDUVisible=") + 2
                val targetRegister = getInstruction<Instruction35c>(targetIndex).registerD

                val instructions = implementation!!.instructions
                for ((index, instruction) in instructions.withIndex()) {
                    if (instruction.opcode != Opcode.IGET_BOOLEAN) continue

                    if (getInstruction<TwoRegisterInstruction>(index).registerA == targetRegister) {
                        speedEDUVisibleReference =
                            getInstruction<ReferenceInstruction>(index).reference
                        speedIndex = index
                        break
                    }
                }
                if (speedIndex == 0) return PatchResultError("SpeedEduVisibleParent Instruction not found!")
            }

            SpeedEduVisibleFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.mutableMethod?.let {
                it.implementation!!.instructions.apply {
                    for ((index, instruction) in withIndex()) {
                        if (instruction.opcode != Opcode.IPUT_BOOLEAN) continue

                        if (it.getInstruction<ReferenceInstruction>(index).reference == speedEDUVisibleReference) {
                            speedEDUVisibleMutableMethod = it
                            speedEDUVisibleIndex = index
                            speedEDUVisibleRegister =
                                it.getInstruction<TwoRegisterInstruction>(index).registerA
                            break
                        }
                    }
                }
                if (speedEDUVisibleIndex == 0) return PatchResultError("SpeedEduVisibleFingerprint Instruction not found!")
            } ?: return SpeedEduVisibleFingerprint.toErrorResult()
        } ?: return SpeedEduVisibleParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var controlsLayoutInflateResult: MethodFingerprintResult
        lateinit var inflateResult: MethodFingerprintResult
        lateinit var playerControlsVisibilityResult: MethodFingerprintResult
        lateinit var seekEDUVisibleResult: MethodFingerprintResult
        lateinit var userScrubbingResult: MethodFingerprintResult

        lateinit var speedEDUVisibleMutableMethod: MutableMethod
        lateinit var speedEDUVisibleReference: Reference

        private var speedEDUVisibleRegister: Int = 1
        private var speedEDUVisibleIndex: Int = 0

        private fun injectSpeedEduVisibilityCall(descriptor: String) {
            speedEDUVisibleMutableMethod.addInstruction(
                speedEDUVisibleIndex,
                "invoke-static {v$speedEDUVisibleRegister}, $descriptor->changeVisibilityNegatedImmediate(Z)V"
            )
        }

        private fun MethodFingerprintResult.injectVisibilityCall(
            descriptor: String,
            fieldName: String
        ) {
            mutableMethod.addInstruction(
                0,
                "invoke-static {p1}, $descriptor->$fieldName(Z)V"
            )
        }

        private fun MethodFingerprintResult.injectCalls(
            descriptor: String
        ) {
            mutableMethod.apply {
                val endIndex = scanResult.patternScanResult!!.endIndex
                val viewRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

                addInstruction(
                    endIndex + 1,
                    "invoke-static {v$viewRegister}, $descriptor->initialize(Ljava/lang/Object;)V"
                )
            }
        }

        fun injectVisibility(descriptor: String) {
            playerControlsVisibilityResult.injectVisibilityCall(descriptor, "changeVisibility")
            seekEDUVisibleResult.injectVisibilityCall(
                descriptor,
                "changeVisibilityNegatedImmediate"
            )
            userScrubbingResult.injectVisibilityCall(descriptor, "changeVisibilityNegatedImmediate")
            injectSpeedEduVisibilityCall(descriptor)
        }

        fun initializeSB(descriptor: String) {
            controlsLayoutInflateResult.injectCalls(descriptor)
        }

        fun initializeControl(descriptor: String) {
            inflateResult.injectCalls(descriptor)
        }
    }
}