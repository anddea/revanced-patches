package app.revanced.patches.youtube.misc.litho.patch

import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.litho.AbstractLithoFilterPatch
import app.revanced.patches.youtube.ads.doublebacktoclose.patch.DoubleBackToClosePatch
import app.revanced.patches.youtube.ads.swiperefresh.patch.SwipeRefreshPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import app.revanced.util.integrations.Constants.ADS_PATH

@DependsOn(
    [
        DoubleBackToClosePatch::class,
        SwipeRefreshPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class LithoFilterPatch : AbstractLithoFilterPatch(
    "$ADS_PATH/LithoFilterPatch;"
) {
    override fun execute(context: BytecodeContext): PatchResult {
        super.execute(context)

        context.updatePatchStatus("ByteBuffer")

        return PatchResultSuccess()
    }
}
