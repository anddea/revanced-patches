package app.revanced.patches.youtube.utils.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object LikeFingerprint : MethodFingerprint(
    "V",
    strings = listOf("like/like")
)
