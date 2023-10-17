package app.revanced.patches.youtube.player.collapsebutton

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.playerbutton.PlayerButtonHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch

@Patch(
    name = "Hide collapse button",
    description = "Hides the collapse button in the video player.",
    dependencies =
    [
        PlayerButtonHookPatch::class,
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
                "18.37.36"
            ]
        )
    ]
)
@Suppress("unused")
object HideCollapseButtonPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_COLLAPSE_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide collapse button")

    }
}
