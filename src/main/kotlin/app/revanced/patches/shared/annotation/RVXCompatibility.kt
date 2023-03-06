package app.revanced.patches.shared.annotation

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility(
    [
        Package("com.google.android.youtube"),
        Package("com.google.android.apps.youtube.music")
    ]
)
@Target(AnnotationTarget.CLASS)
internal annotation class RVXCompatibility

