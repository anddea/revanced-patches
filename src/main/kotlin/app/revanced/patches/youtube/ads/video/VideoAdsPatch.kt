package app.revanced.patches.youtube.ads.video

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.ads.AbstractAdsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.ADS_PATH

@Patch(
    name = "Hide video ads",
    description = "Hides ads in the video player.",
    dependencies = [SettingsPatch::class],
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
object VideoAdsPatch : AbstractAdsPatch(
    "$ADS_PATH/HideVideoAdsPatch;->hideVideoAds()Z"
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: ADS_SETTINGS",
                "SETTINGS: HIDE_VIDEO_ADS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide video ads")

    }
}
