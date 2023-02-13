package app.revanced.patches.youtube.misc.tooltip.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.youtube.misc.tooltip.bytecode.fingerprints.TooltipContentViewFingerprint

@Name("hide-tooltip-content-bytecode-patch")
@DependsOn([SharedResourcdIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class TooltipContentViewBytecodePatch : BytecodePatch(
    listOf(
        TooltipContentViewFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        TooltipContentViewFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "return-void"
        ) ?: return TooltipContentViewFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
