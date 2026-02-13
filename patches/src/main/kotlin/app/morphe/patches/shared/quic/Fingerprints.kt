@file:Suppress("SpellCheckingInspection")

package app.morphe.patches.shared.quic

import app.morphe.util.fingerprint.legacyFingerprint
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