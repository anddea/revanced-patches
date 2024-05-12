package app.revanced.patches.youtube.player.hapticfeedback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.MarkerHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.ScrubbingHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.SeekHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.SeekUndoHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.ZoomHapticsFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object HapticFeedBackPatch : BaseBytecodePatch(
    name = "Disable haptic feedback",
    description = "Adds an option to disable haptic feedback when swiping the video player.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        MarkerHapticsFingerprint,
        SeekHapticsFingerprint,
        SeekUndoHapticsFingerprint,
        ScrubbingHapticsFingerprint,
        ZoomHapticsFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            SeekHapticsFingerprint to "disableSeekVibrate",
            SeekUndoHapticsFingerprint to "disableSeekUndoVibrate",
            ScrubbingHapticsFingerprint to "disableScrubbingVibrate",
            MarkerHapticsFingerprint to "disableChapterVibrate",
            ZoomHapticsFingerprint to "disableZoomVibrate"
        ).map { (fingerprint, methodName) ->
            fingerprint.injectHook(methodName)
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: DISABLE_HAPTIC_FEEDBACK"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }

    private fun MethodFingerprint.injectHook(methodName: String) {
        resultOrThrow().let {
            it.mutableMethod.apply {
                var index = 0
                var register = 0

                if (name == "run") {
                    index = getTargetIndex(Opcode.SGET)
                    register = getInstruction<OneRegisterInstruction>(index).registerA
                }

                addInstructionsWithLabels(
                    index, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->$methodName()Z
                        move-result v$register
                        if-eqz v$register, :vibrate
                        return-void
                        """, ExternalLabel("vibrate", getInstruction(index))
                )
            }
        }
    }
}

