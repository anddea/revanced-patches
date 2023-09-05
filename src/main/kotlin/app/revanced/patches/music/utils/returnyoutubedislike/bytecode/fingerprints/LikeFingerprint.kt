package app.revanced.patches.music.utils.returnyoutubedislike.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object LikeFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("like/like")
)