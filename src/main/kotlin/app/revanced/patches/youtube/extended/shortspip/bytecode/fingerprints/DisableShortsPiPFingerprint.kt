package app.revanced.patches.youtube.extended.shortspip.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object DisableShortsPiPFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
      Opcode.IGET_BOOLEAN
    ),
    customFingerprint = { it.definingClass == "Lcom/google/android/apps/youtube/app/common/ui/pip/DefaultPipController;" }
)