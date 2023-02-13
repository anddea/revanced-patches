package app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.fingerprints.TimeCounterFingerprint
import app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.fingerprints.TimeCounterParentFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.youtube.misc.timebar.patch.HookTimebarPatch
import app.revanced.util.integrations.Constants.SEEKBAR_LAYOUT

@DependsOn([HookTimebarPatch::class, SharedResourcdIdPatch::class])
@Name("hide-time-and-seekbar-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideTimeAndSeekbarBytecodePatch : BytecodePatch(
    listOf(
        TimeCounterParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        TimeCounterParentFingerprint.result?.let { parentResult ->
            TimeCounterFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.let { method ->
                listOf(
                    HookTimebarPatch.setTimebarMethod,
                    method
                ).forEach {
                    it.addInstructions(
                        0, """
                            invoke-static {}, $SEEKBAR_LAYOUT->hideTimeAndSeekbar()Z
                            move-result v0
                            if-eqz v0, :hide_time_and_seekbar
                            return-void
                        """, listOf(ExternalLabel("hide_time_and_seekbar", it.instruction(0)))
                    )
                }

            } ?: return TimeCounterFingerprint.toErrorResult()
        } ?: return TimeCounterParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
