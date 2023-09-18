package app.revanced.patches.music.misc.importexport.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.intenthook.patch.IntentHookPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType

@Patch
@Name("Import/Export settings")
@Description("Import or export settings as text.")
@DependsOn(
    [
        IntentHookPatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
class ImportExportPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_extended_settings_import_export",
            ""
        )

    }
}
