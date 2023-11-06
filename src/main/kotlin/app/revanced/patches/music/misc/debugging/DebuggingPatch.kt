package app.revanced.patches.music.misc.debugging

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType

@Patch(
    name = "Enable debug logging",
    description = "Adds debugging options.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.15.52",
                "6.20.51",
                "6.25.53",
                "6.26.50"
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