package app.revanced.patches.music.misc.debugging

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object DebuggingPatch : BaseResourcePatch(
    name = "Enable debug logging",
    description = "Adds an option to enable debug logging.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_enable_debug_logging",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_enable_debug_buffer_logging",
            "false",
            "revanced_enable_debug_logging"
        )

    }
}