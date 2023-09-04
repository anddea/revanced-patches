package app.revanced.patches.youtube.layout.optimize.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("Optimize resource")
@Description("Removes duplicate resources from YouTube.")
@DependsOn(
    [
        RedundantResourcePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class OptimizeResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        SettingsPatch.updatePatchStatus("optimize-resource")

    }
}
