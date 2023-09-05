package app.revanced.patches.music.utils.annotations

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility(
    [
        Package(
            "com.google.android.apps.youtube.music",
            arrayOf(
                "6.15.52",
                "6.17.52"
            )
        )
    ]
)
@Target(AnnotationTarget.CLASS)
internal annotation class MusicCompatibility
