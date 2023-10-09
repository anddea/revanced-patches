package app.revanced.patches.shared.patch.packagename

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.types.StringPatchOption.Companion.stringPatchOption

@Patch(
    name = "Custom package name",
    description = "Specifies the package name for YouTube and YT Music in the MicroG build.",
    compatiblePackages = [
        CompatiblePackage("com.google.android.youtube"),
        CompatiblePackage("com.google.android.apps.youtube.music")
    ]
)
@Suppress("unused")
object PackageNamePatch : ResourcePatch() {
    internal var YouTubePackageName by stringPatchOption(
        key = "YouTubePackageName",
        default = "app.rvx.android.youtube",
        title = "Package Name of YouTube",
        description = "The package name of the YouTube. (NON-ROOT user only)"
    )

    internal var MusicPackageName by stringPatchOption(
        key = "MusicPackageName",
        default = "app.rvx.android.apps.youtube.music",
        title = "Package Name of YouTube Music",
        description = "The package name of the YouTube Music. (NON-ROOT user only)"
    )

    override fun execute(context: ResourceContext) {
    }
}
