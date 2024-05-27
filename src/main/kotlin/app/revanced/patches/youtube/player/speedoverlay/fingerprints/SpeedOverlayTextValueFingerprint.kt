package app.revanced.patches.youtube.player.speedoverlay.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.ReferenceFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SpeedOverlayTextValueFingerprint : ReferenceFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.CONST_WIDE_HIGH16),
    reference = { "Ljava/math/BigDecimal;->signum()I" }
)