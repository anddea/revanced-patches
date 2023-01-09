package app.revanced.patches.youtube.misc.customvideobuffer.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction
import org.jf.dexlib2.Opcode

object ReBufferFingerprint : MethodFingerprint(
    returnType = "I",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    opcodes = listOf(
        Opcode.IF_LEZ,
        Opcode.RETURN
    ),
    customFingerprint = {
        it.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/media/PlayerConfigModel;"
                && it.implementation!!.instructions.any { instruction ->
            ((instruction as? NarrowLiteralInstruction)?.narrowLiteral == 5000)
        }
    }
)