package app.morphe.patches.music.layout.branding.name

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.patch.PatchList.CUSTOM_BRANDING_NAME_FOR_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.util.removeStringsElements
import app.morphe.util.valueOrThrow

private const val APP_NAME_NOTIFICATION = "ReVanced Extended Music"
private const val APP_NAME_LAUNCHER = "RVX Music"

@Suppress("unused")
val customBrandingNamePatch = resourcePatch(
    CUSTOM_BRANDING_NAME_FOR_YOUTUBE_MUSIC.title,
    CUSTOM_BRANDING_NAME_FOR_YOUTUBE_MUSIC.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val appNameNotificationOption = stringOption(
        key = "appNameNotification",
        default = APP_NAME_LAUNCHER,
        values = mapOf(
            "ReVanced Extended Music" to APP_NAME_NOTIFICATION,
            "RVX Music" to APP_NAME_LAUNCHER,
            "YouTube Music" to "YouTube Music",
            "YT Music" to "YT Music",
        ),
        title = "App name in notification panel",
        description = "The name of the app as it appears in the notification panel.",
        required = true
    )

    val appNameLauncherOption = stringOption(
        key = "appNameLauncher",
        default = APP_NAME_LAUNCHER,
        values = mapOf(
            "ReVanced Extended Music" to APP_NAME_NOTIFICATION,
            "RVX Music" to APP_NAME_LAUNCHER,
            "YouTube Music" to "YouTube Music",
            "YT Music" to "YT Music",
        ),
        title = "App name in launcher",
        description = "The name of the app as it appears in the launcher.",
        required = true
    )

    execute {
        // Check patch options first.
        val notificationName = appNameNotificationOption
            .valueOrThrow()
        val launcherName = appNameLauncherOption
            .valueOrThrow()

        removeStringsElements(
            arrayOf("app_launcher_name", "app_name")
        )

        document("res/values/strings.xml").use { document ->
            mapOf(
                "app_name" to notificationName,
                "app_launcher_name" to launcherName
            ).forEach { (k, v) ->
                val stringElement = document.createElement("string")

                stringElement.setAttribute("name", k)
                stringElement.textContent = v

                document.getElementsByTagName("resources").item(0)
                    .appendChild(stringElement)
            }
        }

        updatePatchStatus(CUSTOM_BRANDING_NAME_FOR_YOUTUBE_MUSIC)

    }
}
