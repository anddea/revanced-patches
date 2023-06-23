package app.revanced.patches.youtube.utils.litho.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object LithoThemeFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL, // Paint.setColor: inject point
        Opcode.RETURN_VOID
    ),
    customFingerprint = { methodDef, _ -> methodDef.name == "onBoundsChange" }
)