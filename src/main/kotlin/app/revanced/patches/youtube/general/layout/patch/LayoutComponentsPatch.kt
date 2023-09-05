package app.revanced.patches.youtube.general.layout.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import app.revanced.util.integrations.Constants.PATCHES_PATH

@Patch
@Name("Hide layout components")
@Description("Hides general layout components.")
@DependsOn(
    [
        LithoFilterPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class LayoutComponentsPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/CommunityPostFilter;")
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/LayoutComponentsFilter;")
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/LayoutComponentsUniversalFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "PREFERENCE: PLAYER_SETTINGS",

                "SETTINGS: HIDE_AUDIO_TRACK_BUTTON",
                "SETTINGS: HIDE_LAYOUT_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-layout-components")

        context.updatePatchStatus("LayoutComponent")

    }
}
