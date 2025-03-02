package app.revanced.patches.shared.textcomponent

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val textComponentConstructorFingerprint = legacyFingerprint(
    name = "textComponentConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.CONSTRUCTOR,
    strings = listOf("TextComponent")
)

internal val textComponentContextFingerprint = legacyFingerprint(
    name = "textComponentContextFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_BOOLEAN
    )
)
