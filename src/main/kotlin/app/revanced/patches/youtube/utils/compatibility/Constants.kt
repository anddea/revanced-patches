package app.revanced.patches.youtube.utils.compatibility

import app.revanced.patcher.patch.Patch

object Constants {
    val COMPATIBLE_PACKAGE = setOf(
        Patch.CompatiblePackage(
            "com.google.android.youtube",
            setOf(
                "18.29.38", // Latest version that supports the 'Zoomed to fill' setting.
                "18.33.40", // Latest version that do not use litho components in Shorts.
                "18.38.44", // Latest version with no delay in applying video quality on the server side.
                "18.48.39", // Latest version that do not use Rolling Number.
                "19.05.36", // Latest version with the least YouTube experimental flag.
                "19.16.39", // Latest version that supports the 'Restore old seekbar thumbnails' setting.
                "19.20.35", // Latest version supported by the RVX patch.
            )
        )
    )
}