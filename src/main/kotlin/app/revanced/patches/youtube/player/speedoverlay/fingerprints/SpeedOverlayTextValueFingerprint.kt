package app.revanced.patches.youtube.player.speedoverlay.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object SpeedOverlayTextValueFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.CONST_WIDE_HIGH16),
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            getReference<MethodReference>()?.toString() == "Ljava/math/BigDecimal;->signum()I"
        } >= 0
    }
)