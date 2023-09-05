package app.revanced.patches.reddit.utils.settings.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.patch.IntegrationsPatch
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch
import kotlin.io.path.exists

@Patch
@Name("Reddit settings")
@Description("Adds ReVanced settings to Reddit.")
@DependsOn(
    [
        IntegrationsPatch::class,
        SettingsBytecodePatch::class
    ]
)
@RedditCompatibility
class SettingsPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        /**
         * Replace settings icon and label
         */
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
                        "\"@drawable/icon_beta_planet\" android:title=\"ReVanced Extended\""
                    )
            )
        }

    }
}