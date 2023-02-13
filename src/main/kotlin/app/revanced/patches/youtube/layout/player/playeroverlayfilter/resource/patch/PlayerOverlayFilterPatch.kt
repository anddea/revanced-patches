package app.revanced.patches.youtube.layout.player.playeroverlayfilter.resource.patch

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
import app.revanced.patches.youtube.layout.player.playeroverlayfilter.bytecode.patch.PlayerOverlayFilterBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper

@Patch
@Name("hide-player-overlay-filter")
@Description("Remove the dark filter layer from the player's background.")
@DependsOn(
    [
        SettingsPatch::class,
        PlayerOverlayFilterBytecodePatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class PlayerOverlayFilterPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings2(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: PLAYER",
            "SETTINGS: HIDE_PALYER_OVERLAY_FILTER"
        )

        ResourceHelper.patchSuccess(
            context,
            "hide-player-overlay-filter"
        )

        return PatchResultSuccess()
    }
}