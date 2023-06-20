package app.revanced.patches.youtube.fullscreen.quickactions.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.ads.general.resource.patch.GeneralAdsPatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.quickactionscontainer.patch.HideQuickActionsContainerPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-quick-actions")
@Description("Adds the options to hide quick actions components in the fullscreen.")
@DependsOn(
    [
        GeneralAdsPatch::class,
        HideQuickActionsContainerPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class QuickActionsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "SETTINGS: HIDE_QUICK_ACTIONS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-quick-actions")

        return PatchResultSuccess()
    }
}
