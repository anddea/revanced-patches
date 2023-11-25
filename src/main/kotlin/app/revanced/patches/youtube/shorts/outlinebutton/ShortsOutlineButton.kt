package app.revanced.patches.youtube.shorts.outlinebutton

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources

@Patch(
    name = "Shorts outline button",
    description = "Apply the outline icon to the action button of the Shorts player.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object ShortsOutlineButton : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).forEach { dpi ->
            context.copyResources(
                "youtube/shorts/outline",
                ResourceUtils.ResourceGroup(
                    "drawable-$dpi",
                    "ic_remix_filled_white_24.webp",
                    "ic_remix_filled_white_shadowed.webp",
                    "ic_right_comment_shadowed.webp",
                    "ic_right_dislike_off_shadowed.webp",
                    "ic_right_like_off_shadowed.webp",
                    "ic_right_share_shadowed.webp"
                )
            )
        }

        arrayOf(
            // Shorts outline icons for older versions of YouTube
            ResourceUtils.ResourceGroup(
                "drawable",
                "ic_right_comment_32c.xml",
                "ic_right_dislike_off_32c.xml",
                "ic_right_like_off_32c.xml",
                "ic_right_share_32c.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/shorts/outline", resourceGroup)
        }

        SettingsPatch.updatePatchStatus("Shorts outline button")

    }
}