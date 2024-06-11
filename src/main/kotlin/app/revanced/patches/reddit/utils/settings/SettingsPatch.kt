package app.revanced.patches.reddit.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.integrations.IntegrationsPatch
import app.revanced.util.patch.BaseResourcePatch
import kotlin.io.path.exists

@Suppress("DEPRECATION")
object SettingsPatch : BaseResourcePatch(
    name = "Settings",
    description = "Adds ReVanced Extended settings to Reddit.",
    dependencies = setOf(
        IntegrationsPatch::class,
        SettingsBytecodePatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    requiresIntegrations = true
) {
    private const val DEFAULT_NAME = "ReVanced Extended"

    private val RVXSettingsMenuName by stringPatchOption(
        key = "RVXSettingsMenuName",
        default = DEFAULT_NAME,
        title = "RVX settings menu name",
        description = "The name of the RVX settings menu.",
        required = true
    )
    override fun execute(context: ResourceContext) {
        /**
         * Replace settings icon and label
         */

        var settingsLabel = DEFAULT_NAME

        if (!RVXSettingsMenuName.isNullOrEmpty())
            settingsLabel = RVXSettingsMenuName!!

        arrayOf("preferences", "preferences_logged_in").forEach { targetXML ->
            val resDirectory = context["res"]
            val targetXml = resDirectory.resolve("xml").resolve("$targetXML.xml").toPath()

            if (!targetXml.exists())
                throw PatchException("The preferences can not be found.")

            val preference = context["res/xml/$targetXML.xml"]

            preference.writeText(
                preference.readText()
                    .replace(
                        "\"@drawable/icon_text_post\" android:title=\"@string/label_acknowledgements\"",
                        "\"@drawable/icon_beta_planet\" android:title=\"$settingsLabel\""
                    )
            )
        }
    }
}
