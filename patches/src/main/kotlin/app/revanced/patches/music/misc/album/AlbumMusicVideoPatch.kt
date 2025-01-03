package app.revanced.patches.music.misc.album

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.dismiss.dismissQueueHookPatch
import app.revanced.patches.music.utils.extension.Constants.MISC_PATH
import app.revanced.patches.music.utils.patch.PatchList.DISABLE_MUSIC_VIDEO_IN_ALBUM
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.music.video.information.videoIdHook
import app.revanced.patches.music.video.information.videoInformationPatch
import app.revanced.patches.music.video.playerresponse.hookPlayerResponse
import app.revanced.patches.music.video.playerresponse.playerResponseMethodHookPatch
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/AlbumMusicVideoPatch;"

@Suppress("unused")
val albumMusicVideoPatch = bytecodePatch(
    DISABLE_MUSIC_VIDEO_IN_ALBUM.title,
    DISABLE_MUSIC_VIDEO_IN_ALBUM.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        dismissQueueHookPatch,
        videoInformationPatch,
        playerResponseMethodHookPatch,
    )

    execute {

        // region hook player response

        hookPlayerResponse("$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponse(Ljava/lang/String;Ljava/lang/String;I)V")

        // endregion

        // region hook video id

        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        // endregion

        // region patch for hide snack bar

        snackBarParentFingerprint.methodOrThrow().addInstructionsWithLabels(
            0, """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideSnackBar()Z
                move-result v0
                if-eqz v0, :hide
                return-void
                :hide
                nop
                """
        )

        // endregion

        // region patch for setOnClick / setOnLongClick listener

        audioVideoSwitchToggleConstructorFingerprint.methodOrThrow().apply {
            val onClickListenerIndex = indexOfAudioVideoSwitchSetOnClickListenerInstruction(this)
            val viewRegister = getInstruction<FiveRegisterInstruction>(onClickListenerIndex).registerC

            addInstruction(
                onClickListenerIndex + 1,
                "invoke-static { v$viewRegister }, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->setAudioVideoSwitchToggleOnLongClickListener(Landroid/view/View;)V"
            )

            val onClickListenerSyntheticIndex = indexOfFirstInstructionReversedOrThrow(onClickListenerIndex) {
                opcode == Opcode.INVOKE_DIRECT &&
                        getReference<MethodReference>()?.name == "<init>"
            }
            val onClickListenerSyntheticClass = (getInstruction<ReferenceInstruction>(onClickListenerSyntheticIndex).reference as MethodReference).definingClass

            findMethodOrThrow(onClickListenerSyntheticClass) {
                name == "onClick"
            }.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->openMusic()Z
                    move-result v0
                    if-eqz v0, :ignore
                    return-void
                    :ignore
                    nop
                    """
            )
        }

        // endregion

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_disable_music_video_in_album",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_disable_music_video_in_album_redirect_type",
            "revanced_disable_music_video_in_album"
        )

        updatePatchStatus(DISABLE_MUSIC_VIDEO_IN_ALBUM)

    }
}
