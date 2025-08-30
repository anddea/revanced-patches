package app.revanced.patches.shared.spoof.blockrequest

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

fun baseBlockRequestPatch(
    classDescriptor: String,
) = bytecodePatch(
    description = "baseBlockRequestPatch"
) {
    execute {

        // region Block /initplayback requests to fall back to /get_watch requests.

        buildInitPlaybackRequestFingerprint.methodOrThrow().apply {
            val index = indexOfUriToStringInstruction(this) + 1
            val register =
                getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                    invoke-static { v$register }, $classDescriptor->blockInitPlaybackRequest(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
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
                invokeToStringIndex, """
                    invoke-static { v$uriRegister }, $classDescriptor->blockGetWatchRequest(Landroid/net/Uri;)Landroid/net/Uri;
                    move-result-object v$uriRegister
                    """,
            )
        }

        // endregion

        // region Remove /videoplayback request body to fix playback.

        buildMediaDataSourceFingerprint.methodOrThrow().apply {
            val targetIndex = instructions.lastIndex

            addInstructions(
                targetIndex,
                """
                    # Field a: Stream uri.
                    # Field c: Http method.
                    # Field d: Post data.
                    move-object/from16 v0, p0
                    iget-object v1, v0, $definingClass->a:Landroid/net/Uri;
                    iget v2, v0, $definingClass->c:I
                    iget-object v3, v0, $definingClass->d:[B
                    invoke-static { v1, v2, v3 }, $classDescriptor->removeVideoPlaybackPostBody(Landroid/net/Uri;I[B)[B
                    move-result-object v1
                    iput-object v1, v0, $definingClass->d:[B
                    """,
            )
        }

        // endregion
    }
}

