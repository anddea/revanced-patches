package app.revanced.patches.youtube.misc.hdrbrightness.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.misc.hdrbrightness.bytecode.patch.HDRBrightnessBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("enable-hdr-auto-brightness")
@Description("Makes the brightness of HDR videos follow the system default.")
@DependsOn(
    [
        HDRBrightnessBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HDRBrightnessPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: MISC_SETTINGS",
            "SETTINGS: ENABLE_HDR_AUTO_BRIGHTNESS"
        )

        ResourceHelper.patchSuccess(
            context,
            "enable-hdr-auto-brightness"
        )

        return PatchResultSuccess()
    }
}