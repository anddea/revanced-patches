package app.revanced.patches.youtube.general.layout

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.PATCHES_PATH

@Patch(
    name = "Hide layout components",
    description = "Hides general layout components.",
    dependencies = [
        LithoFilterPatch::class,
        SettingsPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
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
                "18.39.41"
            ]
        )
    ]
)
@Suppress("unused")
object LayoutComponentsPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/BrowseStoreButtonFilter;")
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/CommunityPostFilter;")
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/LayoutComponentsFilter;")
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/LayoutComponentsUniversalFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "PREFERENCE: GENERAL_SETTINGS",
                "PREFERENCE: PLAYER_SETTINGS",

                "SETTINGS: HIDE_AUDIO_TRACK_BUTTON",
                "SETTINGS: HIDE_CHANNEL_BAR_BUTTON",
                "SETTINGS: HIDE_LAYOUT_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide layout components")
    }
}
