package app.revanced.patches.shared.annotation

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility(
    [Package(
        "com.google.android.youtube", arrayOf(
            "18.12.35",
            "18.13.38",
            "18.14.41",
            "18.15.40",
            "18.16.39"
        )
    )]
)
@Target(AnnotationTarget.CLASS)
internal annotation class YouTubeCompatibility

