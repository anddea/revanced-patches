package app.revanced.patches.music.misc.settings.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object SettingsHeadersFragmentFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT

    ),
    customFingerprint = { it.definingClass.endsWith("/SettingsHeadersFragment;") && it.name == "onCreate" }
)