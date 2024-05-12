package app.revanced.patches.youtube.utils.compatibility

import app.revanced.patcher.patch.Patch

object Constants {
    val COMPATIBLE_PACKAGE = setOf(
        Patch.CompatiblePackage(
            "com.google.android.youtube",
            setOf(
                "18.29.38",
                "18.33.40",
                "18.38.44",
                "18.48.39",
                "19.05.36",
                "19.16.39"
            )
        )
    )
}