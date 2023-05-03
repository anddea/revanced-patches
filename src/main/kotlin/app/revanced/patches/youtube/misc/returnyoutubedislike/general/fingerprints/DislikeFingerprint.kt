package app.revanced.patches.youtube.misc.returnyoutubedislike.general.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object DislikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/dislike")
)