package app.revanced.patches.youtube.misc.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object RemoveLikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/removelike")
)