package app.morphe.patches.shared.drc

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * YouTube 19.30 ~
 * YouTube Music 7.13 ~
 */
internal val compressionRatioFingerprint = legacyFingerprint(
    name = "compressionRatioFingerprint",
    returnType = "Lj${'$'}/util/Optional;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.IGET,
        Opcode.NEG_FLOAT,
    )
)

/**
 * ~ YouTube 19.29
 * ~ YouTube Music 7.12
 */
internal val compressionRatioLegacyFingerprint = legacyFingerprint(
    name = "compressionRatioLegacyFingerprint",
    returnType = "F",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IGET,
        Opcode.RETURN,
    )
)

internal const val VOLUME_NORMALIZATION_EXPERIMENTAL_FEATURE_FLAG = 45425391L

internal val volumeNormalizationConfigFingerprint = legacyFingerprint(
    name = "volumeNormalizationConfigFingerprint",
    literals = listOf(VOLUME_NORMALIZATION_EXPERIMENTAL_FEATURE_FLAG)
)

// modified by lavinhoque33, 2026-10-07: YouTube 21.39 / YT Music 9.40 removed the flag; the new
// loudness algorithm is unconditional inside this method (chm.ax / hct.t). Does not match older versions.
internal val volumeNormalizationMethodFingerprint = legacyFingerprint(
    name = "volumeNormalizationMethodFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L", "F", "Z"),
    strings = listOf("xheaac", ";rng.0;trkcfg.0"),
)
