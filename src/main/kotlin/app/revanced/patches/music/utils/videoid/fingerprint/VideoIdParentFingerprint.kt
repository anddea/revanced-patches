package app.revanced.patches.music.utils.videoid.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object VideoIdParentFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = emptyList(),
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_SUPER,
        Opcode.IGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT
    ),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/WatchFragment;") && methodDef.name == "onDestroyView" }
)
