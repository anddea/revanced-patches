package app.revanced.patches.youtube.utils.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object DislikeFingerprint : MethodFingerprint(
    "V",
    strings = listOf("like/dislike")
)
