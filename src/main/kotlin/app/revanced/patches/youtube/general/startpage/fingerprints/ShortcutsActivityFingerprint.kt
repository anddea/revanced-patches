package app.revanced.patches.youtube.general.startpage.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object ShortcutsActivityFingerprint : MethodFingerprint(
    parameters = listOf("Landroid/content/Intent;"),
    strings = listOf("has_handled_intent"),
)