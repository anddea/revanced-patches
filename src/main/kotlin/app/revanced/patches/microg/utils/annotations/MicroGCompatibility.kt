package app.revanced.patches.microg.utils.annotations

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility([Package("com.mgoogle.android.gms")])
@Target(AnnotationTarget.CLASS)
internal annotation class MicroGCompatibility
