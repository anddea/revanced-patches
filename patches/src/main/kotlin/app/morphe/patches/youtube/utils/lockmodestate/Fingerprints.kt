package app.morphe.patches.youtube.utils.lockmodestate

import app.morphe.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val lockModeStateFingerprint = legacyFingerprint(
    name = "lockModeStateFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC.value,
    parameters = emptyList(),
    opcodes = listOf(Opcode.RETURN_OBJECT),
    customFingerprint = { method, _ ->
        method.name == "getLockModeStateEnum"
    }
)
