package app.revanced.patches.shared.ads

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * This method seems to be deprecated, but I'm not sure.
 */
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

/**
 * Simply injecting into [videoAdsFingerprint] can block video ads.
 *
 * Nevertheless, if this Method is not injected,
 * Video ad stream is downloaded, consuming unnecessary network resources.
 */
internal val playerBytesAdLayoutFingerprint = legacyFingerprint(
    name = "playerBytesAdLayoutFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf(
        "Bootstrapped layout construction resulted in non PlayerBytesLayout. PlayerAds count: "
    )
)

internal val videoAdsFingerprint = legacyFingerprint(
    name = "videoAdsFingerprint",
    returnType = "V",
    strings = listOf("markFillRequested", "requestEnterSlot")
)
