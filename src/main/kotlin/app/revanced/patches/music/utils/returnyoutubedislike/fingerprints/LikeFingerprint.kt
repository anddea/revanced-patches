package app.revanced.patches.music.utils.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object LikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/like")
)