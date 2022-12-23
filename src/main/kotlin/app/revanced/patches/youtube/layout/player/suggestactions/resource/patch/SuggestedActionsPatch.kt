package app.revanced.patches.youtube.layout.player.suggestactions.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.layout.player.suggestactions.bytecode.patch.SuggestedActionsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("hide-suggested-actions")
@Description("Hide the suggested actions bar inside the player.")
@DependsOn(
    [
        SettingsPatch::class,
        SuggestedActionsBytecodePatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SuggestedActionsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings2(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: PLAYER",
            "SETTINGS: HIDE_SUGGESTED_ACTION"
        )

        ResourceHelper.patchSuccess(
            context,
            "hide-suggested-actions"
        )

        return PatchResultSuccess()
    }
}