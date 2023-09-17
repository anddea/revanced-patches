package app.revanced.patches.music.video.information.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerInitFingerprint : MethodFingerprint(
    strings = listOf("playVideo called on player response with no videoStreamingData."),
)