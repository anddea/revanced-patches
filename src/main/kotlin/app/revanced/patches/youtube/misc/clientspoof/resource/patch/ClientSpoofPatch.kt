package app.revanced.patches.youtube.misc.clientspoof.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.misc.clientspoof.bytecode.patch.ClientSpoofBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("client-spoof")
@Description("Spoofs the YouTube client to prevent playback issues.")
@DependsOn(
    [
        ClientSpoofBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ClientSpoofPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        ResourceHelper.patchSuccess(
            context,
            "client-spoof"
        )

        return PatchResultSuccess()
    }
}