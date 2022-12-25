package app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.fingerprints.TimeCounterFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.patches.timebar.HookTimebarPatch
import app.revanced.shared.util.integrations.Constants.SEEKBAR_LAYOUT

@DependsOn([HookTimebarPatch::class])
@Name("hide-time-and-seekbar-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideTimeAndSeekbarBytecodePatch : BytecodePatch(
    listOf(
        TimeCounterFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        listOf(
            HookTimebarPatch.SetTimbarFingerprintResult,
            TimeCounterFingerprint.result!!
        ).forEach { result ->
            val method = result.mutableMethod
            method.addInstructions(
                0, """
                    const/4 v0, 0x0
                    invoke-static { }, $SEEKBAR_LAYOUT->hideTimeAndSeekbar()Z
                    move-result v0
                    if-eqz v0, :hide_time_and_seekbar
                    return-void
                """, listOf(ExternalLabel("hide_time_and_seekbar", method.instruction(0)))
            )
        }

        return PatchResultSuccess()
    }
}
