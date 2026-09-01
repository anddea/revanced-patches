package app.morphe.patches.youtube.utils.compatibility

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    internal const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"

    val COMPATIBILITY_YOUTUBE = Compatibility(
        name = "YouTube",
        packageName = YOUTUBE_PACKAGE_NAME,
        targets = listOf(
            AppTarget(version = "20.05.46", minSdk = 26),
            AppTarget(version = "21.04.223", minSdk = 28),
            AppTarget(version = "21.07.247", minSdk = 28),
        )
    )

    val COMPATIBILITY_YOUTUBE_RELOAD_VIDEO = COMPATIBILITY_YOUTUBE.excluding(
        "19.43.41",
    )
}
