package app.revanced.patches.music.utils.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object DislikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/dislike")
)