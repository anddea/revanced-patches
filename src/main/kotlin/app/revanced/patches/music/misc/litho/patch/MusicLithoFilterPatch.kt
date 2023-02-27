package app.revanced.patches.music.misc.litho.patch

import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.litho.AbstractLithoFilterPatch
import app.revanced.util.integrations.Constants.ADS_PATH

@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicLithoFilterPatch : AbstractLithoFilterPatch(
    "$ADS_PATH/MusicLithoFilterPatch;"
) {
    override fun execute(context: BytecodeContext): PatchResult {
        super.execute(context)

        return PatchResultSuccess()
    }
}
