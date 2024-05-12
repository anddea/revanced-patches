package app.revanced.patches.music.general.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object SearchBarParentFingerprint : MethodFingerprint(
    returnType = "Landroid/content/Intent;",
    strings = listOf("web_search")
)