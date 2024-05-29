package app.revanced.patches.music.general.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object ContentPillInFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Content pill VE is null")
)