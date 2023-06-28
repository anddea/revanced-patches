package app.revanced.patches.youtube.overlaybutton.alwaysrepeat.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.SettingsBooleanTimeRangeDialog
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object AutoNavInformerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.XOR_INT_2ADDR
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(
            SettingsBooleanTimeRangeDialog
        )
    }
)