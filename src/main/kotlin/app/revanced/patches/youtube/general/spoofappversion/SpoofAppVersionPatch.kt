package app.revanced.patches.youtube.general.spoofappversion

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addEntryValues
import app.revanced.patches.youtube.utils.settings.SettingsPatch
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
    private const val ATTRIBUTE_NAME_ENTRIES =
        "revanced_spoof_app_version_target_entries"

    private const val ATTRIBUTE_NAME_ENTRY_VALUE =
        "revanced_spoof_app_version_target_entry_values"

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

    private fun ResourceContext.appendAppVersion(appVersion: String) {
        addEntryValues(
            ATTRIBUTE_NAME_ENTRIES,
            "@string/revanced_spoof_app_version_target_entry_" + appVersion.replace(".", "_"),
            prepend = false
        )
        addEntryValues(
            ATTRIBUTE_NAME_ENTRY_VALUE,
            appVersion,
            prepend = false
        )
    }
}