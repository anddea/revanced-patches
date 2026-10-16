/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3384
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.originaltitles

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.litho.context.conversionContextPatch
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.litho.relayout.lithoRelayoutPatch
import app.morphe.patches.shared.mapping.resourceMappingPatch
import app.morphe.patches.shared.textcomponent.hookTextComponent
import app.morphe.patches.shared.textcomponent.textComponentPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.RESTORE_ORIGINAL_TITLES
import app.morphe.patches.youtube.utils.proto.elementProtoParserHookPatch
import app.morphe.patches.youtube.utils.proto.hookElement
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.videoid.hookVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import app.morphe.patches.shared.misc.media.MediaSessionSetMetadataFingerprint
import app.morphe.util.findFreeRegister
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/originaltitles/RestoreOriginalTitlesPatch;"

@Suppress("unused")
val restoreOriginalTitlesPatch = bytecodePatch(
    name = "Restore original titles",
    description = "Adds an option to show the original video titles and descriptions instead of the auto-translated ones.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        lithoFilterPatch,
        conversionContextPatch,
        resourceMappingPatch,
        elementProtoParserHookPatch,
        textComponentPatch,
        lithoRelayoutPatch,
        videoIdPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPreference(
            arrayOf("PREFERENCE_SCREEN: FEED", "SETTINGS: RESTORE_ORIGINAL_TITLES"),
            RESTORE_ORIGINAL_TITLES,
        )

        // Elements are parsed on the main thread, where the original title cannot be fetched.
        // The element hook finds the translated title and starts fetching the original title,
        // and the text hook replaces the title text when the element is laid out. Titles not yet
        // fetched are shown as loading, and the Litho views are laid out again when fetched.
        hookElement("$EXTENSION_CLASS->restoreOriginalTitle([B)[B")
        hookTextComponent(EXTENSION_CLASS)

        // The original description is fetched when the video is opened,
        // and replaced when the description panel is opened.
        hookVideoId("$EXTENSION_CLASS->newVideoLoaded(Ljava/lang/String;)V")

        // The title of the media notification.
        MediaSessionSetMetadataFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.first().index
                val instruction = getInstruction<FiveRegisterInstruction>(index)
                val sessionRegister = instruction.registerC
                val metadataRegister = instruction.registerD

                addInstructions(
                    index,
                    """
                        invoke-static { v$sessionRegister, v$metadataRegister }, $EXTENSION_CLASS->restoreMediaMetadataTitle(Landroid/media/session/MediaSession;Landroid/media/MediaMetadata;)Landroid/media/MediaMetadata;
                        move-result-object v$metadataRegister
                    """
                )
            }
        }

        // The playlist panel on the watch page does not use Litho.
        PlaylistPanelVideoBindFingerprint.matchAll().forEach {
            it.method.apply {
                val titleViewField = it.instructionMatches.first().getFieldAccessed()
                // Video id is stored in a field of the view holder.
                val videoIdIndex = it.instructionMatches.last().index
                val insertIndex = videoIdIndex + 1
                val videoIdInstruction = getInstruction<TwoRegisterInstruction>(videoIdIndex)
                val videoIdRegister = videoIdInstruction.registerA
                val viewHolderRegister = videoIdInstruction.registerB
                val titleViewRegister = findFreeRegister(insertIndex, videoIdRegister, viewHolderRegister)

                addInstructions(
                    insertIndex,
                    """
                        iget-object v$titleViewRegister, v$viewHolderRegister, $titleViewField
                        invoke-static { v$titleViewRegister, v$videoIdRegister }, $EXTENSION_CLASS->restoreOriginalTitle(Landroid/widget/TextView;Ljava/lang/String;)V
                    """
                )
            }
        }

        // The video information shown while the video is loading shows the title of the element that opened the video,
        // which can be marked with the video id.
        LoadingVideoInformationBindFingerprint.matchOrNull()?.let {
            it.method.apply {
                val index = it.instructionMatches[4].index
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_CLASS->restoreKnownTitles(Landroid/widget/TextView;)V"
                )
            }
        }

        // The fullscreen engagement overlay shows the title of the opened video without the video id.
        FullscreenEngagementOverlayFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.last().index
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_CLASS->restorePlayerTitle(Landroid/widget/TextView;)V"
                )
            }
        }

        // The next video of the collapsed playlist panel does not include the video id.
        NextVideoTitleViewFingerprint.matchAll().forEach {
            it.method.apply {
                val index = it.instructionMatches.last().index
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_CLASS->restoreKnownTitles(Landroid/widget/TextView;)V"
                )
            }
        }
    }
}
