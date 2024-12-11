package app.revanced.patches.youtube.player.hapticfeedback

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_HAPTIC_FEEDBACK
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val hapticFeedbackPatch = bytecodePatch(
    DISABLE_HAPTIC_FEEDBACK.title,
    DISABLE_HAPTIC_FEEDBACK.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        fun Pair<String, Fingerprint>.hookHapticFeedback(methodName: String) =
            methodOrThrow().apply {
                var index = 0
                var register = 0

                if (name == "run") {
                    index = indexOfFirstInstructionOrThrow(Opcode.SGET)
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

        arrayOf(
            seekHapticsFingerprint to "disableSeekVibrate",
            seekUndoHapticsFingerprint to "disableSeekUndoVibrate",
            scrubbingHapticsFingerprint to "disableScrubbingVibrate",
            markerHapticsFingerprint to "disableChapterVibrate",
            zoomHapticsFingerprint to "disableZoomVibrate"
        ).map { (fingerprint, methodName) ->
            fingerprint.hookHapticFeedback(methodName)
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: DISABLE_HAPTIC_FEEDBACK"
            ),
            DISABLE_HAPTIC_FEEDBACK
        )

        // endregion

    }
}
