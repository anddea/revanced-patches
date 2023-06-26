package app.revanced.patches.youtube.utils.videoid.general.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerInitFingerprint : MethodFingerprint(
    strings = listOf("playVideo called on player response with no videoStreamingData.")
)