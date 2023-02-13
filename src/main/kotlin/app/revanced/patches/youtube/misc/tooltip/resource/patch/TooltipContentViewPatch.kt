package app.revanced.patches.youtube.misc.tooltip.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.misc.tooltip.bytecode.patch.TooltipContentViewBytecodePatch
import app.revanced.util.resources.ResourceHelper

@Patch
@Name("hide-tooltip-content")
@Description("Hides the tooltip box that appears on first install.")
@DependsOn(
    [
        TooltipContentViewBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class MinimizedPlaybackPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        ResourceHelper.patchSuccess(
            context,
            "hide-tooltip-content"
        )

        return PatchResultSuccess()
    }
}