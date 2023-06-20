package app.revanced.patches.youtube.layout.optimize.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("optimize-resource")
@DependsOn(
    [
        RedundantResourcePatch::class,
        SettingsPatch::class
    ]
)
@Description("Removes duplicate resources from YouTube.")
@YouTubeCompatibility
@Version("0.0.1")
class OptimizeResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        SettingsPatch.updatePatchStatus("optimize-resource")

        return PatchResultSuccess()
    }
}
