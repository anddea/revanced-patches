package app.revanced.util.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.util.fingerprint.MultiMethodFingerprint
import app.revanced.util.fingerprint.MultiMethodFingerprint.Companion.resolve

abstract class MultiMethodBytecodePatch(
    val fingerprints: Set<MethodFingerprint> = setOf(),
    val multiFingerprints: Set<MultiMethodFingerprint> = setOf()
) : BytecodePatch(fingerprints) {
    override fun execute(context: BytecodeContext) {
        multiFingerprints.resolve(context, context.classes)
    }
}
