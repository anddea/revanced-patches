package app.revanced.patches.music.video.information.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object VideoEndFingerprint : MethodFingerprint(
    strings = listOf("Attempting to seek during an ad")
)