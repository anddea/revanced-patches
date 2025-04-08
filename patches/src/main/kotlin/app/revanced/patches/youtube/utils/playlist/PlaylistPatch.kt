package app.revanced.patches.youtube.utils.playlist

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.mainactivity.getMainActivityMethod
import app.revanced.patches.youtube.utils.auth.authHookPatch
import app.revanced.patches.youtube.utils.dismiss.dismissPlayerHookPatch
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
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

        setPivotBarVisibilityFingerprint
            .matchOrThrow(setPivotBarVisibilityParentFingerprint)
            .let {
                it.method.apply {
                    val viewIndex = it.patternMatch!!.startIndex
                    val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA
                    addInstruction(
                        viewIndex + 1,
                        "invoke-static {v$viewRegister}," +
                                " $EXTENSION_CLASS_DESCRIPTOR->setPivotBar(Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;)V",
                    )
                }
            }

        // Users deleted videos via YouTube's flyout menu.
        val setVideoIdReference = with(playlistEndpointFingerprint.methodOrThrow()) {
            val setVideoIdIndex = indexOfSetVideoIdInstruction(this)
            getInstruction<ReferenceInstruction>(setVideoIdIndex).reference as FieldReference
        }

        editPlaylistFingerprint
            .matchOrThrow(editPlaylistConstructorFingerprint)
            .let {
                it.method.apply {
                    val castIndex = it.patternMatch!!.startIndex
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
