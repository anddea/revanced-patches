package app.revanced.patches.music.general.floatingbutton.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isNarrowLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object FloatingButtonParentFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.INVOKE_DIRECT),
    customFingerprint = { methodDef, _ -> methodDef.isNarrowLiteralExists(259982244) }
)

