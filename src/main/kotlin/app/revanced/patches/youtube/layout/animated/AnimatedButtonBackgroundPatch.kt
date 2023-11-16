package app.revanced.patches.youtube.layout.animated

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources

@Patch(
    name = "Hide animated button background",
    description = "Hides the background of the pause and play animated buttons in the Shorts player.",
    dependencies = [SettingsPatch::class],
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
                "18.45.38"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object AnimatedButtonBackgroundPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {
        /**
         * Copy json
         */
        context.copyResources(
            "youtube/animated",
            ResourceUtils.ResourceGroup(
                "raw",
                "pause_tap_feedback.json",
                "play_tap_feedback.json"
            )
        )

        SettingsPatch.updatePatchStatus("Hide animated button background")
    }
}