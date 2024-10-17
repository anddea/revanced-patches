package app.revanced.util.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.util.fingerprint.MultiMethodFingerprint
import app.revanced.util.fingerprint.MultiMethodFingerprint.Companion.resolve

/**
 * Taken from BiliRoamingX:
 * https://github.com/BiliRoamingX/BiliRoamingX/blob/ae58109f3acdd53ec2d2b3fb439c2a2ef1886221/patches/src/main/kotlin/app/revanced/patches/bilibili/patcher/patch/MultiMethodBytecodePatch.kt
 */
abstract class MultiMethodBytecodePatch(
    val fingerprints: Set<MethodFingerprint> = setOf(),
    val multiFingerprints: Set<MultiMethodFingerprint> = setOf()
) : BytecodePatch(fingerprints) {
    override fun execute(context: BytecodeContext) {
        multiFingerprints.resolve(context, context.classes)
    }
}
