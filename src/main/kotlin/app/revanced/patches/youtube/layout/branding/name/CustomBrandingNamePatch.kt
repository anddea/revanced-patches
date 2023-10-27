package app.revanced.patches.youtube.layout.branding.name

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.resources.ResourceHelper.updatePatchStatusLabel

@Patch(
    name = "Custom branding name YouTube",
    description = "Rename the YouTube app to the name specified in options.json.",
    dependencies = [
        RemoveElementsPatch::class,
        SettingsPatch::class
    ],
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
                "18.41.39"
            ]
        )
    ]
)
@Suppress("unused")
object CustomBrandingNamePatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

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

    private const val APP_NAME = "ReVanced Extended"

    internal var AppName by stringPatchOption(
        key = "AppName",
        default = APP_NAME,
        values = mapOf(
            "Full name" to APP_NAME,
            "Short name" to "RVX"
        ),
        title = "App name",
        description = "The name of the app."
    )
}
