package app.revanced.patches.youtube.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * This value is the key for the playback speed overlay value.
 */
internal object SpeedOverlayValueFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(Opcode.DOUBLE_TO_FLOAT),
    literalSupplier = { 45411328 }
)