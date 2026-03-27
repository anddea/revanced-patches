package app.morphe.patches.youtube.utils.playlist

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.mainactivity.getMainActivityMethod
import app.morphe.patches.youtube.player.overlaybuttons.geminiButton
import app.morphe.patches.youtube.utils.auth.authHookPatch
import app.morphe.patches.youtube.utils.dismiss.dismissPlayerHookPatch
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlaylistPatch;"

val playlistPatch = bytecodePatch(
    description = "playlistPatch",
) {
    dependsOn(
        geminiButton,

        sharedExtensionPatch,
        mainActivityResolvePatch,
        dismissPlayerHookPatch,
        playerTypeHookPatch,
        videoInformationPatch,
        authHookPatch,
    )

    execute {
        // Open the queue manager by pressing and holding the back button.
        getMainActivityMethod("onKeyLongPress")
            .addInstructionsWithLabels(
                0, """
                    invoke-static/range {p1 .. p1}, $EXTENSION_CLASS_DESCRIPTOR->onKeyLongPress(I)Z
                    move-result v0
                    if-eqz v0, :ignore
                    return v0
                    :ignore
                    nop
                    """
            )

        // Users deleted videos via YouTube's flyout menu.
        val setVideoIdReference = with(playlistEndpointFingerprint.methodOrThrow()) {
            val setVideoIdIndex = indexOfSetVideoIdInstruction(this)
            getInstruction<ReferenceInstruction>(setVideoIdIndex).reference as FieldReference
        }

        editPlaylistFingerprint
            .matchOrThrow(editPlaylistConstructorFingerprint)
            .let {
                it.method.apply {
                    val castIndex = it.instructionMatches.first().index
                    val castClass =
                        getInstruction<ReferenceInstruction>(castIndex).reference.toString()

                    if (castClass != setVideoIdReference.definingClass) {
                        throw PatchException("Method signature parameter did not match: $castClass")
                    }
                    val castRegister = getInstruction<OneRegisterInstruction>(castIndex).registerA
                    val insertIndex = castIndex + 1
                    val insertRegister =
                        getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            iget-object v$insertRegister, v$castRegister, $setVideoIdReference
                            invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->removeFromQueue(Ljava/lang/String;)V
                            """
                    )
                }
            }
    }
}
