package app.revanced.patches.youtube.button.overlaybuttons.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.playercontrols.patch.PlayerControlsPatch
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.util.integrations.Constants.BUTTON_PATH

@Name("overlay-buttons-bytecode-patch")
@DependsOn(
    dependencies = [
        PlayerControlsPatch::class,
        LegacyVideoIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class OverlayButtonsBytecodePatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            "Download",
            "AutoRepeat",
            "CopyWithTimeStamp",
            "Copy",
            "Speed"
        ).forEach {
            PlayerControlsPatch.initializeControl("$BUTTON_PATH/$it;")
            PlayerControlsPatch.injectVisibility("$BUTTON_PATH/$it;")
        }

        return PatchResultSuccess()
    }
}