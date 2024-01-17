package app.revanced.patches.music.general.dialog

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.integrations.Constants.GENERAL
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.patch.dialog.AbstractRemoveViewerDiscretionDialogPatch

@Patch(
    name = "Remove viewer discretion dialog",
    description = "Adds an option to remove the dialog that appears when opening a video that has been age-restricted " +
            "by accepting it automatically. This does not bypass the age restriction.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object RemoveViewerDiscretionDialogPatch : AbstractRemoveViewerDiscretionDialogPatch(
    GENERAL
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_remove_viewer_discretion_dialog",
            "false"
        )
    }
}
