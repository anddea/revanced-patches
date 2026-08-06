package app.morphe.patches.music.utils.compatibility

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    internal const val YOUTUBE_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music"

    val COMPATIBILITY_YOUTUBE_MUSIC = Compatibility(
        name = "YouTube Music",
        packageName = YOUTUBE_MUSIC_PACKAGE_NAME,
        targets = listOf(
            AppTarget(version = "8.12.54", minSdk = 26), // This was the latest version supported by the previous RVX patch.
            AppTarget(version = "8.28.54", minSdk = 26), // This is the latest version that supports the 'Replace Samples button' setting.
            AppTarget(version = "8.30.54", minSdk = 26),
            AppTarget(version = "9.15.51", minSdk = 26),
        )
    )

    val COMPATIBILITY_YOUTUBE_MUSIC_THIRD_PARTY_LYRICS =
        COMPATIBILITY_YOUTUBE_MUSIC.excluding(
            "8.12.54",
            "8.28.54",
            "8.30.54",
        )
}
