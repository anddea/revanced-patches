package app.revanced.patches.youtube.player.previousnextbutton

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.playerbutton.PlayerButtonHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch

@Patch(
    name = "Hide previous next button",
    description = "Hides the previous and next button in the player controller.",
    dependencies = [
        PlayerButtonHookPatch::class,
        SettingsPatch::class
    ],
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
                "18.42.41"
            ]
        )
    ]
)
@Suppress("unused")
object HidePreviousNextButtonPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_PREVIOUS_NEXT_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide previous next button")

    }
}
