package app.revanced.patches.youtube.misc.playercontrols.bytecode.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.playercontrols.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
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
        showPlayerControlsFingerprintResult = PlayerControlsVisibilityFingerprint.result!!
        controlsLayoutInflateFingerprintResult = ControlsLayoutInflateFingerprint.result!!

        // TODO: another solution is required, this is hacky
        listOf(BottomControlsInflateFingerprint).resolve(context, context.classes)
        inflateFingerprintResult = BottomControlsInflateFingerprint.result!!

        VisibilityNegatedFingerprint.resolve(context, VisibilityNegatedParentFingerprint.result!!.classDef)
        visibilityNegatedFingerprintResult = VisibilityNegatedFingerprint.result!!

        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var showPlayerControlsFingerprintResult: MethodFingerprintResult
        lateinit var controlsLayoutInflateFingerprintResult: MethodFingerprintResult
        lateinit var inflateFingerprintResult: MethodFingerprintResult
        lateinit var visibilityNegatedFingerprintResult: MethodFingerprintResult

        fun MethodFingerprintResult.injectVisibilityCall(
            descriptor: String,
            fieldname: String
        ) {
            mutableMethod.addInstruction(
                0,
                "invoke-static {p1}, $descriptor->$fieldname(Z)V"
            )
        }

        fun MethodFingerprintResult.injectCalls(
            descriptor: String
        ) {
            val endIndex = scanResult.patternScanResult!!.endIndex
            val viewRegister = (mutableMethod.instruction(endIndex) as OneRegisterInstruction).registerA

            mutableMethod.addInstruction(
                endIndex + 1,
                "invoke-static {v$viewRegister}, $descriptor->initialize(Ljava/lang/Object;)V"
            )
        }

        fun injectVisibility(descriptor: String) {
            showPlayerControlsFingerprintResult.injectVisibilityCall(descriptor, "changeVisibility")
        }

        fun injectVisibilityNegated(descriptor: String) {
            visibilityNegatedFingerprintResult.injectVisibilityCall(descriptor, "changeVisibilityNegatedImmediate")
        }

        fun initializeSB(descriptor: String) {
            controlsLayoutInflateFingerprintResult.injectCalls(descriptor)
        }

        fun initializeControl(descriptor: String) {
            inflateFingerprintResult.injectCalls(descriptor)
        }
    }
}