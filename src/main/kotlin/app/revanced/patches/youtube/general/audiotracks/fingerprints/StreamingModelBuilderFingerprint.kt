package app.revanced.patches.youtube.general.audiotracks.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object StreamingModelBuilderFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("vprng")
)