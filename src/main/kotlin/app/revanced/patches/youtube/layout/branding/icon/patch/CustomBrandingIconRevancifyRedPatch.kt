package app.revanced.patches.youtube.layout.branding.icon.patch

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
import app.revanced.util.resources.IconHelper.customIcon
import app.revanced.util.resources.ResourceHelper.updatePatchStatusIcon

@Patch(false)
@Name("Custom branding icon Revancify red")
@Description("Changes the YouTube launcher icon to Revancify Red.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class CustomBrandingIconRevancifyRedPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.customIcon("revancify-red")
        context.updatePatchStatusIcon("revancify_red")

        return PatchResultSuccess()
    }
}
