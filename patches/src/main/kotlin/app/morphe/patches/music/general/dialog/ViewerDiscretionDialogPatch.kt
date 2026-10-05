package app.morphe.patches.music.general.dialog

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.patch.PatchList.REMOVE_VIEWER_DISCRETION_DIALOG
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.dialog.baseViewerDiscretionDialogPatch

@Suppress("unused")
val viewerDiscretionDialogPatch = bytecodePatch(
    REMOVE_VIEWER_DISCRETION_DIALOG.title,
    REMOVE_VIEWER_DISCRETION_DIALOG.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(
        // modified by lavinhoque33, 2026-10-04
        // Tell the shared patch it runs on Music (YouTube's is_20_21_or_greater is uninitialized here).
        baseViewerDiscretionDialogPatch(GENERAL_CLASS_DESCRIPTOR, isYouTubeMusic = true),
        settingsPatch,
    )

    execute {
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_remove_viewer_discretion_dialog",
            "false"
        )

        updatePatchStatus(REMOVE_VIEWER_DISCRETION_DIALOG)

    }
}
