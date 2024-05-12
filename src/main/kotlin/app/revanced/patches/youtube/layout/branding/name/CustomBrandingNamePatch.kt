package app.revanced.patches.youtube.layout.branding.name

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.shared.elements.StringsElementsUtils.removeStringsElements
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusLabel
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("DEPRECATION", "unused")
object CustomBrandingNamePatch : BaseResourcePatch(
    name = "Custom branding name YouTube",
    description = "Rename the YouTube app to the name specified in options.json.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false,
) {
    private const val APP_NAME = "RVX"

    private val AppName by stringPatchOption(
        key = "AppName",
        default = APP_NAME,
        values = mapOf(
            "Full name" to "ReVanced Extended",
            "Short name" to APP_NAME
        ),
        title = "App name",
        description = "The name of the app.",
        required = true
    )

    override fun execute(context: ResourceContext) {
        context.removeStringsElements(
            arrayOf("application_name")
        )

        AppName?.let {
            context.xmlEditor["res/values/strings.xml"].use { editor ->
                val document = editor.file

                mapOf(
                    "application_name" to it
                ).forEach { (k, v) ->
                    val stringElement = document.createElement("string")

                    stringElement.setAttribute("name", k)
                    stringElement.textContent = v

                    document.getElementsByTagName("resources").item(0).appendChild(stringElement)
                }
            }
            context.updatePatchStatusLabel(it)
        } ?: throw PatchException("Invalid app name.")
    }
}
