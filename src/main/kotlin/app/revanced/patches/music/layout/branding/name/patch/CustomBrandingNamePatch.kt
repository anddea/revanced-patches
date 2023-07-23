package app.revanced.patches.music.layout.branding.name.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.fix.decoding.patch.DecodingPatch

@Patch
@Name("Custom branding Music name")
@Description("Rename the YouTube Music app to the name specified in options.json.")
@DependsOn(
    [
        DecodingPatch::class,
        RemoveElementsPatch::class
    ]
)
@MusicCompatibility
@Version("0.0.1")
class CustomBrandingNamePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        val longName = MusicLongName
            ?: throw PatchResultError("Invalid app name.")

        val shortName = MusicShortName
            ?: throw PatchResultError("Invalid app name.")

        context.xmlEditor["res/values/strings.xml"].use { editor ->
            val document = editor.file

            mapOf(
                "app_name" to longName,
                "app_launcher_name" to shortName
            ).forEach { (k, v) ->
                val stringElement = document.createElement("string")

                stringElement.setAttribute("name", k)
                stringElement.textContent = v

                document.getElementsByTagName("resources").item(0).appendChild(stringElement)
            }
        }

        return PatchResultSuccess()
    }

    companion object : OptionsContainer() {
        var MusicLongName: String? by option(
            PatchOption.StringOption(
                key = "MusicLongName",
                default = "ReVanced Extended Music",
                title = "Application Name of YouTube Music",
                description = "The name of the YouTube Music it will show on your notification panel."
            )
        )
        var MusicShortName: String? by option(
            PatchOption.StringOption(
                key = "MusicShortName",
                default = "RVX Music",
                title = "Application Name of YouTube Music",
                description = "The name of the YouTube Music it will show on your home screen."
            )
        )
    }
}
