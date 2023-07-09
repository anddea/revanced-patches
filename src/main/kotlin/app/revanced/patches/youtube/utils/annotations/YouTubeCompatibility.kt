package app.revanced.patches.youtube.utils.annotations

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility(
    [Package(
        "com.google.android.youtube", arrayOf(
            "18.20.39",
            "18.21.35",
            "18.22.37",
            "18.23.36",
            "18.24.37",
            "18.25.40"
        )
    )]
)
@Target(AnnotationTarget.CLASS)
internal annotation class YouTubeCompatibility

