package app.revanced.patches.youtube.misc.quic.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object CronetEngineBuilderFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC.value,
    parameters = listOf("Z"),
    customFingerprint = { it, _ -> it.definingClass.endsWith("CronetEngine\$Builder;") && it.name == "enableQuic" }
)