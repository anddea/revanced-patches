package app.revanced.patches.youtube.general.spoofappversion

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.appendAppVersion
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object SpoofAppVersionPatch : BaseResourcePatch(
    name = "Spoof app version",
    description = "Adds options to spoof the YouTube client version. " +
            "This can be used to restore old UI elements and features.",
    dependencies = setOf(
        SettingsPatch::class,
        SpoofAppVersionBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        if (SettingsPatch.upward1834) {
            context.appendAppVersion("18.33.40")
            if (SettingsPatch.upward1839) {
                context.appendAppVersion("18.38.45")
                if (SettingsPatch.upward1849) {
                    context.appendAppVersion("18.48.39")
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_APP_VERSION"
            )
        )
        SettingsPatch.updatePatchStatus(this)
    }
}