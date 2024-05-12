package app.revanced.patches.music.general.spoofappversion

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.general.oldstylelibraryshelf.OldStyleLibraryShelfPatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object SpoofAppVersionPatch : BaseResourcePatch(
    name = "Spoof app version",
    description = "Adds options to spoof the YouTube Music client version. " +
            "This can remove the radio mode restriction in Canadian regions or disable real-time lyrics.",
    dependencies = setOf(
        OldStyleLibraryShelfPatch::class,
        SettingsPatch::class,
        SpoofAppVersionBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_spoof_app_version",
            "false"
        )
        SettingsPatch.addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_spoof_app_version_target",
            "revanced_spoof_app_version"
        )

    }
}