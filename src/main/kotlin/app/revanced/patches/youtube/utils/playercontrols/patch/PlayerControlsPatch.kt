package app.revanced.patches.youtube.utils.playercontrols.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.BottomControlsInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.ControlsLayoutInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.FullscreenEngagementSpeedEduVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.FullscreenEngagementSpeedEduVisibleParentFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityModelFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.QuickSeekVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.SeekEDUVisibleFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.UserScrubbingFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.bytecode.getStringIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.Reference

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
    override fun execute(context: BytecodeContext) {

        fun MutableMethod.findReference(targetString: String): Reference {
            val targetIndex = getStringIndex(targetString) + 2
            val targetOpcode = getInstruction(targetIndex).opcode

            if (targetOpcode == Opcode.INVOKE_VIRTUAL) {
                val targetRegister = getInstruction<Instruction35c>(targetIndex).registerD

                val instructions = implementation!!.instructions
                for ((index, instruction) in instructions.withIndex()) {
                    if (instruction.opcode != Opcode.IGET_BOOLEAN) continue

                    if (getInstruction<TwoRegisterInstruction>(index).registerA == targetRegister)
                        return getInstruction<ReferenceInstruction>(index).reference
                }
            } else if (targetOpcode == Opcode.IGET_BOOLEAN) {
                return getInstruction<ReferenceInstruction>(targetIndex).reference
            }

            throw PatchException("Reference not found: $targetString")
        }

        PlayerControlsVisibilityModelFingerprint.result?.classDef?.let { classDef ->
            quickSeekVisibleMutableMethod =
                QuickSeekVisibleFingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.mutableMethod ?: throw QuickSeekVisibleFingerprint.exception

            seekEDUVisibleMutableMethod =
                SeekEDUVisibleFingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.mutableMethod ?: throw SeekEDUVisibleFingerprint.exception

            userScrubbingMutableMethod =
                UserScrubbingFingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.mutableMethod ?: throw UserScrubbingFingerprint.exception
        } ?: throw PlayerControlsVisibilityModelFingerprint.exception

        YouTubeControlsOverlayFingerprint.result?.classDef?.let { classDef ->
            playerControlsVisibilityMutableMethod =
                PlayerControlsVisibilityFingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.mutableMethod
                    ?: throw PlayerControlsVisibilityFingerprint.exception
        } ?: throw YouTubeControlsOverlayFingerprint.exception

        controlsLayoutInflateResult =
            ControlsLayoutInflateFingerprint.result
                ?: throw ControlsLayoutInflateFingerprint.exception

        inflateResult =
            BottomControlsInflateFingerprint.result
                ?: throw BottomControlsInflateFingerprint.exception

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
                    ?: throw FullscreenEngagementSpeedEduVisibleFingerprint.exception
        } ?: throw FullscreenEngagementSpeedEduVisibleParentFingerprint.exception

    }

    internal companion object {
        lateinit var controlsLayoutInflateResult: MethodFingerprintResult
        lateinit var inflateResult: MethodFingerprintResult

        lateinit var playerControlsVisibilityMutableMethod: MutableMethod
        lateinit var quickSeekVisibleMutableMethod: MutableMethod
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
            quickSeekVisibleMutableMethod.injectVisibilityCall(
                descriptor,
                "changeVisibilityNegatedImmediate"
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