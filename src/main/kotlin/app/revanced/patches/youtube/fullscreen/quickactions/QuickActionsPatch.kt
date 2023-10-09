package app.revanced.patches.youtube.fullscreen.quickactions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.quickactions.QuickActionsHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.PATCHES_PATH

@Patch(
    name = "Hide quick actions",
    description = "Adds the options to hide quick actions components in the fullscreen.",
    dependencies = [
        LithoFilterPatch::class,
        QuickActionsHookPatch::class,
        SettingsPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40"
            ]
        )
    ]
)
@Suppress("unused")
object QuickActionsPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/QuickActionFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "SETTINGS: HIDE_QUICK_ACTIONS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-quick-actions")

    }
}
