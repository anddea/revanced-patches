package app.morphe.patches.music.utils.playertype

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.extension.Constants.UTILS_PATH
import app.morphe.util.fingerprint.methodOrThrow

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerTypeHookPatch;"

@Suppress("unused")
val playerTypeHookPatch = bytecodePatch(
    description = "playerTypeHookPatch"
) {

    execute {

        playerTypeFingerprint.methodOrThrow().addInstruction(
            0,
            "invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V"
        )

    }
}
