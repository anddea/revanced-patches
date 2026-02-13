package app.morphe.patches.youtube.player.hapticfeedback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_HAPTIC_FEEDBACK
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val hapticFeedbackPatch = bytecodePatch(
    DISABLE_HAPTIC_FEEDBACK.title,
    DISABLE_HAPTIC_FEEDBACK.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        fun Pair<String, Fingerprint>.hookHapticFeedback(methodName: String) =
            matchOrThrow().let {
                it.method.apply {
                    var index = 0
                    var register = 0

                    if (name == "run") {
                        val stringIndex = it.stringMatches!!.first().index
                        index = indexOfFirstInstructionReversedOrThrow(stringIndex) {
                            opcode == Opcode.SGET &&
                                    getReference<FieldReference>()?.toString() == "Landroid/os/Build${'$'}VERSION;->SDK_INT:I"
                        }
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
