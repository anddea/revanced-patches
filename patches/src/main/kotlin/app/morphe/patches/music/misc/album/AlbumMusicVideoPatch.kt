/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.music.misc.album

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.MISC_PATH
import app.morphe.patches.music.utils.patch.PatchList.DISABLE_MUSIC_VIDEO_IN_ALBUM
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addListPreference
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.music.video.information.videoIdHook
import app.morphe.patches.music.video.information.videoInformationPatch
import app.morphe.patches.music.video.playerresponse.Hook
import app.morphe.patches.music.video.playerresponse.addPlayerResponseMethodHook
import app.morphe.patches.music.video.playerresponse.playerResponseMethodHookPatch
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
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
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        videoInformationPatch,
        playerResponseMethodHookPatch,
    )

    execute {
        // region hook player response

        addPlayerResponseMethodHook(
            Hook.VideoIdAndPlaylistId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponse(Ljava/lang/String;Ljava/lang/String;I)V"
            ),
        )

        // endregion

        // region hook video id

        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        // endregion

        // region patch for hide snack bar

        snackBarFingerprint
            .methodOrThrow(snackBarAttributeFingerprint)
            .addInstructionsWithLabels(
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
            val viewRegister =
                getInstruction<FiveRegisterInstruction>(onClickListenerIndex).registerC

            addInstruction(
                onClickListenerIndex + 1,
                "invoke-static { v$viewRegister }, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->setAudioVideoSwitchToggleOnLongClickListener(Landroid/view/View;)V"
            )

            val onClickListenerSyntheticIndex =
                indexOfFirstInstructionReversedOrThrow(onClickListenerIndex) {
                    opcode == Opcode.INVOKE_DIRECT &&
                            getReference<MethodReference>()?.name == "<init>"
                }
            val onClickListenerSyntheticClass =
                (getInstruction<ReferenceInstruction>(onClickListenerSyntheticIndex).reference as MethodReference).definingClass

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
        addListPreference(
            CategoryType.MISC,
            "revanced_disable_music_video_in_album_redirect_type",
            "revanced_disable_music_video_in_album"
        )

        updatePatchStatus(DISABLE_MUSIC_VIDEO_IN_ALBUM)

    }
}
