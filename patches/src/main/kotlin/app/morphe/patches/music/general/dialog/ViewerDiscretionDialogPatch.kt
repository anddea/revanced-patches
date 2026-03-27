package app.morphe.patches.music.general.dialog

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
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
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseViewerDiscretionDialogPatch(GENERAL_CLASS_DESCRIPTOR),
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
