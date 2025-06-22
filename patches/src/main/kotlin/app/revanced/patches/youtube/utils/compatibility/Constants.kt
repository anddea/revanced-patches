package app.revanced.patches.youtube.utils.compatibility

import app.revanced.patcher.patch.PackageName
import app.revanced.patcher.patch.VersionName

internal object Constants {
    internal const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"

    val COMPATIBLE_PACKAGE: Pair<PackageName, Set<VersionName>?> = Pair(
        YOUTUBE_PACKAGE_NAME,
        setOf(
            "19.43.41", // This is the latest version where edge-to-edge display is not enforced on Android 15+.
            "19.44.39", // This is the only version that has experimental shortcut icons.
            "19.47.53", // This is the latest version supported by the RVX patch.
        )
    )
}
