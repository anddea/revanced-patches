package app.revanced.patches.youtube.layout.branding.name

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.shared.patch.elements.AbstractRemoveStringsElementsPatch
import app.revanced.patches.youtube.utils.integrations.Constants.LANGUAGE_LIST
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusLabel
import app.revanced.patches.youtube.utils.settings.SettingsPatch

@Patch(
    name = "Custom branding name YouTube",
    description = "Rename the YouTube app to the name specified in options.json.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43",
                "19.15.36",
                "19.16.38"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object CustomBrandingNamePatch : AbstractRemoveStringsElementsPatch(
    LANGUAGE_LIST,
    arrayOf("application_name")
) {
    private const val APP_NAME = "ReVanced Extended"

    private val AppName by stringPatchOption(
        key = "AppName",
        default = APP_NAME,
        values = mapOf(
            "Full name" to APP_NAME,
            "Short name" to "RVX"
        ),
        title = "App name",
        description = "The name of the app.",
        required = true
    )

    override fun execute(context: ResourceContext) {
        super.execute(context)

        AppName?.let {
            context.document["res/values/strings.xml"].use { editor ->
                mapOf(
                    "application_name" to it
                ).forEach { (k, v) ->
                    val stringElement = editor.createElement("string")

                    stringElement.setAttribute("name", k)
                    stringElement.textContent = v

                    editor.getElementsByTagName("resources").item(0).appendChild(stringElement)
                }
            }
            context.updatePatchStatusLabel(it)
        } ?: throw PatchException("Invalid app name.")
    }
}
