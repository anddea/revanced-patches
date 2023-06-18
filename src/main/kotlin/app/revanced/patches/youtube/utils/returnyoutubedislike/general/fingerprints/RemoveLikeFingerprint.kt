package app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object RemoveLikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/removelike")
)