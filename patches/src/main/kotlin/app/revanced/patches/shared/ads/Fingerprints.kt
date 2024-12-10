package app.revanced.patches.shared.ads

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.util.MethodUtil

internal val advertisingIdFingerprint = legacyFingerprint(
    name = "advertisingIdFingerprint",
    returnType = "V",
    strings = listOf("a."),
    customFingerprint = { method, classDef ->
        MethodUtil.isConstructor(method) &&
                classDef.fields.find { it.type == "Ljava/util/Random;" } != null
    }
)

internal val sslGuardFingerprint = legacyFingerprint(
    name = "sslGuardFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("Cannot initialize SslGuardSocketFactory will null"),
)

internal val musicAdsFingerprint = legacyFingerprint(
    name = "musicAdsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.CONST_WIDE_16,
        Opcode.IPUT_WIDE,
        Opcode.CONST_WIDE_16,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.CONST_4,
    ),
    literals = listOf(4L)
)

internal val videoAdsFingerprint = legacyFingerprint(
    name = "videoAdsFingerprint",
    returnType = "V",
    strings = listOf("markFillRequested", "requestEnterSlot")
)
