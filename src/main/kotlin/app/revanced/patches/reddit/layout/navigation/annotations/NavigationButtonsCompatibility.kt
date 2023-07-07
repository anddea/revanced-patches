package app.revanced.patches.reddit.layout.navigation.annotations

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility([Package("com.reddit.frontpage", arrayOf("2023.25.1"))])
@Target(AnnotationTarget.CLASS)
internal annotation class NavigationButtonsCompatibility
