package app.revanced.patches.youtube.flyoutpanel.player.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object CinematicLightingFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.CONST_STRING, // "menu_item_cinematic_lighting"
        null, //  Opcode.INVOKE_INTERFACE or Opcode.INVOKE_VIRTUAL
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ
    ),
    strings = listOf("menu_item_cinematic_lighting")
)