package app.revanced.patches.shared.packagename

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption

@Patch(
    name = "Custom package name",
    description = "Changes the package name for the non-root build of YouTube and YouTube Music to the name specified in options.json.",
    compatiblePackages = [
        CompatiblePackage("com.google.android.youtube"),
        CompatiblePackage("com.google.android.apps.youtube.music")
    ]
)
@Suppress("unused")
object PackageNamePatch : ResourcePatch() {
    private const val CLONE_PACKAGE_NAME_YOUTUBE = "bill.youtube"
    private const val DEFAULT_PACKAGE_NAME_YOUTUBE = "anddea.youtube"
    internal const val ORIGINAL_PACKAGE_NAME_YOUTUBE = "com.google.android.youtube"

    private const val CLONE_PACKAGE_NAME_YOUTUBE_MUSIC = "bill.youtube.music"
    private const val DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC = "anddea.youtube.music"
    internal const val ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"

    internal var packageNameYouTube = DEFAULT_PACKAGE_NAME_YOUTUBE
    internal var packageNameYouTubeMusic = DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC

    private val PackageNameYouTube by stringPatchOption(
        key = "PackageNameYouTube",
        default = DEFAULT_PACKAGE_NAME_YOUTUBE,
        values = mapOf(
            "Clone" to CLONE_PACKAGE_NAME_YOUTUBE,
            "Default" to DEFAULT_PACKAGE_NAME_YOUTUBE
        ),
        title = "Package name of YouTube",
        description = "The name of the package to use in MicroG support",
        required = true
    )

    private val PackageNameYouTubeMusic by stringPatchOption(
        key = "PackageNameYouTubeMusic",
        default = DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC,
        values = mapOf(
            "Clone" to CLONE_PACKAGE_NAME_YOUTUBE_MUSIC,
            "Default" to DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC
        ),
        title = "Package name of YouTube Music",
        description = "The name of the package to use in MicroG support",
        required = true
    )

    override fun execute(context: ResourceContext) {
        if (PackageNameYouTube != null && PackageNameYouTube!! != ORIGINAL_PACKAGE_NAME_YOUTUBE) {
            packageNameYouTube = PackageNameYouTube!!
        }
        if (PackageNameYouTubeMusic != null && PackageNameYouTubeMusic!! != ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC) {
            packageNameYouTubeMusic = PackageNameYouTubeMusic!!
        }
    }

    fun getPackageName(originalPackageName: String): String {
        if (originalPackageName == ORIGINAL_PACKAGE_NAME_YOUTUBE) {
            return packageNameYouTube
        } else if (originalPackageName == ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC) {
            return packageNameYouTubeMusic
        }
        throw PatchException("Unknown package name!")
    }
}
