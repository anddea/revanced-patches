package app.morphe.patches.music.general.landscapemode

import app.morphe.patches.music.utils.resourceid.isTablet
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val tabletIdentifierFingerprint = legacyFingerprint(
    name = "tabletIdentifierFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    ),
    literals = listOf(isTablet)
)

