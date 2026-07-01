package app.morphe.patches.music.video.information

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.parametersMatch
import app.morphe.patcher.string
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val playerControllerSetTimeReferenceFingerprint = legacyFingerprint(
    name = "playerControllerSetTimeReferenceFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.IGET_OBJECT
    ),
    strings = listOf("Media progress reported outside media playback: ")
)

internal val videoEndFingerprint = legacyFingerprint(
    name = "videoEndFingerprint",
    strings = listOf("Attempting to seek during an ad")
)

internal val videoIdFingerprint = legacyFingerprint(
    name = "videoIdFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/String;"),
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("Null initialPlayabilityStatus")
)

/** Modern player seek method used by YouTube Music 8.51+. */
internal object ModernVideoEndFingerprint : Fingerprint(
    parameters = listOf("J", "L"),
    filters = listOf(
        string("currentPositionMs."),
        string(";seekTimeUs.")
    )
)

/** Modern track-load callback, including the extra boolean introduced after 9.15. */
internal object ModernVideoIdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = listOf(string("Null initialPlayabilityStatus")),
    custom = { method, _ ->
        parametersMatch(method.parameters, listOf("L", "Ljava/lang/String;")) ||
                parametersMatch(method.parameters, listOf("L", "Ljava/lang/String;", "Z"))
    }
)

internal object ModernPlayerControllerSetTimeReferenceFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.IGET_OBJECT
    ),
    strings = listOf("Media progress reported outside media playback: ")
)
