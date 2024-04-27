package app.revanced.patches.youtube.utils.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object RemoveLikeFingerprint : MethodFingerprint(
    "V",
    strings = listOf("like/removelike")
)
