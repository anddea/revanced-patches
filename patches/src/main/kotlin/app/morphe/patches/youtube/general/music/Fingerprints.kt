package app.morphe.patches.youtube.general.music

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val appDeepLinkFingerprint = legacyFingerprint(
    name = "appDeepLinkFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Ljava/util/Map;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_STRING,
    ),
    strings = listOf("android.intent.action.VIEW"),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            getReference<FieldReference>()?.name == "appDeepLinkEndpoint"
        } >= 0
    }
)
