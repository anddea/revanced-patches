package app.revanced.patches.youtube.navigation.homepage.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object LauncherActivityFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("forLauncherActivity", "Launcher config used on invalid activity: %s")
)