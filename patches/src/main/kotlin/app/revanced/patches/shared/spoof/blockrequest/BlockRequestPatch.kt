package app.revanced.patches.shared.spoof.blockrequest

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.extension.Constants.SPOOF_PATH
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/BlockRequestPatch;"

val blockRequestPatch = bytecodePatch(
    description = "blockRequestPatch"
) {
    execute {
        // region Block /initplayback requests to fall back to /get_watch requests.

        buildInitPlaybackRequestFingerprint.methodOrThrow().apply {
            val uriIndex = indexOfUriToStringInstruction(this)
            val uriRegister =
                getInstruction<FiveRegisterInstruction>(uriIndex).registerC

            addInstructions(
                uriIndex,
                """
                    invoke-static { v$uriRegister }, $EXTENSION_CLASS_DESCRIPTOR->blockInitPlaybackRequest(Landroid/net/Uri;)Landroid/net/Uri;
                    move-result-object v$uriRegister
                    """,
            )
        }

        // endregion

        // region Block /get_watch requests to fall back to /player requests.

        buildPlayerRequestURIFingerprint.methodOrThrow().apply {
            val invokeToStringIndex = indexOfUriToStringInstruction(this)
            val uriRegister =
                getInstruction<FiveRegisterInstruction>(invokeToStringIndex).registerC

            addInstructions(
                invokeToStringIndex,
                """
                    invoke-static { v$uriRegister }, $EXTENSION_CLASS_DESCRIPTOR->blockGetWatchRequest(Landroid/net/Uri;)Landroid/net/Uri;
                    move-result-object v$uriRegister
                    """,
            )
        }

        // endregion
    }
}