package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.shorts.shortsnavigationbar.patch.ShortsNavigationBarPatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.navbarindex.patch.NavBarIndexHookPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PATCHES_PATH

@Patch
@Name("Hide shorts components")
@Description("Hides other Shorts components.")
@DependsOn(
    [
        LithoFilterPatch::class,
        NavBarIndexHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        ShortsCommentButtonPatch::class,
        ShortsDislikeButtonPatch::class,
        ShortsInfoPanelPatch::class,
        ShortsLikeButtonPatch::class,
        ShortsNavigationBarPatch::class,
        ShortsPaidPromotionBannerPatch::class,
        ShortsRemixButtonPatch::class,
        ShortsShareButtonPatch::class,
        ShortsSubscriptionsButtonPatch::class,
        ShortsToolBarPatch::class
    ]
)
@YouTubeCompatibility
class ShortsComponentPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/ShortsFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SHORTS_SETTINGS",
                "SETTINGS: HIDE_SHORTS_SHELF",
                "SETTINGS: SHORTS_PLAYER_PARENT",
                "SETTINGS: HIDE_SHORTS_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-shorts-component")

    }
}
