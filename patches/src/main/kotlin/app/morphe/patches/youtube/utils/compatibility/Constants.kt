package app.morphe.patches.youtube.utils.compatibility

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    internal const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"

    val COMPATIBILITY_YOUTUBE = Compatibility(
        name = "YouTube",
        packageName = YOUTUBE_PACKAGE_NAME,
        targets = listOf(
            AppTarget(version = "19.43.41", minSdk = 26), // This is the latest version where edge-to-edge display is not enforced on Android 15+.
            AppTarget(version = "19.44.39", minSdk = 26), // This is the only version that has experimental shortcut icons.
            AppTarget(version = "19.47.53", minSdk = 26),
            AppTarget(version = "20.05.46", minSdk = 26),
            AppTarget(version = "20.21.37", minSdk = 26),
            AppTarget(version = "20.31.42", minSdk = 28),
            AppTarget(version = "20.47.62", minSdk = 28),
            AppTarget(version = "20.51.39", minSdk = 28),
        )
    )

    val COMPATIBILITY_YOUTUBE_RELOAD_VIDEO = COMPATIBILITY_YOUTUBE.excluding(
        "19.43.41",
        "19.44.39",
        "19.47.53",
    )
}
