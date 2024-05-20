package app.revanced.patches.youtube.layout.animated

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources


@Suppress("unused")
object ForceAnimatedLikePatch : ResourcePatch(
    name = "Force hide double tap to like animations",
    description = "Hide the like animations when double tap the screen in the Shorts player at compile time.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {
        /**
         * Copy json
         */
        context.copyResources(
            "youtube/animated",
            ResourceGroup(
                "raw",
                "like_tap_feedback.json"
            )
        )

        SettingsPatch.updatePatchStatus("Hide double tap to like animations")
    }
}