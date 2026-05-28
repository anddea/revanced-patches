package app.morphe.patches.music.utils.compatibility

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    internal const val YOUTUBE_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music"

    val COMPATIBILITY_YOUTUBE_MUSIC = Compatibility(
        name = "YouTube Music",
        packageName = YOUTUBE_MUSIC_PACKAGE_NAME,
        targets = listOf(
            AppTarget(version = "6.20.51", minSdk = 21), // This is the latest version that supports Android 5.0
            AppTarget(version = "6.29.59", minSdk = 24), // This is the latest version that supports the 'Restore old player layout' setting.
            AppTarget(version = "6.42.55", minSdk = 24), // This is the latest version that supports Android 7.0
            AppTarget(version = "6.51.53", minSdk = 26), // This is the latest version of YouTube Music 6.xx.xx
            AppTarget(version = "7.16.53", minSdk = 26), // This is the latest version that supports the 'Hide action button labels' setting.
            AppTarget(version = "7.25.53", minSdk = 26), // This is the last supported version for 2024.
            AppTarget(version = "8.12.54", minSdk = 26), // This was the latest version supported by the previous RVX patch.
            AppTarget(version = "8.28.54", minSdk = 26), // This is the latest version that supports the 'Replace Samples button' setting.
            AppTarget(version = "8.30.54", minSdk = 26), // This is the latest version supported by the RVX patch.
        )
    )

    val COMPATIBILITY_YOUTUBE_MUSIC_SPOOF_APP_VERSION = COMPATIBILITY_YOUTUBE_MUSIC.excluding(
        "6.20.51",
        "6.29.59",
    )

    val COMPATIBILITY_YOUTUBE_MUSIC_SPOOF_APP_VERSION_FOR_LYRICS =
        COMPATIBILITY_YOUTUBE_MUSIC_SPOOF_APP_VERSION.excluding(
            "6.42.55",
        )

    val COMPATIBILITY_YOUTUBE_MUSIC_CAIRO_SPLASH_ANIMATION =
        COMPATIBILITY_YOUTUBE_MUSIC_SPOOF_APP_VERSION_FOR_LYRICS.excluding(
            "6.51.53",
        )
}
