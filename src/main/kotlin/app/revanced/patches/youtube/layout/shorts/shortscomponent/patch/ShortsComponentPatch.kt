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
import app.revanced.patches.youtube.ads.general.bytecode.patch.GeneralAdsBytecodePatch
import app.revanced.patches.youtube.misc.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus

@Patch
@Name("hide-shorts-component")
@Description("Hides other Shorts components.")
@DependsOn(
    [
        GeneralAdsBytecodePatch::class,
        LithoFilterPatch::class,
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

        /*
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
