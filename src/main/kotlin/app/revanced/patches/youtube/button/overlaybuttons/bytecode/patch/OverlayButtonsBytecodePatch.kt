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
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.util.integrations.Constants.BUTTON_PATH

@Name("overlay-buttons-bytecode-patch")
@DependsOn(
    dependencies = [
        PlayerControlsPatch::class,
        MainstreamVideoIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class OverlayButtonsBytecodePatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            "$BUTTON_PATH/Download;",
            "$BUTTON_PATH/AutoRepeat;",
            "$BUTTON_PATH/CopyWithTimeStamp;",
            "$BUTTON_PATH/Copy;",
            "$BUTTON_PATH/Whitelists;",
            "$BUTTON_PATH/Speed;"
        ).forEach { descriptor ->
            PlayerControlsPatch.initializeControl(descriptor)
            PlayerControlsPatch.injectVisibility(descriptor)
        }

        return PatchResultSuccess()
    }
}