package app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object SeekFingerprint : MethodFingerprint(
    strings = listOf("Attempting to seek during an ad")
)