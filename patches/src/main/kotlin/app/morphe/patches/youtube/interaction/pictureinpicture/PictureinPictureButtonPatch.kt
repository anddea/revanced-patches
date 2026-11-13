/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3397
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.interaction.pictureinpicture

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.general.downloads.downloadActionsPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.PICTURE_IN_PICTURE_BUTTON
import app.morphe.patches.youtube.utils.playercontrols.addTopControl
import app.morphe.patches.youtube.utils.playercontrols.injectControl
import app.morphe.patches.youtube.utils.playercontrols.playerControlsPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.setExtensionIsPatchIncluded

private val pictureinPictureButtonResourcePatch = resourcePatch {
    dependsOn(
        settingsPatch,
        playerControlsPatch,
    )

    execute {
        copyResources(
            "youtube/pictureinpicturebutton",
            ResourceGroup(
                "drawable",
                "morphe_pip_button.xml",
                "morphe_pip_button_bold.xml",
            ),
        )

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "PREFERENCE_SCREENS: FLYOUT_MENU",
                "SETTINGS: PICTURE_IN_PICTURE_OVERLAY_BUTTON",
                "SETTINGS: PICTURE_IN_PICTURE_FLYOUT_BUTTON",
            ),
            PICTURE_IN_PICTURE_BUTTON,
        )
    }
}

private const val EXTENSION_BUTTON = "Lapp/morphe/extension/youtube/videoplayer/PictureinPictureButton;"
private const val EXTENSION_CLASS = "Lapp/morphe/extension/youtube/patches/PictureinPictureButtonPatch;"

@Suppress("unused")
val pictureinPictureButtonPatch = bytecodePatch(
    PICTURE_IN_PICTURE_BUTTON.title,
    PICTURE_IN_PICTURE_BUTTON.summary,
) {
    dependsOn(
        pictureinPictureButtonResourcePatch,
        sharedExtensionPatch,
        settingsPatch,
        playerControlsPatch,
        downloadActionsPatch,
        bytecodePatch {
            finalize {
                addTopControl(
                    "youtube/pictureinpicturebutton",
                    "@+id/morphe_pip_button",
                    "@+id/morphe_pip_button",
                )
            }
        },
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        injectControl(EXTENSION_BUTTON)

        setExtensionIsPatchIncluded(EXTENSION_CLASS)
    }
}
