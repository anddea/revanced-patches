package app.revanced.patches.youtube.shorts.shortsoverlay

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources

@Patch(
    name = "Shorts overlay buttons",
    description = "Apply the new icons to the action buttons of the Shorts player.",
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
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39"
            ]
        )
    ],
    use = true
)
@Suppress("unused")
object ShortsOverlayButtonsPatch : ResourcePatch() {
    private const val DEFAULT_ICON_KEY = "TikTok"

    private val IconType by stringPatchOption(
        key = "IconType",
        default = DEFAULT_ICON_KEY,
        values = mapOf(
            "Outline" to "outline",
            "OutlineCircle" to "outlinecircle",
            DEFAULT_ICON_KEY to "tiktok"
        ),
        title = "Icon type of Shorts",
        description = "Apply different icons for Shorts action buttons."
    )

    override fun execute(context: ResourceContext) {
        IconType?.let { iconType ->
            val selectedIconType = iconType.lowercase()

            val commonResources = arrayOf(
                ResourceGroup(
                    "drawable-xxhdpi",
                    "ic_remix_filled_white_24.webp", // for older versions only
                    "ic_remix_filled_white_shadowed.webp",
                    "ic_right_comment_shadowed.webp",
                    "ic_right_dislike_off_shadowed.webp",
                    "ic_right_dislike_on_shadowed.webp",
                    "ic_right_like_off_shadowed.webp",
                    "ic_right_like_on_shadowed.webp",
                    "ic_right_share_shadowed.webp"
                )
            )

            if (selectedIconType == "outline" || selectedIconType == "outlinecircle") {
                arrayOf(
                    "xxxhdpi",
                    "xxhdpi",
                    "xhdpi",
                    "hdpi",
                    "mdpi"
                ).forEach { dpi ->
                    context.copyResources(
                        "youtube/shorts/outline",
                        ResourceGroup(
                            "drawable-$dpi",
                            "ic_right_dislike_on_32c.webp",
                            "ic_right_like_on_32c.webp"
                        ),
                        ResourceGroup(
                            "drawable",
                            "ic_right_comment_32c.xml",
                            "ic_right_dislike_off_32c.xml",
                            "ic_right_like_off_32c.xml",
                            "ic_right_share_32c.xml",
                            "reel_camera_bold_24dp.xml",
                            "reel_more_vertical_bold_24dp.xml",
                            "reel_search_bold_24dp.xml"
                        )
                    )
                }
            }

            commonResources.forEach { resourceGroup ->
                context.copyResources("youtube/shorts/$selectedIconType", resourceGroup)
            }
        } ?: throw PatchException("Invalid icon type path.")

        SettingsPatch.updatePatchStatus("Shorts overlay buttons")
    }
}
