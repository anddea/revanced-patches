package app.revanced.patches.youtube.misc.pipnotification.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.pipnotification.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult

@Name("hide-pip-notification-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class PiPNotificationBytecodePatch : BytecodePatch(
    listOf(
        PrimaryPiPFingerprint,
        SecondaryPiPFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            PrimaryPiPFingerprint,
            SecondaryPiPFingerprint
        ).map {
            it.result ?: return it.toErrorResult()
        }.forEach {
            val index = it.scanResult.patternScanResult!!.startIndex + 1
            it.mutableMethod.addInstruction(index, "return-void")
        }

        return PatchResultSuccess()
    }
}