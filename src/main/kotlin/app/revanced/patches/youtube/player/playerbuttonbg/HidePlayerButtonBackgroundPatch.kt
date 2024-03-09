package app.revanced.patches.youtube.player.playerbuttonbg

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.player.playerbuttonbg.fingerprints.PlayerPatchFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.playerbutton.PlayerButtonHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception

@Patch(
    name = "Hide player buttons background",
    description = "Adds an option to hide player button background.",
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
                "19.05.35",
                "19.05.36"
            ]
        )
    ],
    use = true
)
@Suppress("unused")
object HidePlayerButtonBackgroundPatch : BytecodePatch(
    setOf(PlayerPatchFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PlayerPatchFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "invoke-static {p0}, " +
                "$INTEGRATIONS_PATH/utils/ResourceHelper;->" +
                "hidePlayerButtonBackground(Landroid/view/View;)V"
        ) ?: throw PlayerPatchFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_PLAYER_BUTTON_BACKGROUND"
            )
        )

        SettingsPatch.updatePatchStatus("Hide player buttons background")

    }
}
