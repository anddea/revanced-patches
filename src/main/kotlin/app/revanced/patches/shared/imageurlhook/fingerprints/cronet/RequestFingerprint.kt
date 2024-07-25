package app.revanced.patches.shared.imageurlhook.fingerprints.cronet

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.shared.imageurlhook.fingerprints.cronet.RequestFingerprint.IMPLEMENTATION_CLASS_NAME
import com.android.tools.smali.dexlib2.AccessFlags

internal object RequestFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { _, classDef ->
        classDef.type == IMPLEMENTATION_CLASS_NAME
    }
) {
    const val IMPLEMENTATION_CLASS_NAME = "Lorg/chromium/net/impl/CronetUrlRequest;"
}