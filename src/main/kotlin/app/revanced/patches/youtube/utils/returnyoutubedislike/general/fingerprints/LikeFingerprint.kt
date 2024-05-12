package app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object LikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/like")
)