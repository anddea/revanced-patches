package app.revanced.patches.music.utils.compatibility

import app.revanced.patcher.patch.Patch

object Constants {
    val COMPATIBLE_PACKAGE = setOf(
        Patch.CompatiblePackage(
            "com.google.android.apps.youtube.music",
            setOf(
                "6.29.58",
                "6.31.55",
                "6.33.52",
                "6.50.53",
                "6.51.52"
            )
        )
    )
}