package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object EngagementPanelHeightFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    // In YouTube Music 7.21.50+, there are two methods with similar structure, so this Opcode pattern must be used.
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
    ),
    parameters = emptyList(),
    customFingerprint = custom@{ methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "booleanValue"
        } >= 0
    }
)
