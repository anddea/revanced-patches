package app.revanced.patches.music.misc.drc

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * YouTube Music 7.13.52 ~
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
 * ~ YouTube Music 7.12.52
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
    parameters = listOf("F"),
    returnType = "V",
    literals = listOf(VOLUME_NORMALIZATION_EXPERIMENTAL_FEATURE_FLAG)
)
