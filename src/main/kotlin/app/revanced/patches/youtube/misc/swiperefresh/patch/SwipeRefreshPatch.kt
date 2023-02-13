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
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.extensions.toErrorResult
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

        SwipeRefreshLayoutFingerprint.result?.let {
            with (it.mutableMethod) {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = (instruction(insertIndex) as OneRegisterInstruction).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$register, 0x0"
                )
            }
        } ?: return SwipeRefreshLayoutFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
