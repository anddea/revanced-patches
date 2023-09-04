package app.revanced.patches.youtube.layout.branding.name.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper.updatePatchStatusLabel

@Patch
@Name("Custom branding YouTube name")
@Description("Rename the YouTube app to the name specified in options.json.")
@DependsOn(
    [
        RemoveElementsPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class CustomBrandingNamePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        val appName = YouTubeAppName
            ?: throw PatchException("Invalid app name.")

        context.xmlEditor["res/values/strings.xml"].use { editor ->
            val document = editor.file

            mapOf(
                "application_name" to appName
            ).forEach { (k, v) ->
                val stringElement = document.createElement("string")

                stringElement.setAttribute("name", k)
                stringElement.textContent = v

                document.getElementsByTagName("resources").item(0).appendChild(stringElement)
            }
        }


        context.updatePatchStatusLabel("$appName")

    }

    companion object : OptionsContainer() {
        var YouTubeAppName: String? by option(
            PatchOption.StringOption(
                key = "YouTubeAppName",
                default = "ReVanced Extended",
                title = "Application Name of YouTube",
                description = "The name of the YouTube it will show on your home screen."
            )
        )
    }
}
