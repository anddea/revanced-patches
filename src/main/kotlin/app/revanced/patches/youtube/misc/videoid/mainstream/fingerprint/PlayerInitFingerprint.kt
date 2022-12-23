package app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerInitFingerprint : MethodFingerprint(
    strings = listOf(
        "playVideo called on player response with no videoStreamingData."
    ),
)