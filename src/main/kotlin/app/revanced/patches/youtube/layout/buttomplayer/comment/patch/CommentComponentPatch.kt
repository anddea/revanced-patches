package app.revanced.patches.youtube.layout.buttomplayer.comment.patch

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
import app.revanced.patches.youtube.ads.general.bytecode.patch.GeneralAdsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-comment-component")
@Description("Adds options to hide comment component under a video.")
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

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_LAYOUT_SETTINGS",
                "SETTINGS: COMMENT_COMPONENT_PARENT",
                "SETTINGS: COMMENT_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-comment-component")

        return PatchResultSuccess()
    }
}
