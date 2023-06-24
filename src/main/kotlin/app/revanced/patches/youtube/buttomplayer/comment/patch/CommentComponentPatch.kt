package app.revanced.patches.youtube.buttomplayer.comment.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.ads.general.bytecode.patch.GeneralAdsBytecodePatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PATCHES_PATH

@Patch
@Name("hide-comment-component")
@Description("Hides components related to comments.")
@DependsOn(
    [
        GeneralAdsBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class CommentComponentPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/CommentsFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "SETTINGS: COMMENT_COMPONENT_PARENT",
                "SETTINGS: COMMENT_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-comment-component")

        return PatchResultSuccess()
    }
}
