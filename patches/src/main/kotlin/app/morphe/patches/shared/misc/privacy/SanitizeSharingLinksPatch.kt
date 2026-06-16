package app.morphe.patches.shared.misc.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS =
    "$PATCHES_PATH/SanitizeSharingLinksPatch;"

internal fun sanitizeSharingLinksPatch(
    name: String,
    description: String,
    block: BytecodePatchBuilder.() -> Unit = {},
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    name,
    description,
) {
    block()

    execute {
        executeBlock()

        fun Fingerprint.hookUrlString(matchIndex: Int) {
            val index = instructionMatches[matchIndex].index
            val urlRegister = method.getInstruction<OneRegisterInstruction>(index).registerA

            method.addInstructions(
                index + 1,
                """
                    invoke-static { v$urlRegister }, $EXTENSION_CLASS->sanitize(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$urlRegister
                """
            )
        }

        fun Fingerprint.hookIntentPutExtra(matchIndex: Int) {
            val index = instructionMatches[matchIndex].index

            method.hookQueryParameters(index)
        }

        // YouTube share sheet copy link.
        YouTubeCopyTextFingerprint.hookUrlString(0)

        // YouTube share sheet other apps.
        YouTubeShareSheetFingerprint.hookIntentPutExtra(3)

        // Native system share sheet.
        YouTubeSystemShareSheetFingerprint.hookIntentPutExtra(3)
    }
}

internal fun MutableMethod.hookQueryParameters(index: Int) {
    val urlRegister = getInstruction<FiveRegisterInstruction>(index).registerE

    addInstructionsAtControlFlowLabel(
        index,
        """
            invoke-static { v$urlRegister }, $EXTENSION_CLASS->sanitize(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$urlRegister
        """
    )
}
