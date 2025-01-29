package app.revanced.patches.music.utils.compatibility

import app.revanced.patcher.patch.PackageName
import app.revanced.patcher.patch.VersionName

internal object Constants {
    internal const val YOUTUBE_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music"

    val COMPATIBLE_PACKAGE: Pair<PackageName, Set<VersionName>?> = Pair(
        YOUTUBE_MUSIC_PACKAGE_NAME,
        setOf(
            "6.20.51", // This is the latest version that supports Android 5.0
            "6.29.59", // This is the latest version that supports the 'Restore old player layout' setting.
            "6.42.55", // This is the latest version that supports Android 7.0
            "6.51.53", // This is the latest version of YouTube Music 6.xx.xx
            "7.16.53", // This is the latest version that supports the 'Spoof app version' patch.
            "7.25.53", // This is the last supported version for 2024.
            "8.02.53", // This is the latest version supported by the RVX patch.
        )
    )
}