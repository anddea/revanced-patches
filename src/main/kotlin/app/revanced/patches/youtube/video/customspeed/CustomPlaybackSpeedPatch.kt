package app.revanced.patches.youtube.video.customspeed

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.customspeed.AbstractCustomPlaybackSpeedPatch
import app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.OldSpeedLayoutPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.VIDEO_PATH

@Patch(
    name = "Custom playback speed",
    description = "Adds more playback speed options.",
    dependencies = [
        OldSpeedLayoutPatch::class,
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
object CustomPlaybackSpeedPatch : AbstractCustomPlaybackSpeedPatch(
    "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
    8.0f
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: CUSTOM_PLAYBACK_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("custom-playback-speed")
    }
}
