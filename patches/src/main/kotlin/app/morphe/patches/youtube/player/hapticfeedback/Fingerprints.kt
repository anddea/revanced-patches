package app.morphe.patches.youtube.player.hapticfeedback

import app.morphe.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal val markerHapticsFingerprint = legacyFingerprint(
    name = "markerHapticsFingerprint",
    returnType = "V",
    strings = listOf("Failed to execute markers haptics vibrate.")
)

internal val scrubbingHapticsFingerprint = legacyFingerprint(
    name = "scrubbingHapticsFingerprint",
    returnType = "V",
    strings = listOf("Failed to haptics vibrate for fine scrubbing.")
)

internal val seekHapticsFingerprint = legacyFingerprint(
    name = "seekHapticsFingerprint",
    returnType = "V",
    opcodes = listOf(Opcode.SGET),
    strings = listOf("Failed to easy seek haptics vibrate."),
    customFingerprint = { method, _ -> method.name == "run" }
)

internal val seekUndoHapticsFingerprint = legacyFingerprint(
    name = "seekUndoHapticsFingerprint",
    returnType = "V",
    strings = listOf("Failed to execute seek undo haptics vibrate.")
)

internal val zoomHapticsFingerprint = legacyFingerprint(
    name = "zoomHapticsFingerprint",
    returnType = "V",
    strings = listOf("Failed to haptics vibrate for video zoom")
)
