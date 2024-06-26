package app.revanced.patches.youtube.video.playback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ByteBufferArrayParentFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "C",
    parameters = listOf("Ljava/nio/charset/Charset;", "[C")
)
