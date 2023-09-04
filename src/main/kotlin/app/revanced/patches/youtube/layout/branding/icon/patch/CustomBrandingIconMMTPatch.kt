package app.revanced.patches.youtube.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.IconHelper.customIcon
import app.revanced.util.resources.ResourceHelper.updatePatchStatusIcon

@Patch(false)
@Name("Custom branding icon MMT")
@Description("Changes the YouTube launcher icon to MMT.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class CustomBrandingIconMMTPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        context.customIcon("mmt")
        context.updatePatchStatusIcon("mmt")

    }
}
