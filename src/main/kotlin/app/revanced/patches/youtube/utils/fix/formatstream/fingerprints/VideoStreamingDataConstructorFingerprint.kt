package app.revanced.patches.youtube.utils.fix.formatstream.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object VideoStreamingDataConstructorFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    customFingerprint = { _, classDef ->
        classDef.type == "Lcom/google/android/libraries/youtube/innertube/model/media/VideoStreamingData;"
    }
)

