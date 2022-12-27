package app.revanced.patches.youtube.misc.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.IconHelper
import app.revanced.shared.util.resources.ResourceHelper

@Patch(false)
@Name("custom-branding-icon-afn-blue")
@Description("Changes the YouTube launcher icon (Afn / Blue).")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class CustomBrandingPatch_Blue : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        IconHelper.customIcon(
            context,
            "blue"
        )

        ResourceHelper.iconPatchSuccess(
            context,
            "blue"
        )

        return PatchResultSuccess()
    }
}
