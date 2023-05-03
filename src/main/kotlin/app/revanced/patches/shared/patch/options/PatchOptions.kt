package app.revanced.patches.shared.patch.options

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.*
import app.revanced.patches.shared.annotation.RVXCompatibility

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

        /**
         * Custom Branding Name (YouTube)
         */
        internal var YouTubeAppName: String? by option(
            PatchOption.StringOption(
                key = "YouTubeAppName",
                default = "ReVanced Extended",
                title = "Application Name of YouTube",
                description = "The name of the YouTube it will show on your home screen."
            )
        )

        /**
         * Custom Package Name (YouTube)
         */
        internal var YouTubePackageName: String? by option(
            PatchOption.StringOption(
                key = "YouTubePackageName",
                default = "app.rvx.android.youtube",
                title = "Package Name of YouTube",
                description = "The package name of the YouTube. (NON-ROOT user only)"
            )
        )

        /**
         * Custom Branding Full Name (YouTube Music)
         */
        internal var MusicAppNameFull: String? by option(
            PatchOption.StringOption(
                key = "MusicAppNameFull",
                default = "ReVanced Extended Music",
                title = "Application Name of YouTube Music",
                description = "The name of the YouTube Music it will show on your notification panel."
            )
        )

        /**
         * Custom Branding Short Name (YouTube Music)
         */
        internal var MusicAppNameShort: String? by option(
            PatchOption.StringOption(
                key = "MusicAppNameShort",
                default = "RVX Music",
                title = "Application Name of YouTube Music",
                description = "The name of the YouTube Music it will show on your home screen."
            )
        )

        /**
         * Custom Package Name (YouTube Music)
         */
        internal var MusicPackageName: String? by option(
            PatchOption.StringOption(
                key = "MusicPackageName",
                default = "app.rvx.android.apps.youtube.music",
                title = "Package Name of YouTube Music",
                description = "The package name of the YouTube Music. (NON-ROOT user only)"
            )
        )

        /**
         * Custom Double-tap to seek Values
         */
        internal var CustomDoubleTapLengthArrays: String? by option(
            PatchOption.StringOption(
                key = "CustomDoubleTapLengthArrays",
                default = "3, 5, 10, 15, 20, 30, 60, 120, 180",
                title = "Double-tap to seek Values",
                description = "A list of custom double-tap to seek lengths. Be sure to separate them with commas (,)."
            )
        )

        /**
         * Custom Speed Values
         */
        internal var CustomSpeedArrays: String? by option(
            PatchOption.StringOption(
                key = "CustomSpeedArrays",
                default = "0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0, 5.0",
                title = "Custom Speed Values",
                description = "A list of custom video speeds. Be sure to separate them with commas (,)."
            )
        )

        /**
         * Theme
         */
        internal var darkThemeBackgroundColor: String? by option(
            PatchOption.StringOption(
                key = "darkThemeBackgroundColor",
                default = "@android:color/black",
                title = "Background color for the dark theme",
                description = "The background color of the dark theme. Can be a hex color or a resource reference."
            )
        )
    }
}
