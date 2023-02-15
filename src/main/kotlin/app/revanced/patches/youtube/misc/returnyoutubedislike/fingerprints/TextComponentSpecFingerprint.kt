package app.revanced.patches.youtube.misc.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object TextComponentSpecFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.STATIC.getValue(),
    opcodes = listOf(Opcode.CMPL_FLOAT),
    customFingerprint = { methodDef ->
        methodDef.implementation!!.instructions.any {
            ((it as? NarrowLiteralInstruction)?.narrowLiteral == 16842907)
        }
    }
)