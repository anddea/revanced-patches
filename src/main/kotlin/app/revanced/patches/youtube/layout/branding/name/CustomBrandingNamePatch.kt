package app.revanced.patches.youtube.layout.branding.name

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.shared.elements.StringsElementsUtils.removeStringsElements
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusLabel
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.valueOrThrow

@Suppress("DEPRECATION", "unused")
object CustomBrandingNamePatch : BaseResourcePatch(
    name = "Custom branding name for YouTube",
    description = "Renames the YouTube app to the name specified in options.json.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false,
) {
    private const val APP_NAME = "RVX"

    private val AppName = stringPatchOption(
        key = "AppName",
        default = APP_NAME,
        values = mapOf(
            "ReVanced Extended" to "ReVanced Extended",
            "RVX" to APP_NAME,
            "YouTube RVX" to "YouTube RVX",
            "YouTube" to "YouTube",
        ),
        title = "App name",
        description = "The name of the app.",
        required = true
    )

    override fun execute(context: ResourceContext) {

        // Check patch options first.
        val appName = AppName
            .valueOrThrow()

        context.removeStringsElements(
            arrayOf("application_name")
        )

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
        context.updatePatchStatusLabel(appName)
    }
}
