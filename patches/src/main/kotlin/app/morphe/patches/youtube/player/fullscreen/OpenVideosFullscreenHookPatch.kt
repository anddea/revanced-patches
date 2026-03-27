package app.morphe.patches.youtube.player.fullscreen

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.OpcodesFilter.Companion.opcodesToFilters
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.playservice.is_19_46_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/OpenVideosFullscreenHookPatch;"

/**
 * 19.46+
 */
internal object openVideosFullscreenPortraitFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Lj\$/util/Optional;"),
    filters = listOf(
        opcode(Opcode.MOVE_RESULT), // Conditional check to modify.
        literal(45666112L, location = InstructionLocation.MatchAfterWithin(5)),
        opcode(Opcode.MOVE_RESULT, location = InstructionLocation.MatchAfterWithin(10)),
    )
)

/**
 * Pre 19.46.
 */
internal object openVideosFullscreenPortraitLegacyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Lj\$/util/Optional;"),
    filters = opcodesToFilters(
        Opcode.GOTO,
        Opcode.SGET_OBJECT,
        Opcode.GOTO,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQ,
        Opcode.IF_EQ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    )
)

internal val openVideosFullscreenHookPatch = bytecodePatch(
    description = "openVideosFullscreenHookPatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        val fingerprint: Fingerprint
        val insertIndex: Int

        if (is_19_46_or_greater) {
            fingerprint = openVideosFullscreenPortraitFingerprint
            insertIndex = fingerprint.instructionMatches.first().index

            openVideosFullscreenPortraitFingerprint.let {
                // Remove the A/B feature call so the one-shot override can decide portrait fullscreen.
                it.method.insertLiteralOverride(
                    it.instructionMatches.last().index,
                    false
                )
            }
        } else {
            fingerprint = openVideosFullscreenPortraitLegacyFingerprint
            insertIndex = fingerprint.instructionMatches.last().index
        }

        fingerprint.let {
            it.method.apply {
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->doNotOpenVideoFullscreenPortrait(Z)Z
                        move-result v$register
                    """
                )
            }
        }
    }
}
