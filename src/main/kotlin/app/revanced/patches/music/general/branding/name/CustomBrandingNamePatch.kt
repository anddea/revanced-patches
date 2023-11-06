package app.revanced.patches.music.general.branding.name

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption

@Patch(
    name = "Custom branding name YouTube Music",
    description = "Rename the YouTube Music app to the name specified in options.json.",
    dependencies = [RemoveElementsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.15.52",
                "6.20.51",
                "6.25.53",
                "6.26.50"
            ]
        )
    ]
)
@Suppress("unused")
object CustomBrandingNamePatch : ResourcePatch() {
    private const val APP_NAME_NOTIFICATION = "ReVanced Extended Music"
    private const val APP_NAME_LAUNCHER = "RVX Music"

    private val AppNameNotification by stringPatchOption(
        key = "AppNameNotification",
        default = APP_NAME_NOTIFICATION,
        values = mapOf(
            "Full name" to APP_NAME_NOTIFICATION,
            "Short name" to APP_NAME_LAUNCHER
        ),
        title = "App name in notification panel",
        description = "The name of the app as it appears in the notification panel.",
        required = true
    )

    private val AppNameLauncher by stringPatchOption(
        key = "AppNameLauncher",
        default = APP_NAME_LAUNCHER,
        values = mapOf(
            "Full name" to APP_NAME_NOTIFICATION,
            "Short name" to APP_NAME_LAUNCHER
        ),
        title = "App name in launcher",
        description = "The name of the app as it appears in the launcher.",
        required = true
    )

    override fun execute(context: ResourceContext) {

        AppNameNotification?.let { notificationName ->
            AppNameLauncher?.let { launcherName ->
                context.xmlEditor["res/values/strings.xml"].use { editor ->
                    val document = editor.file

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
            } ?: throw PatchException("Invalid app name.")
        } ?: throw PatchException("Invalid app name.")
    }
}
