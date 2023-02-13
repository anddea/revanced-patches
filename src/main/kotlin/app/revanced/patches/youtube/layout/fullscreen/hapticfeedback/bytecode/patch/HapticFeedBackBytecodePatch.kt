package app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.bytecode.fingerprints.*
import app.revanced.util.integrations.Constants.FULLSCREEN_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("disable-haptic-feedback-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HapticFeedBackBytecodePatch : BytecodePatch(
    listOf(
        MarkerHapticsFingerprint,
        SeekHapticsFingerprint,
        ScrubbingHapticsFingerprint,
        ZoomHapticsFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            SeekHapticsFingerprint to "disableSeekVibrate",
            ScrubbingHapticsFingerprint to "disableScrubbingVibrate",
            MarkerHapticsFingerprint to "disableChapterVibrate",
            ZoomHapticsFingerprint to "disableZoomVibrate"
        ).map { (fingerprint, name) ->
            fingerprint.result?.let {
                if (fingerprint == SeekHapticsFingerprint)
                    it.disableHaptics(name)
                else
                    it.voidHaptics(name)
            } ?: return fingerprint.toErrorResult()
        }

        return PatchResultSuccess()
    }

    private companion object {
        fun MethodFingerprintResult.disableHaptics(targetMethodName: String) {
            val startIndex = scanResult.patternScanResult!!.startIndex
            val endIndex = scanResult.patternScanResult!!.endIndex
            val insertIndex = endIndex + 4
            val targetRegister = (method.implementation!!.instructions.elementAt(insertIndex) as OneRegisterInstruction).registerA
            val dummyRegister = targetRegister + 1

            with (mutableMethod) {
                removeInstruction(insertIndex)

                addInstructions(
                    insertIndex, """
                     invoke-static {}, $FULLSCREEN_LAYOUT->$targetMethodName()Z
                     move-result v$dummyRegister
                     if-eqz v$dummyRegister, :vibrate
                     const-wide/16 v$targetRegister, 0x0
                     goto :exit
                     :vibrate
                     const-wide/16 v$targetRegister, 0x19
                """, listOf(ExternalLabel("exit", mutableMethod.instruction(insertIndex)))
                )

                addInstructions(
                    startIndex, """
                     invoke-static {}, $FULLSCREEN_LAYOUT->$targetMethodName()Z
                     move-result v$dummyRegister
                     if-eqz v$dummyRegister, :vibrate
                     return-void
                """, listOf(ExternalLabel("vibrate", mutableMethod.instruction(startIndex)))
                )
            }
        }

        fun MethodFingerprintResult.voidHaptics(targetMethodName: String) {
             mutableMethod.addInstructions(
                 0, """
                     invoke-static {}, $FULLSCREEN_LAYOUT->$targetMethodName()Z
                     move-result v0
                     if-eqz v0, :vibrate
                     return-void
                 """, listOf(ExternalLabel("vibrate", mutableMethod.instruction(0)))
             )
        }
    }
}

