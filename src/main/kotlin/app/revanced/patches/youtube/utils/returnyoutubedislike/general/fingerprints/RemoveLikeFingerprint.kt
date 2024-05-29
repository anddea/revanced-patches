package app.revanced.patches.youtube.utils.returnyoutubedislike.general.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object RemoveLikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/removelike")
)