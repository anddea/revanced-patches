package app.revanced.patches.youtube.misc.videoid.legacy.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object LegacyVideoIdFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.DECLARED_SYNCHRONIZED or AccessFlags.FINAL or AccessFlags.PUBLIC,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.INVOKE_INTERFACE),
    customFingerprint = {
        it.definingClass.endsWith("PlaybackLifecycleMonitor;")
    }
)
