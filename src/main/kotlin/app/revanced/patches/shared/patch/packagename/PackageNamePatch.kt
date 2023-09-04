package app.revanced.patches.shared.patch.packagename

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.RVXCompatibility

@Patch
@Name("Custom package name")
@Description("Specifies the package name for YouTube and YT Music in the MicroG build.")
@RVXCompatibility
class PackageNamePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {
    }

    companion object : OptionsContainer() {

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
    }
}
