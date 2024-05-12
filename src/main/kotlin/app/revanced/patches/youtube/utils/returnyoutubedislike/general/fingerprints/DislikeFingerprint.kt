package app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object DislikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/dislike")
)