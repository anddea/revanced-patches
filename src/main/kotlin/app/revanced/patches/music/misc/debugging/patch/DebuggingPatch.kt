package app.revanced.patches.music.misc.debugging.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType

@Patch(false)
@Name("Enable debug logging")
@Description("Adds debugging options.")
@DependsOn([SettingsPatch::class])
@MusicCompatibility
class DebuggingPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.MISC,
            "revanced_enable_debug_logging",
            "false"
        )

    }
}