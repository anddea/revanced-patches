package app.revanced.patches.youtube.misc.returnyoutubedislike.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.patch.ReturnYouTubeDislikeBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("return-youtube-dislike")
@Description("Shows the dislike count of videos using the Return YouTube Dislike API.")
@DependsOn(
    [
        SettingsPatch::class,
        ReturnYouTubeDislikeBytecodePatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ReturnYouTubeDislikePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add ReVanced Settings
         */
        ResourceHelper.addReVancedSettings(
            context,
            "PREFERENCE: RETURN_YOUTUBE_DISLIKE"
        )

        ResourceHelper.patchSuccess(
            context,
            "return-youtube-dislike"
        )

        return PatchResultSuccess()
    }
}