package app.revanced.patches.music.navigation.components

import app.revanced.patches.music.utils.resourceid.colorGrey
import app.revanced.patches.music.utils.resourceid.text1
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val tabLayoutFingerprint = legacyFingerprint(
    name = "tabLayoutFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("FEmusic_radio_builder"),
    literals = listOf(colorGrey)
)

internal val tabLayoutTextFingerprint = legacyFingerprint(
    name = "tabLayoutTextFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT
    ),
    literals = listOf(text1)
)
