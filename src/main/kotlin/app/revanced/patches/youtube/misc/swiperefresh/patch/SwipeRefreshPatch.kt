package app.revanced.patches.youtube.misc.swiperefresh.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.swiperefresh.fingerprint.SwipeRefreshLayoutFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("enable-swipe-refresh")
@Description("Enable swipe refresh.")
@YouTubeCompatibility
@Version("0.0.1")
class SwipeRefreshPatch : BytecodePatch(
    listOf(
        SwipeRefreshLayoutFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val result = SwipeRefreshLayoutFingerprint.result ?:return SwipeRefreshLayoutFingerprint.toErrorResult()
        val method = result.mutableMethod
        val index = result.scanResult.patternScanResult!!.endIndex
        val register = (method.instruction(index) as OneRegisterInstruction).registerA

        method.addInstruction(index, "const/4 v$register, 0x0")

        return PatchResultSuccess()
    }
}
