/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3966
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.interaction.postimages

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.general.downloads.downloadActionsPatch
import app.morphe.patches.youtube.interaction.feedactions.FeedComponentHostTouchFingerprint
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.feedcards.feedVideoCardsHookPatch
import app.morphe.patches.youtube.utils.patch.PatchList.DOWNLOAD_POST_IMAGES
import app.morphe.patches.youtube.utils.proto.elementProtoParserHookPatch
import app.morphe.patches.youtube.utils.proto.hookElement
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.setExtensionIsPatchIncluded

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/PostImageDownloadPatch;"

@Suppress("unused")
val postImageDownloadPatch = bytecodePatch(
    name = "Download post images",
    description = "Adds a button and a menu item to download the images and GIFs of posts.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        elementProtoParserHookPatch,
        // Adds the item to the menu of the post.
        downloadActionsPatch,
        // Finds the root host of each post when it is laid out.
        feedVideoCardsHookPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPreference(
            arrayOf("PREFERENCE_SCREEN: FEED", "SETTINGS: POST_IMAGE_DOWNLOAD"),
            DOWNLOAD_POST_IMAGES,
        )

        // The element of a post includes the urls of its images and the texts it shows.
        hookElement("$EXTENSION_CLASS->onElementLoaded([B)[B")

        // The download button is drawn on the overlay of the root host of the post,
        // so the touches of the root host decide if it was tapped.
        FeedComponentHostTouchFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p0, p1 }, $EXTENSION_CLASS->onComponentHostTouch(Lcom/facebook/litho/ComponentHost;Landroid/view/MotionEvent;)Z
                move-result v0
                if-eqz v0, :original
                return v0
                :original
                nop
            """
        )

        setExtensionIsPatchIncluded(EXTENSION_CLASS)
    }
}
