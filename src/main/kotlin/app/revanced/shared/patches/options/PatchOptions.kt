package app.revanced.shared.patches.options

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.*
import app.revanced.shared.annotation.RVXCompatibility

@Patch
@Name("patch-options")
@Description("Create an options.toml file.")
@RVXCompatibility
@Version("0.0.1")
class PatchOptions : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        return PatchResultSuccess()
    }

    /*
    * Custom Branding Name
    */

    companion object : OptionsContainer() {

        /*
        * Custom Branding Name
        */
        internal var YouTube_AppName: String? by option(
            PatchOption.StringOption(
                key = "YouTube_AppName",
                default = "ReVanced Extended",
                title = "Application Name of YouTube",
                description = "The name of the YouTube it will show on your home screen.",
                required = true
            )
        )

        /*
        * Custom Package Name (YouTube)
        */
        internal var YouTube_PackageName: String? by option(
            PatchOption.StringOption(
                key = "YouTube_PackageName",
                default = "app.rvx.android.youtube",
                title = "Package Name of YouTube",
                description = "The package name of the YouTube. (NON-ROOT user only)",
                required = true
            )
        )

        /*
        * Custom Package Name (YouTube Music)
        */
        internal var Music_PackageName: String? by option(
            PatchOption.StringOption(
                key = "Music_PackageName",
                default = "app.rvx.android.apps.youtube.music",
                title = "Package Name of YouTube Music",
                description = "The package name of the YouTube Music. (NON-ROOT user only)",
                required = true
            )
        )

        /*
        * Custom Speed Values
        */
        internal var CustomSpeedArrays: String? by option(
            PatchOption.StringOption(
                key = "Custom_Speed_Arrays",
                default = "0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0, 5.0",
                title = "Custom Speed Values",
                description = "A list of custom video speeds. Be sure to separate them with commas (,).",
                required = true
            )
        )

        /*
        * Overlay Buttons Icon
        */
        internal var Overlay_Buttons_Icon: String? by option(
            PatchOption.StringOption(
                key = "Overlay_Buttons_Icon",
                default = "new",
                title = "Overlay button icon selection",
                description = "Choose an overlay buttons icon (old/new)",
                required = true
            )
        )

        /*
        * Theme
        */
        internal var darkThemeBackgroundColor: String? by option(
            PatchOption.StringOption(
                key = "darkThemeBackgroundColor",
                default = "@android:color/black",
                title = "Background color for the dark theme",
                description = "The background color of the dark theme. Can be a hex color or a resource reference.",
                required = true
            )
        )

    }
}
