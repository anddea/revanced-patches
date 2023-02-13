package app.revanced.patches.youtube.misc.playercontrols.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.playercontrols.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("player-controls-bytecode-patch")
@DependsOn([SharedResourcdIdPatch::class])
@Description("Manages the code for the player controls of the YouTube player.")
@YouTubeCompatibility
@Version("0.0.1")
class PlayerControlsBytecodePatch : BytecodePatch(
    listOf(
        BottomControlsInflateFingerprint,
        ControlsLayoutInflateFingerprint,
        PlayerControlsVisibilityFingerprint,
        VisibilityNegatedParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PlayerControlsVisibilityFingerprint.result?.let {
            showPlayerControlsResult = it
        } ?: return PlayerControlsVisibilityFingerprint.toErrorResult()

        ControlsLayoutInflateFingerprint.result?.let {
            controlsLayoutInflateResult = it
        } ?: return ControlsLayoutInflateFingerprint.toErrorResult()

        BottomControlsInflateFingerprint.result?.let {
            inflateResult = it
        } ?: return BottomControlsInflateFingerprint.toErrorResult()

        VisibilityNegatedParentFingerprint.result?.let { parentResult ->
            VisibilityNegatedFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                visibilityNegatedResult = it
            } ?: return VisibilityNegatedFingerprint.toErrorResult()
        } ?: return VisibilityNegatedParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var showPlayerControlsResult: MethodFingerprintResult
        lateinit var controlsLayoutInflateResult: MethodFingerprintResult
        lateinit var inflateResult: MethodFingerprintResult
        lateinit var visibilityNegatedResult: MethodFingerprintResult

        fun MethodFingerprintResult.injectVisibilityCall(
            descriptor: String,
            fieldname: String
        ) {
            mutableMethod.addInstruction(
                0,
                "invoke-static {p1}, $descriptor->$fieldname(Z)V"
            )
        }

        private fun MethodFingerprintResult.injectCalls(
            descriptor: String
        ) {
            val endIndex = scanResult.patternScanResult!!.endIndex
            with (mutableMethod) {
                val viewRegister = (instruction(endIndex) as OneRegisterInstruction).registerA
                addInstruction(
                    endIndex + 1,
                    "invoke-static {v$viewRegister}, $descriptor->initialize(Ljava/lang/Object;)V"
                )
            }
        }

        fun injectVisibility(descriptor: String) {
            showPlayerControlsResult.injectVisibilityCall(descriptor, "changeVisibility")
        }

        fun injectVisibilityNegated(descriptor: String) {
            visibilityNegatedResult.injectVisibilityCall(descriptor, "changeVisibilityNegatedImmediate")
        }

        fun initializeSB(descriptor: String) {
            controlsLayoutInflateResult.injectCalls(descriptor)
        }

        fun initializeControl(descriptor: String) {
            inflateResult.injectCalls(descriptor)
        }
    }
}