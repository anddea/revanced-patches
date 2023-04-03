package app.revanced.patches.youtube.misc.playercontrols.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.playercontrols.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

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
        PlayerControlsVisibilityModelFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        val playerControlsVisibilityModelClassDef = PlayerControlsVisibilityModelFingerprint.result?.classDef?: return PlayerControlsVisibilityModelFingerprint.toErrorResult()

        val seekEDUVisibleFingerprint =
            object : MethodFingerprint(returnType = "V", parameters = listOf("Z"), customFingerprint = { it.name == "l" }) {}
        seekEDUVisibleFingerprint.resolve(context, playerControlsVisibilityModelClassDef)
        seekEDUVisibleResult = seekEDUVisibleFingerprint.result?: return seekEDUVisibleFingerprint.toErrorResult()

        val userScrubbingFingerprint =
            object : MethodFingerprint(returnType = "V", parameters = listOf("Z"), customFingerprint = { it.name == "o" }) {}
        userScrubbingFingerprint.resolve(context, playerControlsVisibilityModelClassDef)
        userScrubbingResult = userScrubbingFingerprint.result?: return userScrubbingFingerprint.toErrorResult()

        playerControlsVisibilityResult = PlayerControlsVisibilityFingerprint.result?: return PlayerControlsVisibilityFingerprint.toErrorResult()
        controlsLayoutInflateResult = ControlsLayoutInflateFingerprint.result?: return ControlsLayoutInflateFingerprint.toErrorResult()
        inflateResult = BottomControlsInflateFingerprint.result?: return BottomControlsInflateFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var controlsLayoutInflateResult: MethodFingerprintResult
        lateinit var inflateResult: MethodFingerprintResult
        lateinit var playerControlsVisibilityResult: MethodFingerprintResult
        lateinit var seekEDUVisibleResult: MethodFingerprintResult
        lateinit var userScrubbingResult: MethodFingerprintResult

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
            playerControlsVisibilityResult.injectVisibilityCall(descriptor, "changeVisibility")
            seekEDUVisibleResult.injectVisibilityCall(descriptor, "changeVisibilityNegatedImmediate")
            userScrubbingResult.injectVisibilityCall(descriptor, "changeVisibilityNegatedImmediate")
        }

        fun initializeSB(descriptor: String) {
            controlsLayoutInflateResult.injectCalls(descriptor)
        }

        fun initializeControl(descriptor: String) {
            inflateResult.injectCalls(descriptor)
        }
    }
}