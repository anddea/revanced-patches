package app.morphe.patches.youtube.utils.pip

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.morphe.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

val pipStateHookPatch = bytecodePatch(
    description = "pipStateHookPatch",
) {
    execute {
        pipPlaybackFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.instructionMatches.last().index
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR->getExternalDownloaderLaunchedState(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }
    }
}
