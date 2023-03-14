package app.revanced.patches.youtube.misc.quic.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object ExperimentalCronetEngineBuilderFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.PUBLIC.value,
    parameters = listOf("Z"),
    customFingerprint = { it.definingClass == "Lorg/chromium/net/ExperimentalCronetEngine\$Builder;" && it.name == "enableQuic" }
)