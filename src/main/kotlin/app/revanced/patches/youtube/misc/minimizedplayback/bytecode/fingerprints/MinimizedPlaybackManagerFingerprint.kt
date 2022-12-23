package app.revanced.patches.youtube.misc.minimizedplayback.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction

object MinimizedPlaybackManagerFingerprint : MethodFingerprint(
    "Z", AccessFlags.PUBLIC or AccessFlags.STATIC, listOf("L"),
    listOf(Opcode.AND_INT_LIT16),
    customFingerprint = { methodDef ->
        methodDef.implementation!!.instructions.any {
            ((it as? NarrowLiteralInstruction)?.narrowLiteral == 64657230)
        }
    }
)