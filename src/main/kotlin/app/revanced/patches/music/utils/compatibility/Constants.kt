package app.revanced.patches.music.utils.compatibility

import app.revanced.patcher.patch.Patch

object Constants {
    val COMPATIBLE_PACKAGE = setOf(
        Patch.CompatiblePackage(
            "com.google.android.apps.youtube.music",
            setOf(
                "6.29.58", // Latest version that supports the 'Restore old player layout' setting.
                "6.33.52", // Latest version with the legacy code of YouTube Music.
                "6.42.55", // Latest version that supports Android 7.0
                "6.51.53", // Latest version of YouTube Music 6.xx.xx
                "7.04.51", // Latest version supported by the RVX patch.
            )
        )
    )
}