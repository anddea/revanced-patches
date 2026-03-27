package app.morphe.patches.youtube.layout.branding.name

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.CUSTOM_BRANDING_NAME_FOR_YOUTUBE
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.removeStringsElements
import app.morphe.util.valueOrThrow

private const val APP_NAME = "RVX"

@Suppress("unused")
val customBrandingNamePatch = resourcePatch(
    CUSTOM_BRANDING_NAME_FOR_YOUTUBE.title,
    CUSTOM_BRANDING_NAME_FOR_YOUTUBE.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val appNameOption = stringOption(
        key = "appName",
        default = APP_NAME,
        values = mapOf(
            "ReVanced Extended" to "ReVanced Extended",
            "RVX" to APP_NAME,
            "YouTube RVX" to "YouTube RVX",
            "YouTube" to "YouTube",
        ),
        title = "App name",
        description = "The name of the app.",
        required = true,
    )

    execute {
        // Check patch options first.
        val appName = appNameOption
            .valueOrThrow()

        removeStringsElements(
            arrayOf("application_name")
        )

        document("res/values/strings.xml").use { document ->
            val stringElement = document.createElement("string")

            stringElement.setAttribute("name", "application_name")
            stringElement.textContent = appName

            document.getElementsByTagName("resources").item(0)
                .appendChild(stringElement)
        }

    }
}
