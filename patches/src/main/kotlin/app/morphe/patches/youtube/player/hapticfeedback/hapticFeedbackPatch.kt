package app.morphe.patches.youtube.player.hapticfeedback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.checkCast
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.all.misc.transformation.IMethodCall
import app.morphe.patches.all.misc.transformation.filterMapInstruction35c
import app.morphe.patches.all.misc.transformation.transformInstructionsPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_HAPTIC_FEEDBACK
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val PLAYER_CLASS_DESCRIPTOR_PREFIX = "$PLAYER_PATH/PlayerPatch"

@Suppress("unused")
val hapticFeedbackPatch = bytecodePatch(
    DISABLE_HAPTIC_FEEDBACK.title,
    DISABLE_HAPTIC_FEEDBACK.summary,
) {
    dependsOn(
        settingsPatch,
        transformInstructionsPatch(
            filterMap = { classDef, _, instruction, instructionIndex ->
                filterMapInstruction35c<MethodCall>(
                    PLAYER_CLASS_DESCRIPTOR_PREFIX,
                    classDef,
                    instruction,
                    instructionIndex,
                )
            },
            transform = { method, entry ->
                val (methodType, instruction, instructionIndex) = entry
                methodType.replaceInvokeVirtualWithExtension(
                    PLAYER_CLASS_DESCRIPTOR,
                    method,
                    instruction,
                    instructionIndex,
                )
            },
        ),
    )

    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        arrayOf(
            MarkerHapticsFingerprint to "disableChapterVibrate",
            ScrubbingHapticsFingerprint to "disablePreciseSeekingVibrate",
            SeekUndoHapticsFingerprint to "disableSeekUndoVibrate",
            ZoomHapticsFingerprint to "disableZoomVibrate"
        ).forEach { (fingerprint, methodName) ->
            fingerprint.method.addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->$methodName()Z
                    move-result v0
                    if-eqz v0, :vibrate
                    return-void
                    :vibrate
                    nop
                """
            )
        }

        val vibratorField = TapAndHoldHapticsHandlerFingerprint.match()
            .instructionMatches.last().instruction.getReference<FieldReference>()!!

        val tapAndHoldHapticsFingerprint = Fingerprint(
            accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
            returnType = "V",
            parameters = listOf(),
            filters = listOf(
                fieldAccess(
                    opcode = Opcode.IGET_OBJECT,
                    reference = vibratorField,
                ),
                checkCast("Landroid/os/Vibrator;"),
                string("Failed to easy seek haptics vibrate.")
            ),
            custom = { method, _ ->
                method.name == "run"
            }
        )

        tapAndHoldHapticsFingerprint.let {
            // clearMatch() is used because it can be the same method as [TapAndHoldSpeedFingerprint].
            it.clearMatch()
            it.method.apply {
                val index = it.instructionMatches.first().index
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                        invoke-static { v$register }, $PLAYER_CLASS_DESCRIPTOR->disableTapAndHoldVibrate(Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$register
                    """
                )
            }
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

// Information about method calls we want to replace
@Suppress("unused")
private enum class MethodCall(
    override val definedClassName: String,
    override val methodName: String,
    override val methodParams: Array<String>,
    override val returnType: String,
) : IMethodCall {
    VibrationEffect(
        "Landroid/os/Vibrator;",
        "vibrate",
        arrayOf("Landroid/os/VibrationEffect;"),
        "V",
    ),
    VibrationMilliseconds(
        "Landroid/os/Vibrator;",
        "vibrate",
        arrayOf("J"),
        "V",
    ),
}
