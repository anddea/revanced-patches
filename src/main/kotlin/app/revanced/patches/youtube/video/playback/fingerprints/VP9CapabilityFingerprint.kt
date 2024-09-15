package app.revanced.patches.youtube.video.playback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object VP9CapabilityFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    strings = listOf(
        "vp9_supported",
        "video/x-vnd.on2.vp9"
    )
)
