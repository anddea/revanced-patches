package app.revanced.patches.youtube.layout.shorts.shortscomponent.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.ads.general.resource.patch.GeneralAdsPatch
import app.revanced.patches.youtube.shorts.shortscomponent.patch.ShortsCommentButtonPatch
import app.revanced.patches.youtube.shorts.shortscomponent.patch.ShortsInfoPanelPatch
import app.revanced.patches.youtube.shorts.shortscomponent.patch.ShortsPaidContentBannerPatch
import app.revanced.patches.youtube.shorts.shortscomponent.patch.ShortsRemixButtonPatch
import app.revanced.patches.youtube.shorts.shortscomponent.patch.ShortsSubscriptionsButtonPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus

@Patch
@Name("hide-shorts-component")
@Description("Hides other Shorts components.")
@DependsOn(
    [
        GeneralAdsPatch::class,
        ResourceMappingPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        ShortsCommentButtonPatch::class,
        ShortsInfoPanelPatch::class,
        ShortsPaidContentBannerPatch::class,
        ShortsRemixButtonPatch::class,
        ShortsSubscriptionsButtonPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ShortsComponentPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        context.updatePatchStatus("ShortsComponent")

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

        return PatchResultSuccess()
    }
}
