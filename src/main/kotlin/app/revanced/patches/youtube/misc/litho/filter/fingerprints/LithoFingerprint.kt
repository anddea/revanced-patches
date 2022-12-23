package app.revanced.patches.youtube.misc.litho.filter.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction

object LithoFingerprint : MethodFingerprint(
    "L", AccessFlags.PUBLIC or AccessFlags.FINAL, listOf("L", "L", "L", "L", "L", "I", "Z"),
    customFingerprint = { methodDef ->
        methodDef.implementation!!.instructions.any {
            ((it as? NarrowLiteralInstruction)?.narrowLiteral == 255496645)
        }
    }
)