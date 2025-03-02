@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.youtube.misc.quic

import app.revanced.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val cronetEngineBuilderFingerprint = legacyFingerprint(
    name = "cronetEngineBuilderFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC.value,
    parameters = listOf("Z"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/CronetEngine\$Builder;") &&
                method.name == "enableQuic"
    }
)

internal val experimentalCronetEngineBuilderFingerprint = legacyFingerprint(
    name = "experimentalCronetEngineBuilderFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC.value,
    parameters = listOf("Z"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/ExperimentalCronetEngine\$Builder;") &&
                method.name == "enableQuic"
    }
)