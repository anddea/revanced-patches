package app.revanced.patches.youtube.utils.browseid.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object BrowseIdClassFingerprint : MethodFingerprint(
    strings = listOf("\u0001\t\u0000\u0001\u0002\u0010\t\u0000\u0000\u0001\u0002\u1008\u0000\u0003\u1008\u0002\u0005\u1008\u0003\u0006\u1409\u0005\u0007\u1007\u0004\u0008\u1009\u0006\u000c\u1008\n\u000e\u180c\u000b\u0010\u1007\r")
)