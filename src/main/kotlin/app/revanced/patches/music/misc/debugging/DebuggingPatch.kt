package app.revanced.patches.music.misc.debugging

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch

@Patch(
    name = "Enable debug logging",
    description = "Adds an option to enable debug logging.",
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
    ],
    use = false
)
@Suppress("unused")
object DebuggingPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.MISC,
            "revanced_enable_debug_logging",
            "false"
        )

    }
}