package app.revanced.patches.youtube.misc.pipnotification.resource.patch

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
import app.revanced.patches.youtube.misc.pipnotification.bytecode.patch.PiPNotificationBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper

@Patch
@Name("hide-pip-notification")
@Description("Disable pip notification when you first launch pip mode.")
@DependsOn(
    [
        PiPNotificationBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class PiPNotificationPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        ResourceHelper.patchSuccess(
            context,
            "hide-pip-notification"
        )

        return PatchResultSuccess()
    }
}