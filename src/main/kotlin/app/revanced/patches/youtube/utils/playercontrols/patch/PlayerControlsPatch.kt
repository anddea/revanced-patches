package app.revanced.patches.youtube.utils.playercontrols.patch

import app.revanced.extensions.toErrorResult
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
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.BottomControlsInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.ControlsLayoutInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.FullscreenEngagementSpeedEduVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.FullscreenEngagementSpeedEduVisibleParentFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityModelFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.SeekEDUVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.UserScrubbingFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.bytecode.getStringIndex
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.reference.Reference

@DependsOn([SharedResourceIdPatch::class])
class PlayerControlsPatch : BytecodePatch(
    listOf(
        BottomControlsInflateFingerprint,
        ControlsLayoutInflateFingerprint,
        FullscreenEngagementSpeedEduVisibleParentFingerprint,
        PlayerControlsVisibilityModelFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        fun MutableMethod.findReference(targetString: String): Reference {
            val targetIndex = getStringIndex(targetString) + 2
            val targetRegister = getInstruction<Instruction35c>(targetIndex).registerD

            val instructions = implementation!!.instructions
            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode != Opcode.IGET_BOOLEAN) continue

                if (getInstruction<TwoRegisterInstruction>(index).registerA == targetRegister)
                    return getInstruction<ReferenceInstruction>(index).reference
            }
            throw PatchResultError("Reference not found: $targetString")
        }

        PlayerControlsVisibilityModelFingerprint.result?.classDef?.let { classDef ->
            seekEDUVisibleMutableMethod =
                SeekEDUVisibleFingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.mutableMethod ?: return SeekEDUVisibleFingerprint.toErrorResult()

            userScrubbingMutableMethod =
                UserScrubbingFingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.mutableMethod ?: return UserScrubbingFingerprint.toErrorResult()
        } ?: return PlayerControlsVisibilityModelFingerprint.toErrorResult()

        YouTubeControlsOverlayFingerprint.result?.classDef?.let { classDef ->
            playerControlsVisibilityMutableMethod =
                PlayerControlsVisibilityFingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.mutableMethod
                    ?: return PlayerControlsVisibilityFingerprint.toErrorResult()
        } ?: return YouTubeControlsOverlayFingerprint.toErrorResult()

        controlsLayoutInflateResult =
            ControlsLayoutInflateFingerprint.result
                ?: return ControlsLayoutInflateFingerprint.toErrorResult()

        inflateResult =
            BottomControlsInflateFingerprint.result
                ?: return BottomControlsInflateFingerprint.toErrorResult()

        FullscreenEngagementSpeedEduVisibleParentFingerprint.result?.let { parentResult ->
            parentResult.mutableMethod.apply {
                fullscreenEngagementViewVisibleReference =
                    findReference(", isFullscreenEngagementViewVisible=")
                speedEDUVisibleReference = findReference(", isSpeedmasterEDUVisible=")
            }

            fullscreenEngagementSpeedEduVisibleMutableMethod =
                FullscreenEngagementSpeedEduVisibleFingerprint.also {
                    it.resolve(
                        context,
                        parentResult.classDef
                    )
                }.result?.mutableMethod
                    ?: return FullscreenEngagementSpeedEduVisibleFingerprint.toErrorResult()
        } ?: return FullscreenEngagementSpeedEduVisibleParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var controlsLayoutInflateResult: MethodFingerprintResult
        lateinit var inflateResult: MethodFingerprintResult

        lateinit var playerControlsVisibilityMutableMethod: MutableMethod
        lateinit var seekEDUVisibleMutableMethod: MutableMethod
        lateinit var userScrubbingMutableMethod: MutableMethod

        lateinit var fullscreenEngagementSpeedEduVisibleMutableMethod: MutableMethod
        lateinit var fullscreenEngagementViewVisibleReference: Reference
        lateinit var speedEDUVisibleReference: Reference

        private fun injectFullscreenEngagementSpeedEduViewVisibilityCall(
            reference: Reference,
            descriptor: String
        ) {
            fullscreenEngagementSpeedEduVisibleMutableMethod.apply {
                for ((index, instruction) in implementation!!.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.IPUT_BOOLEAN) continue
                    if (getInstruction<ReferenceInstruction>(index).reference != reference) continue

                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    addInstruction(
                        index,
                        "invoke-static {v$register}, $descriptor->changeVisibilityNegatedImmediate(Z)V"
                    )
                    break
                }
            }
        }

        private fun MutableMethod.injectVisibilityCall(
            descriptor: String,
            fieldName: String
        ) {
            addInstruction(
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
            playerControlsVisibilityMutableMethod.injectVisibilityCall(
                descriptor,
                "changeVisibility"
            )
            seekEDUVisibleMutableMethod.injectVisibilityCall(
                descriptor,
                "changeVisibilityNegatedImmediate"
            )
            userScrubbingMutableMethod.injectVisibilityCall(
                descriptor,
                "changeVisibilityNegatedImmediate"
            )

            injectFullscreenEngagementSpeedEduViewVisibilityCall(
                fullscreenEngagementViewVisibleReference,
                descriptor
            )
            injectFullscreenEngagementSpeedEduViewVisibilityCall(
                speedEDUVisibleReference,
                descriptor
            )
        }

        fun initializeSB(descriptor: String) {
            controlsLayoutInflateResult.injectCalls(descriptor)
        }

        fun initializeControl(descriptor: String) {
            inflateResult.injectCalls(descriptor)
        }
    }
}